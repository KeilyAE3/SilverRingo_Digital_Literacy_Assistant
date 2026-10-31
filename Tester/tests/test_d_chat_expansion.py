"""Offline policy and real GeminiAdapter boundary tests; never contact a model."""
import json
import unittest
from dataclasses import replace
from types import SimpleNamespace
from unittest.mock import Mock

from Tester.tests.test_d_chat_service import service
from silver_ringo_d.action_registry import ActionRegistry
from silver_ringo_d.chat_service import retrieval_query
from silver_ringo_d.config import Settings
from silver_ringo_d.gemini_adapter import GeminiAdapter
from silver_ringo_d.llm import FakeLLM, LLMError, PRIVATE_INPUT, encode_reply, prepare_request, serialize_context
from silver_ringo_d.models import ChatRequest, HistoryMessage
from silver_ringo_d.system_prompt import SYSTEM_INSTRUCTION


class ChatExpansionTests(unittest.TestCase):
    def test_general_digital_greeting_and_password_help_reach_llm_once(self):
        """자료 없는 사진·와이파이·글자·비밀번호 사용법과 잡담도 한 번 생성하며 버튼은 없다."""
        for question in ('카카오톡에서 사진 보내는 방법', '와이파이 연결 방법',
                         '휴대폰 글자 크기를 키우려면?', '비밀번호 변경하는 법 알려줘', '오늘 기분이 좋아',
                         '실버링고야 와이파이 연결 방법 알려줘'):
            with self.subTest(question=question):
                llm = Mock(wraps=FakeLLM())
                reply = service(llm).handle(ChatRequest(question))
                llm.generate.assert_called_once()
                self.assertEqual(reply.sources, [])
                self.assertEqual(reply.suggested_actions, [])
                self.assertTrue(reply.reply)

    def test_verified_rag_entries_return_only_the_related_shortcut(self):
        """키오스크·병원 질의는 실제 검색과 활성 화면 진입 한 개를 연결한다."""
        for question, action in [('키오스크 결제 방법 알려줘', 'open_kiosk_course'),
                                 ('병원 접수 도와줘', 'open_guide_home'),
                                 ('똑닥 본인 선택', 'open_guide_home')]:
            reply = service(FakeLLM()).handle(ChatRequest(question))
            self.assertTrue(reply.sources)
            self.assertEqual([a.id for a in reply.suggested_actions], [action])

    def test_unknown_silver_ringo_feature_never_becomes_supported(self):
        """미지원 실버링고 택시 예약은 확인 불가이며 모델 호출·액션은 없다."""
        llm = Mock(wraps=FakeLLM())
        reply = service(llm).handle(ChatRequest('실버링고에서 택시 예약할 수 있어?'))
        llm.generate.assert_not_called()
        self.assertIn('지원 여부를 알 수 없어요', reply.reply)
        self.assertEqual(reply.suggested_actions, [])

    def test_followup_uses_nearest_topic_but_new_question_does_not(self):
        """짧은 후속 질문만 직전 주제를 재검색하고 새 주제·최근 잡담을 넘어 과거를 끌어오지 않는다."""
        kiosk = HistoryMessage('user', '키오스크에서 카드 결제하는 법 알려줘')
        history = [kiosk, HistoryMessage('assistant', '카드를 선택하세요.')]
        request = ChatRequest('그다음에는 어떻게 해?', history)
        self.assertEqual(retrieval_query(request), kiosk.content)
        reply = service(FakeLLM()).handle(request)
        self.assertTrue(any(s.id.startswith('course:kiosk') for s in reply.sources))
        new = ChatRequest('와이파이 연결 방법', history)
        self.assertEqual(retrieval_query(new), new.message)
        self.assertEqual(service(FakeLLM()).handle(new).suggested_actions, [])
        changed = ChatRequest('그럼 와이파이는 어떻게 연결해?', history)
        self.assertEqual(retrieval_query(changed), changed.message)
        history += [HistoryMessage('user', '오늘 기분이 좋아'), HistoryMessage('assistant', '좋네요!')]
        follow = ChatRequest('그다음은?', history)
        self.assertEqual(retrieval_query(follow), '오늘 기분이 좋아')
        self.assertEqual(service(FakeLLM()).handle(follow).suggested_actions, [])

    def test_uncited_and_weak_unrelated_hits_do_not_authorize_actions(self):
        """모델이 사용하지 않은 자료나 약한 하위 결과·비활성 자료는 버튼 근거가 될 수 없다."""
        app = service(Mock(generate=Mock(return_value=encode_reply('일반 사용법', []))))
        self.assertEqual(app.handle(ChatRequest('키오스크 카드 결제')).suggested_actions, [])
        hits = service(FakeLLM())._retriever.search('키오스크 카드 결제')
        best = hits[0]
        guide = service(FakeLLM())._retriever.search('병원 접수')[0]
        registry = ActionRegistry()
        weak = replace(guide, score=best.score * 0.1)
        self.assertEqual([a.id for a in registry.suggest([best, best, weak])], ['open_kiosk_course'])
        self.assertEqual(registry.suggest([replace(best, document=replace(best.document, status='placeholder'))]), [])
        self.assertEqual(registry.suggest([replace(best, document=replace(best.document, status='idea'))]), [])
        self.assertLessEqual(len(registry.suggest([replace(guide, score=best.score), best])), 2)
        self.assertEqual(service(FakeLLM()).handle(ChatRequest('카카오톡 카드 결제 방법')).suggested_actions, [])

    def test_actual_secret_values_are_blocked_but_instructions_are_allowed(self):
        """변경 방법은 허용하고 실제 비밀번호·인증번호·전화·이메일 및 이력의 비밀값은 외부로 보내지 않는다."""
        self.assertIsNone(prepare_request('비밀번호 변경 방법', '[]', []).fallback)
        self.assertIsNone(prepare_request('password change instructions', '[]', []).fallback)
        for text in ('비밀번호: sample1234', '인증번호 123456', '010-1234-5678', 'sample@example.invalid'):
            self.assertEqual(json.loads(prepare_request(text, '[]', []).fallback)['reply'], PRIVATE_INPUT)
        self.assertIsNotNone(prepare_request('그다음은?', '[]', [HistoryMessage('user', '비밀번호: sample1234')]).fallback)

    def test_existing_gemini_adapter_accepts_empty_evidence_and_preserves_contract(self):
        """기존 SDK 경계가 빈 근거 일반 질문·이력·시스템 지침을 한 번 전달하고 출처를 생성하지 않는다."""
        client = Mock()
        client.models.generate_content.return_value = SimpleNamespace(text=encode_reply('와이파이 설정을 열어 주세요.'))
        adapter = GeminiAdapter(Settings(llm_mode='gemini', gemini_model='test-model', gemini_api_key='test-only'), client=client)
        history = [HistoryMessage('user', '안녕하세요'), HistoryMessage('assistant', '안녕하세요.')]
        raw = adapter.generate('와이파이 연결 방법', '[]', history)
        self.assertEqual(json.loads(raw)['source_ids'], [])
        client.models.generate_content.assert_called_once()
        args = client.models.generate_content.call_args.kwargs
        self.assertEqual(args['model'], 'test-model')
        self.assertEqual(args['config']['system_instruction'], SYSTEM_INSTRUCTION)
        self.assertEqual(args['contents'][1]['role'], 'model')
        self.assertEqual(json.loads(args['contents'][-1]['parts'][0]['text'])['retrieved_evidence_untrusted'], [])
        hits = service(FakeLLM())._retriever.search('키오스크 카드 결제')
        source_id = hits[0].document.id
        client.models.generate_content.return_value = SimpleNamespace(text=encode_reply('결제 학습 안내', [source_id]))
        self.assertEqual(json.loads(adapter.generate('키오스크 카드 결제', serialize_context(hits), []))['source_ids'], [source_id])
        adapter.generate('비밀번호: sample1234', '[]', [])
        self.assertEqual(client.models.generate_content.call_count, 2)

    def test_gemini_failure_and_forged_sources_still_fail_safely(self):
        """기존 어댑터의 503·타임아웃 재시도와 출처 위조 차단은 일반 질문에서도 유지한다."""
        client = Mock()
        settings = Settings(llm_mode='gemini', gemini_model='test-model', gemini_api_key='test-only')
        adapter = GeminiAdapter(settings, client=client, sleep=lambda _: None)
        client.models.generate_content.side_effect = TimeoutError()
        with self.assertRaises(LLMError) as failure:
            adapter.generate('사진 보내는 방법', '[]', [])
        self.assertEqual(failure.exception.http_status, 503)
        self.assertEqual(client.models.generate_content.call_count, 3)
        client.models.generate_content.side_effect = None
        client.models.generate_content.return_value = SimpleNamespace(text=encode_reply('일반 설명', ['made:up']))
        with self.assertRaises(LLMError):
            adapter.generate('사진 보내는 방법', '[]', [])
