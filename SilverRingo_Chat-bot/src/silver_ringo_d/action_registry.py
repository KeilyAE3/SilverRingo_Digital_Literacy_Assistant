"""Validated allow-list for actions exposed to the client.

The catalog is metadata, not an instruction source. Only explicitly enabled
actions with an in-app route can be returned or validated.
"""
from dataclasses import dataclass
import json
from pathlib import Path
import re
from typing import Iterable

from .models import ActionSuggestion, KnowledgeHit, identifier, text


class ActionRegistryError(ValueError):
    """The action catalog is malformed or cannot be read."""


@dataclass(frozen=True)
class Action:
    id: str
    kind: str
    target: str
    label: str
    enabled: bool
    status: str

    def __post_init__(self):
        try:
            identifier(self.id)
            text(self.kind, 'kind')
            text(self.target, 'target')
            text(self.label, 'label')
            text(self.status, 'status')
        except ValueError as exc:
            raise ActionRegistryError('Invalid action fields') from exc
        if type(self.enabled) is not bool:
            raise ActionRegistryError('Invalid action enabled flag')


_ROUTE = re.compile(r'^[a-z][a-z0-9]*(?:\.[a-z][a-z0-9]*)+$')
INTEGRATED_ROUTES = {'open_kiosk_course': 'learning.kiosk', 'open_guide_home': 'guides.home'}


class ActionRegistry:
    def __init__(self, catalog: str | Path | Iterable[dict] | None = None):
        if catalog is None:
            catalog = Path(__file__).resolve().parents[2] / 'knowledge' / 'action_catalog.json'
        records = self._read(catalog)
        actions = {}
        for record in records:
            action = self._parse(record)
            if action.id in actions:
                raise ActionRegistryError('Duplicate action ID')
            actions[action.id] = action
        self._actions = actions

    @staticmethod
    def _read(catalog):
        if isinstance(catalog, (str, Path)):
            try:
                data = json.loads(Path(catalog).read_text(encoding='utf-8-sig'))
            except (OSError, UnicodeError, json.JSONDecodeError):
                raise ActionRegistryError('Cannot read action catalog') from None
            if not isinstance(data, dict) or type(data.get('schema_version')) is not int or data['schema_version'] != 1:
                raise ActionRegistryError('Invalid action catalog schema')
            records = data.get('actions')
        else:
            records = catalog
        if not isinstance(records, list):
            raise ActionRegistryError('Action catalog actions must be a list')
        return records

    @staticmethod
    def _parse(record):
        if not isinstance(record, dict):
            raise ActionRegistryError('Action record must be an object')
        try:
            return Action(record['id'], record['kind'], record['target'], record['label'],
                          record['enabled'], record['status'])
        except (KeyError, TypeError):
            raise ActionRegistryError('Invalid action record') from None

    @staticmethod
    def _executable(action):
        return (action.enabled and action.status == 'integrated'
                and action.kind == 'navigate' and bool(_ROUTE.fullmatch(action.target))
                and INTEGRATED_ROUTES.get(action.id) == action.target)

    def get(self, action_id):
        """Return only an action currently safe to expose to the client."""
        if not isinstance(action_id, str):
            return None
        action = self._actions.get(action_id)
        return action if action is not None and self._executable(action) else None

    def validate(self, action_id, params=None):
        if params is not None and (not isinstance(params, dict) or params):
            return False
        return self.get(action_id) is not None

    def suggest(self, hits: list[KnowledgeHit]) -> list[ActionSuggestion]:
        if not isinstance(hits, list):
            raise TypeError('hits must be a list')
        suggestions = []
        seen = set()
        if any(not isinstance(hit, KnowledgeHit) for hit in hits):
            raise TypeError('hits must contain KnowledgeHit records')
        ordered = sorted(hits, key=lambda hit: (-hit.score, hit.document.id))
        for hit in ordered:
            if not isinstance(hit, KnowledgeHit):
                raise TypeError('hits must contain KnowledgeHit records')
            if hit.document.status not in ('implemented', 'prototype') or hit.score < max(4.0, ordered[0].score * 0.75):
                continue
            for action_id in hit.document.related_action_ids:
                if action_id in seen:
                    continue
                action = self.get(action_id)
                if action is None:
                    continue
                seen.add(action_id)
                suggestions.append(ActionSuggestion(action.id, action.kind, action.target, action.label))
                if len(suggestions) == 2:
                    return suggestions
        return suggestions
