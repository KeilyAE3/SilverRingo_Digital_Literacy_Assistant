import sys
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'SilverRingo_Chat-bot' / 'src'))

from silver_ringo_d.action_registry import ActionRegistry
from silver_ringo_d.chat_service import ChatService
from silver_ringo_d.config import Settings
from silver_ringo_d.knowledge_store import KnowledgeStore
from silver_ringo_d.llm import FakeLLM, LLMError
from silver_ringo_d.models import ChatRequest
from silver_ringo_d.retriever import LexicalRetriever


def service(llm):
    store = KnowledgeStore(ROOT / 'SilverRingo_Chat-bot' / 'knowledge')
    store.load()
    return ChatService(LexicalRetriever(store), llm, ActionRegistry(), Settings())


class DChatServiceTests(unittest.TestCase):
    def test_app_answer_contains_evidence(self):
        """D-QA-01: 앱 설명 응답에 검색 근거가 포함된다."""
        response = service(FakeLLM()).handle(ChatRequest('실버링고는 어떤 앱이야?'))
        self.assertTrue(response.reply)
        self.assertTrue(any(source.id == 'app:overview' for source in response.sources))

    def test_learning_answer_contains_payment_evidence(self):
        """D-QA-02: 마이크로러닝 응답이 카드 결제 학습 근거를 포함한다."""
        response = service(FakeLLM()).handle(
            ChatRequest('키오스크에서 카드 결제하는 법을 배우고 싶어'))
        self.assertTrue(any(source.id == 'course:kiosk:level:5'
                            for source in response.sources))

    def test_assistant_answer_contains_ddocdoc_evidence(self):
        """D-QA-03: 병원 접수 안내 응답이 똑닥 근거를 포함한다."""
        response = service(FakeLLM()).handle(
            ChatRequest('똑닥으로 병원 접수를 하려는데 어려워'))
        self.assertTrue(any(source.id.startswith('guide:ddocdoc')
                            for source in response.sources))

    def test_unsupported_request_has_no_action(self):
        """D-QA-04: 정부24 요청에 현재 지원하지 않는 기능임을 안전하게 안내한다."""
        response = service(FakeLLM()).handle(ChatRequest('정부24로 등본을 발급해 줘'))
        self.assertEqual(response.suggested_actions, [])
        self.assertFalse(any(source.id.startswith('guide:retired')
                             for source in response.sources))
        self.assertIn('확인된 정보로는 지원 여부를 알 수 없어요', response.reply)

    def test_gemini_failure_becomes_safe_error(self):
        """D-QA-05: Gemini 호출 실패가 안전한 공통 오류로 변환된다."""
        class FailingLLM:
            def generate(self, question, context, history):
                raise LLMError('unavailable')
        with self.assertRaises(LLMError) as raised:
            service(FailingLLM()).handle(
                ChatRequest('키오스크에서 카드 결제하는 법을 배우고 싶어'))
        self.assertEqual(raised.exception.http_status, 503)
        self.assertNotIn('API', str(raised.exception))


if __name__ == '__main__':
    unittest.main()
