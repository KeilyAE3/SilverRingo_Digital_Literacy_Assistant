"""Dependency-free collaboration checks; real application commands are opt-in."""
import datetime as dt
import json
import os
from pathlib import Path
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]


def require(condition, message):
    if not condition:
        raise ValueError(message)


def nonempty(value):
    return isinstance(value, str) and bool(value.strip())


def local_path(root, value, must_exist=True):
    require(nonempty(value), 'path must be a nonempty string')
    require(not Path(value).is_absolute(), f'absolute path is forbidden: {value}')
    path = (root / value).resolve()
    require(path.is_relative_to(root.resolve()), f'path escapes repository: {value}')
    require(not must_exist or path.exists(), f'missing path: {value}')
    return path


def strings(value, name, allow_empty=True):
    require(isinstance(value, list) and all(nonempty(x) for x in value), f'{name}: expected string list')
    require(allow_empty or bool(value), f'{name}: cannot be empty')


def commands(value):
    require(isinstance(value, list), 'commands must be a list of argument arrays')
    for argv in value:
        strings(argv, 'command argv', allow_empty=False)


def load_config(root):
    config = json.loads((root / 'automation/config.json').read_text())
    modules = config['modules']
    require(isinstance(modules, list), 'modules must be a list')
    ids = set()
    for module in modules:
        require(nonempty(module['id']) and module['id'] not in ids, 'module ids must be nonempty and unique')
        ids.add(module['id'])
        strings(module['paths'], 'module paths', allow_empty=False)
        for path in module['paths']:
            local_path(root, path)
        commands(module['commands'])
    commands(config['integration_commands'])
    return config


def validate_contract(data, module_ids):
    for key in ('id', 'version', 'provider', 'inputs', 'outputs', 'errors', 'compatibility'):
        require(nonempty(data.get(key)), f'contract: missing {key}')
    require(data['provider'] in module_ids, 'contract provider is not a registered module')
    strings(data.get('consumers'), 'contract consumers', allow_empty=False)
    require(set(data['consumers']) <= module_ids, 'contract consumer is not a registered module')


def validate_report(root, data, contract_paths):
    for key in ('owner', 'task', 'summary', 'approach', 'interface_changes', 'verification'):
        require(nonempty(data.get(key)), f'report: missing {key}')
    for key in ('files', 'contracts', 'blocked_by', 'next_steps'):
        strings(data.get(key), f'report {key}', allow_empty=key not in ('files', 'next_steps'))
    for path in data['files']:
        local_path(root, path, must_exist=False)  # deleted files can be reported
    require(set(data['contracts']) <= contract_paths, 'report references an unknown contract path')


def render_schedule(data):
    tasks = data['tasks']
    require(isinstance(tasks, list), 'schedule tasks must be a list')
    by_id = {}
    for task in tasks:
        require(re.fullmatch(r'[A-Za-z][A-Za-z0-9_]*', task['id']) is not None, 'invalid schedule id')
        require(task['id'] not in by_id, 'duplicate schedule id')
        for key in ('title', 'owner'):
            require(nonempty(task[key]) and not any(c in task[key] for c in ':\n\r`'), f'invalid schedule {key}')
        dt.date.fromisoformat(task['start'])
        require(type(task['days']) is int and task['days'] > 0, 'days must be a positive integer')
        require(task['status'] in ('todo', 'active', 'done'), 'invalid schedule status')
        strings(task['after'], 'schedule after')
        by_id[task['id']] = task
    visiting, finished, ordered = set(), set(), []

    def visit(task_id):
        require(task_id in by_id, f'unknown dependency: {task_id}')
        require(task_id not in visiting, f'cyclic dependency: {task_id}')
        if task_id in finished:
            return
        visiting.add(task_id)
        task = by_id[task_id]
        for parent_id in task['after']:
            visit(parent_id)
            parent = by_id[parent_id]
            end = dt.date.fromisoformat(parent['start']) + dt.timedelta(days=parent['days'])
            require(dt.date.fromisoformat(task['start']) >= end, f'{task_id} starts before {parent_id} finishes')
        visiting.remove(task_id)
        finished.add(task_id)
        ordered.append(task)

    for task_id in by_id:
        visit(task_id)
    lines = ['# 팀 일정', '', '자동 생성: `automation/schedule.json`을 수정한 뒤 `python automation/check.py --write-schedule`을 실행하세요.', '',
             '날짜는 달력 일수 기준입니다. 의존 작업 종료 전 시작하는 일정과 순환 의존은 검사에 실패합니다.', '',
             '담당·일정 합의는 Discord에서 진행하며, 이 표에는 시각화할 작업만 등록합니다.', '']
    if tasks:
        lines += ['```mermaid', 'gantt', '    title SilverRingo 팀 일정', '    dateFormat YYYY-MM-DD', '    axisFormat %m/%d']
        # One section per task keeps dependency order intact across owners.
        for task in ordered:
            state = '' if task['status'] == 'todo' else task['status'] + ', '
            lines += [f"    section {task['owner']}", f"    {task['title']} :{state}{task['id']}, {task['start']}, {task['days']}d"]
        lines += ['```', '', '| 작업 | 담당 | 선행 작업 |', '|---|---|---|']
        lines += [f"| {t['id']} | {t['owner'].replace('|', '/')} | {', '.join(t['after']) or '-'} |" for t in ordered]
    else:
        lines += ['등록된 일정이 없습니다.']
    return '\n'.join(lines) + '\n'


