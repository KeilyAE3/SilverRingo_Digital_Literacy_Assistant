"""M01 offline schema validation, snapshots and local Git change detection."""
from collections import Counter
from copy import deepcopy
from pathlib import Path, PurePosixPath
import json
import re
import subprocess
from threading import RLock
from .models import KnowledgeDocument, SourceRef, STATUSES, identifier, text

CURRENT_STATUSES = frozenset({'implemented', 'prototype', 'placeholder'})
FILES = ('app_info.json', 'learning_courses.json', 'assistant_guides.json', 'action_catalog.json')


class KnowledgeError(ValueError):
    """Invalid knowledge; messages identify structure, never raw contents."""


def require(condition, location):
    if not condition:
        raise KnowledgeError(f'Invalid knowledge: {location}')


def array(obj, key):
    require(isinstance(obj, dict) and isinstance(obj.get(key), list), key)
    return obj[key]


def strings(value, location):
    require(isinstance(value, list) and all(isinstance(s, str) and s.strip() for s in value), location)


def source_ref(value):
    require(isinstance(value, dict), 'source')
    try:
        ref = SourceRef(value['branch'], value['path'], value['kind'])
    except (KeyError, TypeError, ValueError):
        raise KnowledgeError('Invalid source fields') from None
    path = PurePosixPath(ref.path)
    require(not path.is_absolute() and '..' not in path.parts and '\\' not in ref.path
            and ':' not in ref.path, 'source.path')
    require(isinstance(value.get('commit'), str) and re.fullmatch('[0-9a-f]{40}', value['commit']), 'source.commit')
    if 'sha256' in value:
        require(isinstance(value['sha256'], str) and re.fullmatch('[0-9a-f]{64}', value['sha256']), 'source.sha256')
    return ref


def read_json(path):
    def unique_pairs(pairs):
        result = {}
        for key, value in pairs:
            require(key not in result, path.name + ': duplicate JSON key')
            result[key] = value
        return result
    try:
        data = json.loads(path.read_text(encoding='utf-8-sig'), object_pairs_hook=unique_pairs)
    except (OSError, UnicodeError, json.JSONDecodeError):
        raise KnowledgeError(f'Cannot read valid JSON: {path.name}') from None
    require(isinstance(data, dict) and type(data.get('schema_version')) is int
            and data['schema_version'] == 1, path.name + '.schema_version')
    return data


