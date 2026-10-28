"""Pure orchestration of retrieval, response generation and action suggestions."""
import json
from uuid import uuid4

from .action_registry import ActionRegistry
from .llm import LLMClient, LLMError, serialize_context
from .models import ChatRequest, ChatResponse, ResponseSource


NO_EVIDENCE_REPLY = '앱에서 확인된 정보로는 지원 여부를 알 수 없어요. 궁금한 기능을 조금 더 자세히 말씀해 주세요.'
_PLAN_TERMS = ('앞으로', '향후', '예정', '계획', '구상', '추가될', '지원할 예정')


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
        include_ideas = any(term in request.message.casefold() for term in _PLAN_TERMS)
        hits = self._retriever.search(request.message, top_k=self._settings.top_k,
                                      include_ideas=include_ideas)
        if not isinstance(hits, list):
            raise ValueError('retriever must return a list')
        request_id = 'local-' + uuid4().hex
        if not hits:
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
        return ChatResponse(payload['reply'], sources, self._actions.suggest(hits), request_id)
