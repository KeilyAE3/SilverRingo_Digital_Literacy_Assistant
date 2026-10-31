"""Exercise the existing FastAPI boundary, not a second chatbot implementation."""
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'SilverRingo_Chat-bot' / 'src'))

from fastapi.testclient import TestClient
from silver_ringo_d.api import create_app
from silver_ringo_d.llm import FakeLLM, LLMError
from Tester.tests.test_d_chat_service import service


class ChatHttpTests(unittest.TestCase):
    def test_http_contract_and_request_validation(self):
        """Android contract: POST JSON returns reply/sources/actions/id; invalid input is rejected."""
        with TestClient(create_app(service(FakeLLM()))) as client:
            self.assertTrue(client.get('/health').json()['knowledge_loaded'])
            response = client.post('/v1/chat', json={
                'message': '키오스크 카드 결제를 배우고 싶어',
                'history': [{'role': 'user', 'content': '키오스크가 어려워'}],
            })
            self.assertEqual(response.status_code, 200)
            payload = response.json()
            self.assertEqual(set(payload), {'reply', 'sources', 'suggested_actions', 'request_id'})
            self.assertTrue(payload['reply'])
            self.assertTrue(any(s['id'] == 'course:kiosk:level:5' for s in payload['sources']))
            self.assertEqual(payload['suggested_actions'], [])
            for body in ({'message': ' '}, {'message': 'a' * 1001},
                         {'message': '질문', 'history': [{'role': 'system', 'content': 'x'}]},
                         {'message': '질문', 'extra': 'not allowed'}):
                self.assertEqual(client.post('/v1/chat', json=body).status_code, 422)

    def test_llm_failure_is_a_safe_http_error(self):
        """Unavailable provider returns the existing 503 JSON error without provider diagnostics."""
        class Unavailable:
            def generate(self, *args):
                raise LLMError('unavailable')
        with TestClient(create_app(service(Unavailable()))) as client:
            response = client.post('/v1/chat', json={'message': '키오스크 카드 결제'})
            self.assertEqual(response.status_code, 503)
            self.assertEqual(response.json()['error'], 'llm_unavailable')
            self.assertEqual(set(response.json()), {'error', 'message'})
