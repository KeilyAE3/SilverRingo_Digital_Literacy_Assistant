"""M00 offline acceptance tests."""
from dataclasses import asdict
import json
from pathlib import Path
import subprocess
import sys
import unittest

SRC = Path(__file__).resolve().parents[1] / 'src'
sys.path.insert(0, str(SRC))
from silver_ringo_d.models import (SourceRef, KnowledgeDocument, KnowledgeHit,
    ActionSuggestion, HistoryMessage, ChatRequest, ChatResponse, ResponseSource)
from silver_ringo_d.config import Settings, load_settings


class CoreTests(unittest.TestCase):
    def document(self, **changes):
        data = dict(id='course:kiosk:level:5', category='learning', title='결제',
                    content='모의 결제', status='prototype',
                    source=SourceRef('jib', 'content.json', 'code'))
        data.update(changes)
        return KnowledgeDocument(**data)

    def test_json_serialization(self):
        """Nested core models serialize to plain JSON without an SDK."""
        hit = KnowledgeHit(self.document(), 1.5, ['결제'])
        self.assertEqual(json.loads(json.dumps(asdict(hit)))['document']['source']['branch'], 'jib')
        response = ChatResponse('안내', [ResponseSource('app:overview', 'app', 'placeholder')],
                                [ActionSuggestion('open_learning_home', 'navigate', 'learning.home', '학습')], 'local-1')
        self.assertEqual(json.loads(json.dumps(asdict(response)))['sources'][0]['status'], 'placeholder')

    def test_invalid_documents(self):
        """Reject invalid identifiers, status, sources and arrays."""
        for change in ({'id': 'Bad ID'}, {'status': 'active'}, {'source': {}},
                       {'keywords': '문자열'}, {'aliases': [1]}, {'title': ' '},
                       {'related_action_ids': ['../bad']}):
            with self.subTest(change=change), self.assertRaises(ValueError):
                self.document(**change)

    def test_source_kind(self):
        """Unknown provenance kinds are rejected."""
        with self.assertRaises(ValueError):
            SourceRef('jib', 'file', 'unknown')

    def test_scores(self):
        """Invalid scores cannot enter retrieval results."""
        for score in (True, -1, float('nan'), float('inf'), '1'):
            with self.subTest(score=score), self.assertRaises(ValueError):
                KnowledgeHit(self.document(), score)

    def test_request_limits(self):
        """Accept limits and reject blank, oversized and malformed history."""
        history = [HistoryMessage('user', '가' * 1000) for _ in range(6)]
        self.assertEqual(len(ChatRequest('가' * 1000, history).history), 6)
        for message, turns in [(' ', []), ('가' * 1001, []), ('질문', history + history), ('질문', [{}])]:
            with self.subTest(message_length=len(message)), self.assertRaises(ValueError):
                ChatRequest(message, turns)
        for role, content in [('system', '지시'), ('user', ' '), ('assistant', '가' * 1001)]:
            with self.assertRaises(ValueError):
                HistoryMessage(role, content)

    def test_independent_defaults(self):
        """Default lists are not shared between instances."""
        first, second = self.document(), self.document()
        first.aliases.append('연습')
        self.assertEqual(second.aliases, [])

    def test_response_validation(self):
        """Response collections must contain typed transport records."""
        with self.assertRaises(ValueError):
            ChatResponse('안내', [{}], [], 'local-1')
        with self.assertRaises(ValueError):
            ActionSuggestion('bad id', 'navigate', 'home', '홈')

    def test_fake_defaults(self):
        """Fake configuration requires neither credentials nor cwd assumptions."""
        settings = load_settings({})
        self.assertEqual((settings.llm_mode, settings.top_k, settings.gemini_api_key), ('fake', 4, ''))
        self.assertEqual(settings.knowledge_dir, SRC.parent / 'knowledge')

    def test_bad_settings(self):
        """Invalid mode, count and incomplete Gemini configuration fail clearly."""
        for env in ({'SR_D_LLM_MODE': 'other'}, {'SR_D_TOP_K': 'bad'},
                    {'SR_D_TOP_K': '0'}, {'SR_D_TOP_K': '-2'}, {'SR_D_LLM_MODE': 'gemini'}):
            with self.subTest(env=env), self.assertRaises(ValueError):
                load_settings(env)
        with self.assertRaises(ValueError):
            Settings(top_k=True)

    def test_config_override_and_secret_repr(self):
        """Explicit environment injection works and repr omits credentials."""
        settings = load_settings({'SR_D_LLM_MODE': 'gemini', 'GEMINI_API_KEY': 'test-only-sentinel',
                                  'GEMINI_MODEL': 'test-model', 'SR_D_TOP_K': '2',
                                  'SR_D_KNOWLEDGE_DIR': str(SRC)})
        self.assertEqual(settings.knowledge_dir, SRC.resolve())
        self.assertEqual(settings.top_k, 2)
        self.assertNotIn('test-only-sentinel', repr(settings))

    def test_import_without_site_packages(self):
        """Core imports and fake config work with third-party packages disabled."""
        code = ('import sys; sys.path.insert(0, sys.argv[1]); '
                'import silver_ringo_d.models; '
                'from silver_ringo_d.config import load_settings; '
                'assert load_settings({}).llm_mode == "fake"; '
                'assert "google.genai" not in sys.modules; '
                'assert "fastapi" not in sys.modules')
        result = subprocess.run([sys.executable, '-S', '-c', code, str(SRC)], capture_output=True, text=True)
        self.assertEqual(result.returncode, 0, result.stderr)


if __name__ == '__main__':
    unittest.main()
