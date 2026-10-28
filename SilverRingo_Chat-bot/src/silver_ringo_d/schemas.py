"""HTTP boundary schemas. Core dataclasses remain independent of FastAPI."""
from pydantic import BaseModel, ConfigDict, Field, field_validator

from .models import ChatRequest, HistoryMessage


class HistoryItem(BaseModel):
    model_config = ConfigDict(extra='forbid')

    role: str
    content: str = Field(min_length=1, max_length=1000)

    @field_validator('role')
    @classmethod
    def valid_role(cls, value):
        if value not in ('user', 'assistant'):
            raise ValueError('role must be user or assistant')
        return value


class ChatInput(BaseModel):
    model_config = ConfigDict(extra='forbid')

    message: str = Field(min_length=1, max_length=1000)
    history: list[HistoryItem] = Field(default_factory=list, max_length=6)

    @field_validator('message')
    @classmethod
    def nonblank_message(cls, value):
        if not value.strip():
            raise ValueError('message must not be blank')
        return value

    def to_core(self) -> ChatRequest:
        return ChatRequest(self.message, [HistoryMessage(item.role, item.content)
                                          for item in self.history])


class SourceOutput(BaseModel):
    model_config = ConfigDict(extra='forbid')

    id: str
    category: str
    status: str


class ActionOutput(BaseModel):
    model_config = ConfigDict(extra='forbid')

    id: str
    type: str
    target: str
    label: str


class ChatOutput(BaseModel):
    model_config = ConfigDict(extra='forbid')

    reply: str
    sources: list[SourceOutput]
    suggested_actions: list[ActionOutput]
    request_id: str


class HealthOutput(BaseModel):
    status: str
    knowledge_loaded: bool


class ErrorOutput(BaseModel):
    error: str
    message: str
