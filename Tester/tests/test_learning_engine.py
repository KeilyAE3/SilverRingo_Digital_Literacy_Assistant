"""Run the existing JavaScript learning checks through Team Sync registration."""
from pathlib import Path
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[2]


class LearningEngineTests(unittest.TestCase):
    def run_node(self, *args):
        result = subprocess.run(
            ["node", *args], cwd=ROOT, capture_output=True,
            text=True, encoding="utf-8", timeout=60,
        )
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_existing_learning_engine(self):
        """실제 JS 엔진: 주문·가격·단계 완료·잠금 등 기존 11개 회귀 검사가 통과한다."""
        self.run_node("--test", "silver-ringo-c/tests/engine.test.cjs")

    def test_web_and_android_lesson_content_match(self):
        """콘텐츠 연결: 웹이 쓰는 content.js와 Android 목록용 content.json이 일치한다."""
        self.run_node("-e", """
            const fs = require('node:fs');
            const assert = require('node:assert/strict');
            assert.deepEqual(
                JSON.parse(fs.readFileSync('silver-ringo-c/web/content.json', 'utf8')),
                require('./silver-ringo-c/web/content.js')
            );
        """)