class KnowledgeStore:
    """Validate all files before publishing. Return copies to protect snapshots.

    Explicit statuses permit roadmap/history; defaults exclude idea and retired.
    Action data is not execution authorization. M01 rejects enabled actions.
    """
    def __init__(self, knowledge_dir):
        self.knowledge_dir = Path(knowledge_dir)
        self._lock = RLock()
        self._snapshot = None

    def load(self):
        self.reload()

    def reload(self):
        with self._lock:
            resources = {name: read_json(self.knowledge_dir / name) for name in FILES}
            documents, actions, sources = self._validate(resources)
            self._snapshot = (tuple(documents), tuple(actions), tuple(sources))

    def _get(self):
        if self._snapshot is None:
            raise KnowledgeError('Knowledge has not been loaded')
        return self._snapshot

    def documents(self, statuses=None):
        selected = CURRENT_STATUSES if statuses is None else frozenset(statuses)
        require(selected <= STATUSES, 'requested statuses')
        with self._lock:
            return deepcopy([doc for doc in self._get()[0] if doc.status in selected])

    def actions(self):
        with self._lock:
            return deepcopy(list(self._get()[1]))

    def counts(self):
        with self._lock:
            counts = Counter(doc.status for doc in self._get()[0])
            return {status: counts[status] for status in sorted(STATUSES)}

    def source_changes(self, repository):
        """Compare pinned blobs with local origin refs; no fetch or source execution."""
        with self._lock:
            sources = deepcopy(self._get()[2])
        result, seen = [], set()
        for source in sources:
            key = (source['branch'], source['path'], source['commit'])
            if key in seen:
                continue
            seen.add(key)
            blobs = []
            for revision in (source['commit'], 'refs/remotes/origin/' + source['branch']):
                proc = subprocess.run(['git', '-C', str(repository), 'rev-parse', '--verify',
                                       revision + ':' + source['path']], capture_output=True, text=True)
                require(proc.returncode == 0, 'source Git object unavailable')
                blobs.append(proc.stdout.strip())
            result.append(dict(branch=source['branch'], path=source['path'], commit=source['commit'],
                               changed=blobs[0] != blobs[1]))
        return result

    @staticmethod
    def _validate(resources):
        docs, sources, ids = [], [], set()
        actions = array(resources['action_catalog.json'], 'actions')
        action_ids = set()
        for action in actions:
            require(isinstance(action, dict), 'action')
            try:
                identifier(action.get('id'))
                for name in ('kind', 'target', 'label', 'owner', 'status'):
                    text(action.get(name), name)
            except ValueError:
                raise KnowledgeError('Invalid action fields') from None
            require(action['id'] not in ids, 'duplicate action ID')
            ids.add(action['id'])
            action_ids.add(action['id'])
            require(action.get('enabled') is False and action['status'] == 'unintegrated', 'M01 action must be disabled/unintegrated')
            strings(action.get('requires'), 'action.requires')
            source_ref(action.get('source'))
            sources.append(action['source'])

        def add(record, category):
            require(isinstance(record, dict), category + '.record')
            source = source_ref(record.get('source'))
            try:
                doc = KnowledgeDocument(id=record['id'], category=category, title=record['title'],
                    content=record.get('content', record.get('summary')), status=record['status'], source=source,
                    keywords=record.get('keywords', []), aliases=record.get('aliases', []),
                    related_action_ids=record.get('related_action_ids', []))
            except (ValueError, TypeError, KeyError):
                raise KnowledgeError(f'Invalid {category} document') from None
            require(doc.id not in ids, 'duplicate knowledge ID')
            ids.add(doc.id)
            require(set(doc.related_action_ids) <= action_ids, 'unknown action reference')
            if doc.aliases:
                require(record.get('alias_origin') == 'curated_by_d', 'alias provenance')
            if doc.status in ('idea', 'retired'):
                require(not doc.related_action_ids and record.get('is_executable') is False, 'noncurrent execution')
            docs.append(doc)
            sources.append(record['source'])

        entries = array(resources['app_info.json'], 'entries')
        require(bool(entries), 'empty app entries')
        for entry in entries:
            add(entry, 'app')
        courses = array(resources['learning_courses.json'], 'courses')
        require(len(courses) == 1 and isinstance(courses[0], dict)
                and courses[0].get('topic_id') == 'kiosk', 'M01 kiosk course')
        for course in courses:
            add(course, 'learning')
            source_ref(course.get('unlock_policy_source'))
            sources.append(course['unlock_policy_source'])
            levels = array(course, 'levels')
            require(len(levels) == 6 and all(isinstance(level, dict) and type(level.get('level_id')) is int for level in levels), 'levels')
            require({level['level_id'] for level in levels} == set(range(1, 7)), 'level IDs')
            for level in levels:
                require(level.get('id') == f"course:kiosk:level:{level['level_id']}", 'level ID mapping')
                for name in ('task', 'start', 'end'):
                    require(isinstance(level.get(name), str) and bool(level[name].strip()), 'level.' + name)
                strings(level.get('checks'), 'level.checks')
                add(level, 'learning')
            guides = array(course, 'guides')
            expected = {'start', 'dining', 'menu', 'options', 'cart', 'review', 'payment', 'card', 'receipt'}
            require(len(guides) == 9 and all(isinstance(g, dict) and isinstance(g.get('step_id'), str) for g in guides), 'guides')
            require({g['step_id'] for g in guides} == expected, 'guide step IDs')
            for guide in guides:
                require(guide.get('id') == 'course:kiosk:guide:' + guide['step_id'], 'guide ID mapping')
                add(guide, 'learning')

        guide_data = resources['assistant_guides.json']
        services = array(guide_data, 'services')
        require(len(services) == 1 and isinstance(services[0], dict)
                and services[0].get('service_key') == 'ddocdoc', 'M01 ddocdoc service')
        for service in services:
            add(service, 'assistant')
            strings(service.get('requirements'), 'service.requirements')
            for key, count, channel in (('native_steps', 7, 'native'), ('web_steps', 4, 'web')):
                steps = array(service, key)
                require(len(steps) == count, key + '.count')
                for number, step in enumerate(steps, 1):
                    require(isinstance(step, dict), key + '.step')
                    require(step.get('id') == f'guide:ddocdoc:{channel}:{number}', key + '.id')
                    if channel == 'native':
                        require(type(step.get('step_number')) is int and step['step_number'] == number, key + '.step_number')
                        strings(step.get('target_keywords'), key + '.target_keywords')
                    else:
                        require(step.get('step_id') == f'ddocdoc-step-{number}', key + '.step_id')
                    add(step, 'assistant')
        for key, status in (('ideas', 'idea'), ('retired', 'retired')):
            for record in array(guide_data, key):
                require(isinstance(record, dict) and record.get('status') == status, key + '.status')
                add(record, 'assistant')
        for retired_id in ('guide:retired:gov24', 'guide:retired:kakao_t'):
            require(any(doc.id == retired_id and doc.status == 'retired' for doc in docs), 'required retired record')
        return docs, actions, sources
