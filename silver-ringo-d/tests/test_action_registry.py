import json
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'src'))

from silver_ringo_d.action_registry import ActionRegistry, ActionRegistryError
from silver_ringo_d.models import KnowledgeDocument, KnowledgeHit, SourceRef


def hit(*action_ids):
    doc = KnowledgeDocument(
        id='app:test', category='app', title='테스트', content='테스트',
        status='implemented', source=SourceRef('test', 'test.json', 'code'),
        related_action_ids=list(action_ids))
    return KnowledgeHit(doc, 1.0)


class ActionRegistryTests(unittest.TestCase):
    def test_default_catalog_has_no_executable_actions(self):
        registry = ActionRegistry()
        self.assertIsNone(registry.get('open_kiosk_course'))
        self.assertFalse(registry.validate('open_kiosk_course'))
        self.assertEqual(registry.suggest([hit('open_kiosk_course')]), [])

    def test_enabled_route_is_validated_and_suggested(self):
        registry = ActionRegistry([{
            'id': 'open_kiosk_course', 'kind': 'navigate',
            'target': 'learning.kiosk', 'label': '키오스크 배우기',
            'enabled': True, 'status': 'integrated',
        }])
        self.assertTrue(registry.validate('open_kiosk_course'))
        self.assertEqual(registry.suggest([hit('open_kiosk_course'), hit('open_kiosk_course')])[0].id,
                         'open_kiosk_course')

    def test_disabled_unknown_and_parameters_are_rejected(self):
        registry = ActionRegistry([{
            'id': 'open_kiosk_course', 'kind': 'navigate',
            'target': 'learning.kiosk', 'label': '키오스크 배우기',
            'enabled': False, 'status': 'unintegrated',
        }])
        self.assertFalse(registry.validate('missing'))
        self.assertFalse(registry.validate('open_kiosk_course', {}))
        self.assertFalse(registry.validate('open_kiosk_course', {'level': 5}))

    def test_external_targets_are_not_executable(self):
        registry = ActionRegistry([{
            'id': 'open_external', 'kind': 'navigate',
            'target': 'https://example.com', 'label': '외부',
            'enabled': True, 'status': 'integrated',
        }])
        self.assertIsNone(registry.get('open_external'))

    def test_duplicate_ids_and_invalid_catalog_fail_explicitly(self):
        record = {'id': 'open_a', 'kind': 'navigate', 'target': 'app.home',
                  'label': 'A', 'enabled': False, 'status': 'unintegrated'}
        with self.assertRaises(ActionRegistryError):
            ActionRegistry([record, dict(record)])
        path = ROOT / 'tests' / 'bad-action-catalog.json'
        path.write_text(json.dumps({'schema_version': 2, 'actions': []}), encoding='utf-8')
        try:
            with self.assertRaises(ActionRegistryError):
                ActionRegistry(path)
        finally:
            path.unlink()


if __name__ == '__main__':
    unittest.main()
