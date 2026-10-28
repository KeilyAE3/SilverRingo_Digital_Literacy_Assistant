"""Validated core contracts; no SDK or HTTP dependencies.

Use dataclasses.asdict for JSON-compatible payloads. Validation occurs at
construction; consumers must revalidate externally modified data at boundaries.
"""
from dataclasses import dataclass, field
import math
import re
from typing import Literal

Status = Literal['implemented', 'prototype', 'placeholder', 'idea', 'retired']
STATUSES = {'implemented', 'prototype', 'placeholder', 'idea', 'retired'}


def text(value, name):
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f'{name} must be a nonblank string')


def identifier(value):
    if not isinstance(value, str) or not re.fullmatch(r'[a-z][a-z0-9:_-]*', value):
        raise ValueError('invalid identifier')


def items(values, expected, name):
    if not isinstance(values, list) or any(not isinstance(v, expected) for v in values):
        raise ValueError(f'{name} has invalid list items')


@dataclass
class SourceRef:
    branch: str
    path: str
    kind: Literal['code', 'integration_doc', 'design_doc']

    def __post_init__(self):
        text(self.branch, 'branch')
        text(self.path, 'path')
        if self.kind not in ('code', 'integration_doc', 'design_doc'):
            raise ValueError('invalid source kind')


@dataclass
class KnowledgeDocument:
    id: str
    category: str
    title: str
    content: str
    status: Status
    source: SourceRef
    keywords: list[str] = field(default_factory=list)
    aliases: list[str] = field(default_factory=list)
    related_action_ids: list[str] = field(default_factory=list)

    def __post_init__(self):
        identifier(self.id)
        for name in ('category', 'title', 'content'):
            text(getattr(self, name), name)
        if not isinstance(self.status, str) or self.status not in STATUSES:
            raise ValueError('invalid knowledge status')
        if not isinstance(self.source, SourceRef):
            raise ValueError('source must be SourceRef')
        for name in ('keywords', 'aliases', 'related_action_ids'):
            items(getattr(self, name), str, name)
            for value in getattr(self, name):
                text(value, name)
        for value in self.related_action_ids:
            identifier(value)


@dataclass
class KnowledgeHit:
    document: KnowledgeDocument
    score: float
    matched_terms: list[str] = field(default_factory=list)

    def __post_init__(self):
        if not isinstance(self.document, KnowledgeDocument):
            raise ValueError('document must be KnowledgeDocument')
        if type(self.score) not in (int, float) or not math.isfinite(self.score) or self.score < 0:
            raise ValueError('score must be finite and nonnegative')
        items(self.matched_terms, str, 'matched_terms')


@dataclass
class ActionSuggestion:
    """Transport data only; does not authorize execution (M04)."""
    id: str
    type: str
    target: str
    label: str

    def __post_init__(self):
        identifier(self.id)
        for name in ('type', 'target', 'label'):
            text(getattr(self, name), name)


@dataclass
class HistoryMessage:
    role: Literal['user', 'assistant']
    content: str

    def __post_init__(self):
        if self.role not in ('user', 'assistant'):
            raise ValueError('invalid history role')
        text(self.content, 'history content')
        if len(self.content) > 1000:
            raise ValueError('history content exceeds 1000 characters')


@dataclass
class ChatRequest:
    message: str
    history: list[HistoryMessage] = field(default_factory=list)

    def __post_init__(self):
        text(self.message, 'message')
        if len(self.message) > 1000:
            raise ValueError('message exceeds 1000 characters')
        items(self.history, HistoryMessage, 'history')
        if len(self.history) > 6:
            raise ValueError('history exceeds 6 messages')


@dataclass
class ResponseSource:
    id: str
    category: str
    status: Status

    def __post_init__(self):
        identifier(self.id)
        text(self.category, 'category')
        if not isinstance(self.status, str) or self.status not in STATUSES:
            raise ValueError('invalid source status')


@dataclass
class ChatResponse:
    reply: str
    sources: list[ResponseSource]
    suggested_actions: list[ActionSuggestion]
    request_id: str

    def __post_init__(self):
        text(self.reply, 'reply')
        text(self.request_id, 'request_id')
        items(self.sources, ResponseSource, 'sources')
        items(self.suggested_actions, ActionSuggestion, 'suggested_actions')
