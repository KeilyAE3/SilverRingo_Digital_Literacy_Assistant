"""Pure orchestration of retrieval, response generation and action suggestions."""
import json
import re
from uuid import uuid4

from .action_registry import ActionRegistry
from .llm import LLMClient, LLMError, serialize_context
from .models import ChatRequest, ChatResponse, ResponseSource
from .normalizer import normalize


NO_EVIDENCE_REPLY = '앱에서 확인된 정보로는 지원 여부를 알 수 없어요. 궁금한 기능을 조금 더 자세히 말씀해 주세요.'
_PLAN_TERMS = ('앞으로', '향후', '예정', '계획', '구상', '추가될', '지원할 예정')

# Only short referential questions borrow the nearest substantive user topic.
_FOLLOWUP = re.compile(r'^(?:(?:그)?다음(?:에는|은|에|엔)?(?:\s*어떻게\s*해)?|그럼(?:\s*어떻게\s*해)?|그건\s*어떻게\s*해|(?:더\s*)?자세히(?:\s*알려줘)?|다시(?:\s*설명해줘)?|다른\s*방법은?)$')


def retrieval_query(request):
    current = normalize(request.message)
    if len(current) <= 40 and _FOLLOWUP.match(current):
        for turn in reversed(request.history):
            if turn.role == 'user' and not _FOLLOWUP.match(normalize(turn.content)):
                return turn.content
    return request.message


class ChatService:
    def __init__(self, retriever, llm_client: LLMClient,
                 action_registry: ActionRegistry, settings):
        if not hasattr(retriever, 'search'):
            raise TypeError('retriever must provide search')
        if not hasattr(llm_client, 'generate'):
            raise TypeError('llm_client must provide generate')
        if not isinstance(action_registry, ActionRegistry):
            raise TypeError('action_registry must be an ActionRegistry')
        if not hasattr(settings, 'top_k'):
            raise TypeError('settings must provide top_k')
        self._retriever = retriever
        self._llm = llm_client
        self._actions = action_registry
        self._settings = settings

    def handle(self, request: ChatRequest) -> ChatResponse:
        if not isinstance(request, ChatRequest):
            raise TypeError('request must be a ChatRequest')
        query = retrieval_query(request)
        include_ideas = any(term in query.casefold() for term in _PLAN_TERMS)
        hits = self._retriever.search(query, top_k=self._settings.top_k,
                                      include_ideas=include_ideas)
        if not isinstance(hits, list):
            raise ValueError('retriever must return a list')
        request_id = 'local-' + uuid4().hex
        topic = normalize(query).replace(' ', '')
        app_named = any(name in topic for name in ('실버링고', '실버린고', 'silverringo', '이앱'))
        app_question = app_named and any(term in topic for term in ('에서', '에는', '지원', '할수', '가능', '있어', '어디'))
        if not hits and app_question:
            return ChatResponse(NO_EVIDENCE_REPLY, [], [], request_id)

        raw = self._llm.generate(request.message, serialize_context(hits), request.history)
        try:
            payload = json.loads(raw)
        except (TypeError, ValueError):
            raise LLMError('invalid_response') from None
        if (not isinstance(payload, dict) or set(payload) != {'reply', 'source_ids'}
                or not isinstance(payload['reply'], str) or not payload['reply'].strip()):
            raise LLMError('invalid_response')
        source_by_id = {hit.document.id: hit.document for hit in hits}
        source_ids = payload.get('source_ids', [])
        if not isinstance(source_ids, list) or any(source_id not in source_by_id for source_id in source_ids):
            raise LLMError('invalid_response')
        sources = [ResponseSource(source_id, source_by_id[source_id].category,
                                  source_by_id[source_id].status)
                   for source_id in source_ids]
        threshold = max(4.0, max((hit.score for hit in hits), default=0) * 0.75)
        shortcut_topic = any(term in topic for term in (
            '키오스크', '무인계산대', '주문기계', '셀프주문', '병원', '똑닥', '접수',
            '실버링고', 'silverringo', '학습', '배우기', '연습', '생활가이드', '가이드기능'))
        cited = [hit for hit in hits if shortcut_topic and hit.document.id in source_ids and hit.score >= threshold]
        return ChatResponse(payload['reply'], sources, self._actions.suggest(cited), request_id)
