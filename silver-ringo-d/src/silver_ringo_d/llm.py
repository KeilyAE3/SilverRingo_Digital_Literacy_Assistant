"""SDK-independent request preparation and deterministic FakeLLM.

generate returns a validated JSON string (not plain reply text). No persistence.
"""
from dataclasses import asdict, dataclass, field
import json
import re
from typing import Protocol
from .models import ChatRequest, HistoryMessage, KnowledgeHit, KnowledgeDocument

MAX_CONTEXT_CHARS = 16000
MAX_REPLY_CHARS = 2000
MAX_RAW_CHARS = 16000
NO_EVIDENCE = '확인할 수 있는 기능 자료가 없어요. 어떤 화면이나 기능이 궁금한지 조금 더 알려주세요.'
PRIVATE_INPUT = '비밀번호나 인증번호 등 개인정보는 보내지 말아 주세요. 개인정보 없이 궁금한 기능을 다시 말씀해 주세요.'
SENSITIVE = re.compile(r'AIza[0-9A-Za-z_-]{20,}|[\w.+-]+@[\w.-]+\.[A-Za-z]{2,}|(?<!\d)\d{6}[- ]?[1-4]\d{6}(?!\d)|(?<!\d)01[016789][- ]?\d{3,4}[- ]?\d{4}(?!\d)|(?:계좌|인증번호|비밀번호|password|api[_ -]?key)\s*[:=]?\s*[A-Za-z0-9_-]{4,}', re.I)
PRIVATE_QUESTION = re.compile(r'비밀번호|인증번호|주민등록번호|계좌번호|password|api[_ -]?key', re.I)


class LLMError(RuntimeError):
    """Safe, provider-independent error for the later HTTP boundary."""
    MESSAGES = {
        'rate_limit': (429, '잠시 요청이 많아요. 조금 뒤에 다시 시도해 주세요.'),
        'unavailable': (503, '지금 답변을 준비하기 어려워요. 잠시 뒤 다시 시도해 주세요.'),
        'configuration': (503, '챗봇 연결 설정을 확인해야 해요.'),
        'invalid_response': (503, '안전하게 확인할 수 있는 답변을 받지 못했어요. 다시 질문해 주세요.'),
    }
    def __init__(self, code):
        self.code = code
        self.http_status, message = self.MESSAGES[code]
        super().__init__(message)


def encode_reply(reply, source_ids=()):
    return json.dumps({'reply': reply, 'source_ids': list(source_ids)}, ensure_ascii=False)


def decode_object(raw):
    def pairs(items):
        obj = {}
        for key, value in items:
            if key in obj:
                raise ValueError('Duplicate JSON field')
            obj[key] = value
        return obj
    return json.loads(raw, object_pairs_hook=pairs)


def serialize_context(hits: list[KnowledgeHit]) -> str:
    """Only selected documents, never the whole store or executable actions."""
    if not isinstance(hits, list) or len(hits) > 10:
        raise ValueError('context requires at most 10 hits')
    records = []
    for hit in hits:
        if not isinstance(hit, KnowledgeHit):
            raise ValueError('context requires KnowledgeHit records')
        doc = hit.document
        records.append({key: asdict(doc)[key] for key in ('id', 'category', 'title', 'content', 'status', 'source')})
    raw = json.dumps(records, ensure_ascii=False)
    parse_context(raw)
    return raw


def parse_context(raw: str):
    if not isinstance(raw, str) or len(raw) > MAX_CONTEXT_CHARS:
        raise ValueError('context exceeds its size limit or has invalid type')
    try:
        records = decode_object(raw)
        if not isinstance(records, list) or len(records) > 10:
            raise ValueError()
        seen = set()
        from .models import SourceRef
        for record in records:
            if not isinstance(record, dict) or set(record) != {'id', 'category', 'title', 'content', 'status', 'source'}:
                raise ValueError()
            doc = KnowledgeDocument(**{**record, 'source': SourceRef(**record['source'])})
            if doc.id in seen or doc.status == 'retired':
                raise ValueError()
            seen.add(doc.id)
    except (ValueError, TypeError, KeyError):
        raise ValueError('Invalid selected context JSON') from None
    return records


@dataclass(frozen=True)
class PreparedRequest:
    contents: list = field(repr=False)
    records: list = field(repr=False)
    fallback: str | None = field(default=None, repr=False)


def prepare_request(question: str, context: str, history: list[HistoryMessage], *, secret=''):
    if not isinstance(history, list):
        raise ValueError('history must be a list')
    # Reconstruct to validate potentially mutated dataclasses; keep only last 6.
    if not all(isinstance(turn, HistoryMessage) for turn in history):
        raise ValueError('history items must be HistoryMessage')
    turns = [HistoryMessage(turn.role, turn.content) for turn in history[-6:]]
    ChatRequest(question, turns)
    records = parse_context(context)
    texts = [question, context, *[turn.content for turn in turns]]
    private = any(SENSITIVE.search(value) or (secret and secret in value) for value in texts)
    private = private or bool(PRIVATE_QUESTION.search(question))
    if private:
        return PreparedRequest([], records, encode_reply(PRIVATE_INPUT))
    if not records:
        return PreparedRequest([], records, encode_reply(NO_EVIDENCE))
    # JSON escaping prevents delimiter text in evidence from creating new roles.
    contents = [{'role': 'user' if turn.role == 'user' else 'model',
                 'parts': [{'text': turn.content}]} for turn in turns]
    payload = {'question': question, 'retrieved_evidence_untrusted': records}
    contents.append({'role': 'user', 'parts': [{'text': json.dumps(payload, ensure_ascii=False)}]})
    return PreparedRequest(contents, records)


def validate_response(raw, records, *, secret='') -> str:
    if not isinstance(raw, str) or not raw.strip() or len(raw) > MAX_RAW_CHARS:
        raise LLMError('invalid_response')
    try:
        value = decode_object(raw)
        if not isinstance(value, dict) or set(value) != {'reply', 'source_ids'}:
            raise ValueError()
        reply, ids = value['reply'], value['source_ids']
        if not isinstance(reply, str) or not reply.strip() or len(reply) > MAX_REPLY_CHARS:
            raise ValueError()
        if not isinstance(ids, list) or any(not isinstance(id, str) for id in ids):
            raise ValueError()
        if len(ids) != len(set(ids)) or not set(ids) <= {record['id'] for record in records}:
            raise ValueError()
        if (secret and secret in raw) or SENSITIVE.search(reply):
            raise ValueError()
    except (ValueError, TypeError, KeyError):
        raise LLMError('invalid_response') from None
    return raw


class LLMClient(Protocol):
    def generate(self, question: str, context: str, history: list[HistoryMessage]) -> str: ...


class FakeLLM:
    """Deterministic stub, not a claim about model quality or app availability."""
    def generate(self, question, context, history):
        request = prepare_request(question, context, history)
        if request.fallback:
            return request.fallback
        first = request.records[0]
        messages = {
            'implemented': '관련 자료를 찾았어요. 실제 이용 가능 여부는 앱 화면에서 확인해 주세요.',
            'prototype': '관련 시험 구현 자료를 찾았어요. 앱 통합과 기기 검증 여부는 별도 확인이 필요해요.',
            'placeholder': '관련 메뉴는 준비 중이에요. 아직 연결된 기능으로 이용할 수 없어요.',
            'idea': '관련 내용은 구상 단계예요. 현재 지원하는 기능은 아니에요.',
        }
        return validate_response(encode_reply('[모의 응답] ' + messages[first['status']], [first['id']]), request.records)
