import sys
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'SilverRingo_Chat-bot' / 'src'))

from silver_ringo_d.action_registry import ActionRegistry
from silver_ringo_d.knowledge_store import KnowledgeStore


class DKnowledgeTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.knowledge_dir = ROOT / 'SilverRingo_Chat-bot' / 'knowledge'
        cls.store = KnowledgeStore(cls.knowledge_dir)
        cls.store.load()

    def test_json_resources_load_as_complete_snapshot(self):
        """D-QA-06: 네 개 JSON 자료가 손상 없이 하나의 검증된 스냅샷으로 적재된다."""
        self.assertGreater(len(self.store.documents()), 0)
        self.assertEqual(len(self.store.actions()), 4)
        for name in ('app_info.json', 'learning_courses.json',
                     'assistant_guides.json', 'action_catalog.json'):
            self.assertTrue((self.knowledge_dir / name).is_file())

    def test_example_knowledge_is_not_runtime_knowledge(self):
        """M07 더미 자료는 별도 example 경로에 있고 기본 런타임 자료 수에 영향을 주지 않는다."""
        example = ROOT / 'Tester' / 'example' / 'knowledge' / 'm07_examples.json'
        self.assertTrue(example.is_file())
        self.assertNotEqual(example.parent, self.knowledge_dir)
        self.assertNotIn('example:course:payment',
                         {document.id for document in self.store.documents()})

    def test_only_verified_screen_entries_are_enabled(self):
        """D-QA-06: 통합된 두 화면만 열며 서비스 자동 시작과 중복 학습 진입은 비활성이다."""
        registry = ActionRegistry(self.knowledge_dir / 'action_catalog.json')
        self.assertIsNotNone(registry.get('open_kiosk_course'))
        self.assertIsNotNone(registry.get('open_guide_home'))
        self.assertIsNone(registry.get('open_learning_home'))
        self.assertIsNone(registry.get('start_ddocdoc_companion'))


if __name__ == '__main__':
    unittest.main()