def changed_files(root, base):
    if not base or set(base) == {'0'}:
        return None  # manual run/new branch: validate all, skip change-only policy
    require(re.fullmatch(r'[0-9a-fA-F]{40}', base) is not None, 'invalid base commit SHA')
    result = subprocess.run(['git', 'diff', '--name-only', '-z', base, 'HEAD'], cwd=root, capture_output=True, check=True)
    return set(result.stdout.decode().strip('\0').split('\0')) - {''}


def markdown_links(root):
    for path in root.rglob('*.md'):
        if any(p in path.relative_to(root).parts for p in ('.git', 'node_modules', '.venv', 'vendor')):
            continue
        content = re.sub(r'```.*?```', '', path.read_text(), flags=re.S)
        for target in re.findall(r'\]\(([^\s)]+)\)', content):
            if re.match(r'^[a-zA-Z][\w+.-]*:', target) or target.startswith(('#', '//')):
                continue
            target = target.split('#')[0].split('?')[0]
            if target:
                from urllib.parse import unquote
                local_path(root, str((path.parent / unquote(target)).relative_to(root)))


def run(root=ROOT, write_schedule=False, base=None):
    results, failures = [], []

    def check(name, action):
        try:
            action()
            results.append(f'- PASS: {name}')
        except Exception as error:
            failures.append(name)
            results.append(f'- FAIL: {name}: {error}')

    config = None
    try:
        config = load_config(root)
        results.append('- PASS: automation configuration')
    except Exception as error:
        results.append(f'- FAIL: automation configuration: {error}')
        failures.append('configuration')

    def schedule():
        rendered = render_schedule(json.loads((root / 'automation/schedule.json').read_text()))
        path = root / 'docs/schedule/TEAM_GANTT.md'
        if write_schedule:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(rendered)
        require(path.exists() and path.read_text() == rendered, 'regenerate TEAM_GANTT.md with --write-schedule')

    check('schedule and dependency consistency', schedule)
    check('local Markdown file links (anchors/external URLs excluded)', lambda: markdown_links(root))
    if config is not None:
        contracts = {p.relative_to(root).as_posix(): p for p in (root / 'docs/contracts').glob('*.json')}
        reports = list((root / 'docs/reports').rglob('*.json'))
        ids = {m['id'] for m in config['modules']}
        for name, path in contracts.items():
            check(name, lambda p=path: validate_contract(json.loads(p.read_text()), ids))
        for path in reports:
            check(path.relative_to(root).as_posix(), lambda p=path: validate_report(root, json.loads(p.read_text()), set(contracts)))

        def change_policy():
            changed = changed_files(root, base)
            if changed is None:
                results.append('- INFO: change-only report check skipped (manual run or new branch); all documents validated')
                return
            module_paths = [path.rstrip('/') for m in config['modules'] for path in m['paths']]
            touched = {p for p in changed if any(p == prefix or p.startswith(prefix + '/') for prefix in module_paths)}
            contract_changes = {p for p in changed if p.startswith('docs/contracts/') and p.endswith('.json')}
            touched |= contract_changes
            updated = [json.loads(p.read_text()) for p in reports if p.relative_to(root).as_posix() in changed]
            covered = {p for report in updated for p in report.get('files', [])}
            require(touched <= covered, 'update a report listing changed module/contract files: ' + ', '.join(sorted(touched - covered)))
            referenced = {p for report in updated for p in report.get('contracts', [])}
            require((contract_changes & set(contracts)) <= referenced, 'changed contracts must be referenced by an updated report')

        check('changed module/contract report coverage', change_policy)
        for module in config['modules']:
            if not module['commands']:
                results.append(f"- NOT CONFIGURED: module {module['id']} executable checks")
            for index, argv in enumerate(module['commands']):
                check(f"module {module['id']} command {index + 1}", lambda a=argv: subprocess.run(a, cwd=root, check=True, timeout=300))
        for index, argv in enumerate(config['integration_commands']):
            check(f'integration command {index + 1}', lambda a=argv: subprocess.run(a, cwd=root, check=True, timeout=300))
        if not config['modules']:
            results.append('- NOT CONFIGURED: application modules/build/unit/contract tests; no app code registered')
        if not config['integration_commands']:
            results.append('- NOT CONFIGURED: application integration tests')

    sha = subprocess.run(['git', 'rev-parse', 'HEAD'], cwd=root, capture_output=True, text=True).stdout.strip() or 'local'
    summary = '# Team synchronization checks\n\nTested commit: `' + sha + '`\n\n' + '\n'.join(results) + '\n\nA green run covers configured checks only. NOT CONFIGURED does not mean application integration passed.\n'
    (root / '.team-sync-summary.md').write_text(summary)
    if os.environ.get('GITHUB_STEP_SUMMARY'):
        with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as output:
            output.write(summary)
    print(summary)
    return bool(failures)


if __name__ == '__main__':
    sys.exit(run(write_schedule='--write-schedule' in sys.argv, base=os.environ.get('SYNC_BASE')))
