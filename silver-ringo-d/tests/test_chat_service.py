from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'src'))

from silver_ringo_d.action_registry import ActionRegistry
from silver_ringo_d.chat_service import ChatService, NO_EVIDENCE_REPLY
from silver_ringo_d.config import Settings
from silver_ringo_d.knowledge_store import KnowledgeStore
from silver_ringo_d.llm import FakeLLM, LLMError
from silver_ringo_d.models import ChatRequest
from silver_ringo_d.retriever import LexicalRetriever


class ChatServiceTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.store = KnowledgeStore(ROOT / 'knowledge')
        cls.store.load()
        cls.settings = Settings()
        cls.service = ChatService(LexicalRetriever(cls.store), FakeLLM(),
                                  ActionRegistry(), cls.settings)

    def test_retrieval_and_fake_response(self):
        response = self.service.handle(ChatRequest('키오스크에서 카드 결제를 연습하고 싶어요.'))
        self.assertEqual(response.sources[0].id, 'course:kiosk:level:5')
        self.assertEqual(response.suggested_actions, [])
        self.assertTrue(response.reply.startswith('[모의 응답]'))
        self.assertTrue(response.request_id.startswith('local-'))

    def test_no_evidence_does_not_call_llm(self):
        class FailingLLM:
            def generate(self, question, context, history):
                raise AssertionError('LLM must not be called without evidence')
        service = ChatService(LexicalRetriever(self.store), FailingLLM(),
                              ActionRegistry(), self.settings)
        response = service.handle(ChatRequest('달에 가는 방법을 알려 주세요.'))
        self.assertEqual(response.reply, NO_EVIDENCE_REPLY)
        self.assertEqual(response.sources, [])

    def test_plan_question_can_include_idea_but_never_suggests_disabled_action(self):
        response = self.service.handle(ChatRequest('앞으로 전자증명서 기능도 만들 계획이야?'))
        self.assertEqual(response.suggested_actions, [])
        self.assertTrue(response.sources)

    def test_invalid_llm_response_is_explicit(self):
        class BadLLM:
            def generate(self, question, context, history):
                return '{"reply": "ok", "source_ids": ["unknown"]}'
        service = ChatService(LexicalRetriever(self.store), BadLLM(),
                              ActionRegistry(), self.settings)
        with self.assertRaises(LLMError) as raised:
            service.handle(ChatRequest('키오스크에서 카드 결제를 연습하고 싶어요.'))
        self.assertEqual(raised.exception.code, 'invalid_response')


if __name__ == '__main__':
    unittest.main()
