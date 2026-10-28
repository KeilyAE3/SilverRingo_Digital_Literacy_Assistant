import sys
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'silver-ringo-d' / 'src'))

from silver_ringo_d.action_registry import ActionRegistry


class DActionTests(unittest.TestCase):
    def test_unknown_and_disabled_actions_are_blocked(self):
        """D-QA-04: 미등록·비활성 액션은 바로가기 실행 후보가 될 수 없다."""
        registry = ActionRegistry(ROOT / 'silver-ringo-d' / 'knowledge' / 'action_catalog.json')
        self.assertFalse(registry.validate('unknown_action'))
        self.assertFalse(registry.validate('open_kiosk_course'))
        self.assertFalse(registry.validate('open_kiosk_course', {'level': 5}))


if __name__ == '__main__':
    unittest.main()
