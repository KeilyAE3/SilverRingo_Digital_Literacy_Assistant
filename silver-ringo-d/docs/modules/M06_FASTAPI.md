# M06 — HTTP API 및 Android 연동 경계

## 목표
M05 파이프라인을 단일 HTTP API로 노출한다. 프론트엔드 UI는 구현하지 않으며, A가 호출할 수 있는 명세와 로컬 실행법을 제공한다.

## API
- `POST /v1/chat` 요청 `message`,`history` → 응답 `reply`,`sources`,`suggested_actions`,`request_id`.
- `GET /health` → `{status:"ok", knowledge_loaded:true}` 같은 비민감 상태.
- 선택: `GET /v1/catalog` (활성 액션만). 첫 구현에 반드시 필요하지는 않음.
- 응답 스키마/상태코드: `docs/02_API_INTEGRATION.md` 준수.

## 개발 제약
- Python FastAPI/Pydantic+uvicorn. 서버 어댑터만 외부 라이브러리에 의존.
- dev 기본 localhost 바인딩; 공개 0.0.0.0로 배포하는 경우 인증/HTTPS/rate limit 없이 사용하지 않음.
- fake 모드와 실 Gemini 모드 명시 선택; 실 API 키 없다고 가짜 성공을 주장하지 않음.
- `history` 6턴/문자수 제한, 메시지 길이 1000자, request size 제한; 상세 traceback/키를 응답에 포함하지 않음.
- Android 에뮬레이터에는 `10.0.2.2`가 PC 호스트 주소로 보일 수 있음. 실제 연결은 A와 테스트.
- 민감정보 수집/전송 정책을 확인; API 키는 모바일 앱이 아니라 D 서버 환경변수.

## 파일
`src/silver_ringo_d/api.py`, `src/silver_ringo_d/schemas.py`, 선택 `__main__.py` 또는 `run.py`, `.env.example`, `requirements.txt` / `pyproject.toml`, README 실행 명령.

## 수락
- GET /health 및 POST /v1/chat fake 모드 로컬 성공.
- 비어 있는 메시지 400/422, 장문 제한, 모델 오류 503/429에 안정적인 JSON.
- API 응답에는 현재 미연동 상태인 액션을 포함하지 않음.
- A에 URL/메서드/요청·응답 JSON 두 예시와 테스트 방법 전달.
