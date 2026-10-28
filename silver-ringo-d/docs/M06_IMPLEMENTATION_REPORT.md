# M06 구현 보고 — FastAPI HTTP 연동 경계

## HTTP API의 의미

M06의 “HTTP API로 노출한다”는 표현은 M05 파이프라인을 웹 서버의 외부 호출
경계로 제공한다는 뜻이다. 단순히 Python 모듈을 추가하는 것이 아니라, A Android
또는 다른 클라이언트가 동일한 JSON 입력을 보내고 동일한 JSON 출력 계약을 받을 수
있도록 만든다.

동시에 Python 내부에서는 `create_app(service)`가 M05 `ChatService`를 주입받는
명시적 진입점이다. 따라서 다른 D 모듈은 FastAPI를 직접 호출하지 않고
`ChatService.handle(ChatRequest)` 계약을 유지하며, 외부 팀은 HTTP 계약만 사용한다.

## 변경 파일

- `src/silver_ringo_d/schemas.py`
  - HTTP 입력/출력 Pydantic 스키마
  - 허용 role, 메시지 1000자, history 6개, 추가 필드 거부
- `src/silver_ringo_d/api.py`
  - `create_app(service)` 애플리케이션 팩토리
  - `build_service()`의 KnowledgeStore → Retriever → LLM → ActionRegistry 연결
  - `GET /health`
  - `POST /v1/chat`
  - 요청 크기 제한과 안전한 오류 응답
- `tests/test_api.py`
  - FastAPI TestClient 기반 HTTP 계약 테스트
- `pyproject.toml`
  - 서버 경계 선택 의존성 `api = ["fastapi", "uvicorn"]`

## 다른 모듈과의 연결 방식

```text
HTTP JSON
  -> schemas.ChatInput
  -> ChatRequest
  -> ChatService.handle()
  -> KnowledgeStore / LexicalRetriever / LLMClient / ActionRegistry
  -> ChatResponse
  -> schemas.ChatOutput
  -> HTTP JSON
```

- M01/M02/M03/M04/M05는 기존 Python 계약을 유지한다.
- M06은 이 모듈들을 HTTP 요청으로 묶는 어댑터일 뿐, Retriever나 LLM을 다시 구현하지 않는다.
- 테스트나 다른 애플리케이션은 `create_app(mock_or_fake_service)`로 서비스를 주입할 수 있다.
- 현재 카탈로그의 액션이 모두 비활성/미통합이므로 API의 `suggested_actions`도 빈 배열이다.
- D 서버가 액션을 실행하지 않으며, A가 활성 액션을 확인한 뒤 화면 이동을 담당한다.

## API 계약

### `POST /v1/chat`

요청:

```json
{
  "message": "키오스크에서 카드 결제를 연습하고 싶어요.",
  "history": []
}
```

성공 응답:

```json
{
  "reply": "[모의 응답] 관련 시험 구현 자료를 찾았어요. 앱 통합과 기기 검증 여부는 별도 확인이 필요해요.",
  "sources": [
    {
      "id": "course:kiosk:level:5",
      "category": "learning",
      "status": "prototype"
    }
  ],
  "suggested_actions": [],
  "request_id": "local-..."
}
```

### `GET /health`

응답:

```json
{
  "status": "ok",
  "knowledge_loaded": true
}
```

이 응답에는 API 키, 모델명, 파일 경로, 개인정보를 포함하지 않는다.

## 테스트 질의와 결과

### 실제 RAG 경로

질의:

```text
키오스크에서 카드 결제를 연습하고 싶어요.
```

처리:

```text
ChatInput
→ ChatRequest
→ LexicalRetriever
→ course:kiosk:level:5
→ FakeLLM
→ ChatOutput
```

HTTP 상태는 `200`이며, source와 response schema가 정상 반환되었다.

### 빈 입력

요청:

```json
{"message": "   "}
```

HTTP `422`, 응답:

```json
{
  "error": "invalid_request",
  "message": "질문 형식이나 글자 수를 확인해 주세요."
}
```

### LLM 호출 오류

Retriever가 근거를 찾은 대표 질의에 대해 Fake LLM을 429 오류로 주입했다.
HTTP `429`와 다음 안전 응답을 확인했다.

```json
{
  "error": "rate_limited",
  "message": "잠시 요청이 많아요. 조금 뒤에 다시 시도해 주세요."
}
```

공급자 traceback, API 키, 원문 예외는 응답에 포함하지 않는다.

## 테스트 실행

실행:

```text
unittest discover -s tests -p test_api.py
```

결과:

```text
Ran 4 tests
OK
```

로컬 실행 시에는 저장소 루트에서 다음처럼 루프백에 바인딩하는 것을 기본으로 한다.

```text
uvicorn silver_ringo_d.api:app --host 127.0.0.1 --port 8000
```

단, 현재 `api:app`은 자동으로 환경을 읽어 서버를 시작하지 않도록 `None`으로
두었다. 운영 실행은 별도의 런처에서 `create_app()`을 호출해 설정·지식 로딩 실패를
명시적으로 처리해야 한다. 다음 단계에서 필요하면 `run.py`를 추가할 수 있다.
Android 에뮬레이터에서는 PC 호스트가 보통 `http://10.0.2.2:8000`으로 보인다.

## 한계와 다음 작업

- FastAPI는 HTTP 경계 선택 의존성이며 핵심 Retriever/ChatService는 외부 SDK 없이 동작한다.
- 현재 테스트는 FakeLLM 기반이며 라이브 Gemini는 M03에서 별도로 승인된 수동 검증 대상이다.
- 실제 Android 연결, 인증, HTTPS, rate limit 정책은 배포 전 별도 합의가 필요하다.
- M07에서 공통 Tester 등록과 데모 시나리오를 추가한다.
