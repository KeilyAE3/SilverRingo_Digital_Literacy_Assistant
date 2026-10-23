import importlib.util
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import json

spec = importlib.util.spec_from_file_location('checks', Path(__file__).resolve().parents[1] / 'check.py')
checks = importlib.util.module_from_spec(spec)
spec.loader.exec_module(checks)


class ValidationTests(unittest.TestCase):
    def task(self, name, start='2026-10-24', after=None):
        return dict(id=name, title=name, owner='member', start=start, days=1, after=after or [], status='todo')

    def test_dependency_cycle_rejected(self):
        with self.assertRaisesRegex(ValueError, 'cyclic'):
            checks.render_schedule({'tasks': [self.task('a', after=['b']), self.task('b', after=['a'])]})

    def test_missing_dependency_rejected(self):
        with self.assertRaisesRegex(ValueError, 'unknown'):
            checks.render_schedule({'tasks': [self.task('a', after=['missing'])]})

    def test_overlapping_dependency_rejected(self):
        with self.assertRaisesRegex(ValueError, 'starts before'):
            checks.render_schedule({'tasks': [self.task('a'), self.task('b', after=['a'])]})

    def test_valid_schedule_dependency_order(self):
        output = checks.render_schedule({'tasks': [self.task('b', '2026-10-25', ['a']), self.task('a')]})
        self.assertLess(output.index('a :a'), output.index('b :b'))

    def test_contract_unknown_consumer_rejected(self):
        data = dict(id='test', version='1', provider='a', consumers=['typo'], inputs='x', outputs='y', errors='e', compatibility='c')
        with self.assertRaisesRegex(ValueError, 'consumer'):
            checks.validate_contract(data, {'a', 'b'})

    def test_missing_report_fields_rejected(self):
        with self.assertRaisesRegex(ValueError, 'missing'):
            checks.validate_report(Path('.'), {'owner': 'a'}, set())

    def test_path_escape_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaisesRegex(ValueError, 'escapes'):
                checks.local_path(Path(directory), '../outside', must_exist=False)

    def test_shell_string_command_rejected(self):
        with self.assertRaises(ValueError):
            checks.commands(['npm test'])

    def test_broken_markdown_link_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'README.md').write_text('[broken](missing.md)')
            with self.assertRaisesRegex(ValueError, 'missing path'):
                checks.markdown_links(root)

    def fixture(self, directory):
        root = Path(directory)
        for path in ('automation', 'docs/schedule', 'docs/contracts', 'docs/reports/alice', 'app'):
            (root / path).mkdir(parents=True, exist_ok=True)
        (root / 'app/module.py').write_text('')
        (root / 'automation/config.json').write_text(json.dumps({'modules': [{'id': 'app', 'paths': ['app'], 'commands': []}], 'integration_commands': []}))
        (root / 'automation/schedule.json').write_text('{"tasks": []}')
        (root / 'docs/schedule/TEAM_GANTT.md').write_text(checks.render_schedule({'tasks': []}))
        return root

    def test_module_change_without_updated_report_fails(self):
        with tempfile.TemporaryDirectory() as directory:
            root = self.fixture(directory)
            with patch.object(checks, 'changed_files', return_value={'app/module.py'}), patch.dict(checks.os.environ, {'GITHUB_STEP_SUMMARY': ''}), patch('builtins.print'):
                self.assertTrue(checks.run(root))

    def test_module_change_with_updated_report_passes(self):
        with tempfile.TemporaryDirectory() as directory:
            root = self.fixture(directory)
            report = dict(owner='alice', task='x', summary='change', approach='approach', interface_changes='none', verification='not run', files=['app/module.py'], contracts=[], blocked_by=[], next_steps=['none'])
            (root / 'docs/reports/alice/x.json').write_text(json.dumps(report))
            with patch.object(checks, 'changed_files', return_value={'app/module.py', 'docs/reports/alice/x.json'}), patch.dict(checks.os.environ, {'GITHUB_STEP_SUMMARY': ''}), patch('builtins.print'):
                self.assertFalse(checks.run(root))


if __name__ == '__main__':
    unittest.main()
