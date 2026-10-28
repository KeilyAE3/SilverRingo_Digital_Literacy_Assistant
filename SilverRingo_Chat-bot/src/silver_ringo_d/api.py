"""FastAPI adapter exposing the M05 ChatService contract."""
from dataclasses import asdict
from pathlib import Path

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from .action_registry import ActionRegistry
from .chat_service import ChatService
from .config import Settings, load_settings
from .gemini_adapter import create_llm
from .knowledge_store import KnowledgeStore, KnowledgeError
from .llm import LLMError
from .retriever import LexicalRetriever
from .schemas import ActionOutput, ChatInput, ChatOutput, ErrorOutput, HealthOutput, SourceOutput

MAX_REQUEST_BYTES = 64 * 1024


def build_service(settings: Settings | None = None) -> tuple[ChatService, bool]:
    settings = load_settings() if settings is None else settings
    store = KnowledgeStore(settings.knowledge_dir)
    store.load()
    return (ChatService(LexicalRetriever(store), create_llm(settings),
                        ActionRegistry(settings.knowledge_dir / 'action_catalog.json'), settings), True)


def create_app(service: ChatService | None = None, *, knowledge_loaded: bool | None = None) -> FastAPI:
    if service is None:
        service, loaded = build_service()
        if knowledge_loaded is None:
            knowledge_loaded = loaded
    elif knowledge_loaded is None:
        knowledge_loaded = True

    app = FastAPI(title='SilverRingo D Chat API', version='0.1.0')

    @app.middleware('http')
    async def request_size_limit(request: Request, call_next):
        content_length = request.headers.get('content-length')
        if content_length is not None:
            try:
                too_large = int(content_length) > MAX_REQUEST_BYTES
            except ValueError:
                too_large = True
            if too_large:
                return JSONResponse(status_code=413, content={
                    'error': 'request_too_large',
                    'message': '요청이 너무 커요. 질문과 대화를 줄여 주세요.',
                })
        return await call_next(request)

    @app.exception_handler(RequestValidationError)
    async def validation_error(_request, _exc):
        return JSONResponse(status_code=422, content={
            'error': 'invalid_request',
            'message': '질문 형식이나 글자 수를 확인해 주세요.',
        })

    @app.exception_handler(LLMError)
    async def llm_error(_request, exc: LLMError):
        code = 'rate_limited' if exc.code == 'rate_limit' else 'llm_unavailable'
        return JSONResponse(status_code=exc.http_status, content={
            'error': code,
            'message': str(exc),
        })

    @app.exception_handler(KnowledgeError)
    async def knowledge_error(_request, _exc):
        return JSONResponse(status_code=503, content={
            'error': 'knowledge_unavailable',
            'message': '지식 자료를 불러오지 못했어요. 잠시 뒤 다시 시도해 주세요.',
        })

    @app.get('/health', response_model=HealthOutput)
    async def health():
        return HealthOutput(status='ok' if knowledge_loaded else 'degraded',
                            knowledge_loaded=bool(knowledge_loaded))

    @app.post('/v1/chat', response_model=ChatOutput)
    async def chat(payload: ChatInput):
        response = service.handle(payload.to_core())
        return ChatOutput(
            reply=response.reply,
            sources=[SourceOutput(**asdict(source)) for source in response.sources],
            suggested_actions=[ActionOutput(**asdict(action))
                                for action in response.suggested_actions],
            request_id=response.request_id,
        )

    return app


app = create_app()
