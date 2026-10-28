from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'src'))

from fastapi.testclient import TestClient

from silver_ringo_d.action_registry import ActionRegistry
from silver_ringo_d.api import create_app
from silver_ringo_d.chat_service import ChatService
from silver_ringo_d.config import Settings
from silver_ringo_d.knowledge_store import KnowledgeStore
from silver_ringo_d.llm import FakeLLM, LLMError
from silver_ringo_d.retriever import LexicalRetriever


class ApiTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        store = KnowledgeStore(ROOT / 'knowledge')
        store.load()
        service = ChatService(LexicalRetriever(store), FakeLLM(),
                              ActionRegistry(), Settings())
        cls.client = TestClient(create_app(service))

    def test_health(self):
        response = self.client.get('/health')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), {'status': 'ok', 'knowledge_loaded': True})

    def test_chat_contract(self):
        response = self.client.post('/v1/chat', json={
            'message': '키오스크에서 카드 결제를 연습하고 싶어요.',
            'history': [],
        })
        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertEqual(body['sources'][0]['id'], 'course:kiosk:level:5')
        self.assertEqual(body['suggested_actions'], [])
        self.assertTrue(body['request_id'].startswith('local-'))

    def test_invalid_input_is_safe(self):
        response = self.client.post('/v1/chat', json={'message': '   '})
        self.assertEqual(response.status_code, 422)
        self.assertEqual(response.json()['error'], 'invalid_request')

    def test_llm_error_is_mapped(self):
        class FailingLLM:
            def generate(self, question, context, history):
                raise LLMError('rate_limit')
        store = KnowledgeStore(ROOT / 'knowledge')
        store.load()
        service = ChatService(LexicalRetriever(store), FailingLLM(),
                              ActionRegistry(), Settings())
        client = TestClient(create_app(service))
        response = client.post('/v1/chat', json={'message': '키오스크에서 카드 결제를 연습하고 싶어요.'})
        self.assertEqual(response.status_code, 429)
        self.assertEqual(response.json()['error'], 'rate_limited')


if __name__ == '__main__':
    unittest.main()
