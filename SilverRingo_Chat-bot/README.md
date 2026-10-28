# SilverRingo D

D파트는 로컬 JSON 지식, 결정적 키워드/유의어 검색, LLM 경계, 액션 허용목록,
ChatService, FastAPI HTTP 경계를 담당한다. 현재는 프로토타입이며 실제 A/B/C 앱
통합 완료를 의미하지 않는다.

## 로컬 실행

Fake 모드 데모:

```text
python SilverRingo_Chat-bot/scripts/demo_queries.py
```

HTTP API:

```text
pip install -e "./SilverRingo_Chat-bot[api]"
uvicorn silver_ringo_d.api:app --app-dir SilverRingo_Chat-bot/src --host 127.0.0.1 --port 8000
```

실제 Gemini는 모바일 앱이 아니라 D 서버의 환경변수로 설정한다. `.env`는 Git에서
제외되며 API 키를 코드·로그·APK에 넣지 않는다.

## 테스트

Tester의 등록 목록이 유일한 공통 검사 진입점이다.

```text
python Tester/automation/check.py --mode local
python Tester/automation/check.py --mode workflow
```

main workflow는 대표 최종 QA 6개(D-QA-01~06)만 실행한다. 상세 회귀 검사는
local 모드에서 실행한다.

M07 예시 지식은 [Tester/example/knowledge](../Tester/example/knowledge)에 있다.
이는 `example_only=true`인 QA용 더미 자료이며 `SilverRingo_Chat-bot/knowledge`의
런타임 자료와 분리되어 있다. 실행 서비스는 이 예시를 읽지 않는다.

## 인터페이스

- 내부: `ChatService.handle(ChatRequest) -> ChatResponse`
- 외부: `POST /v1/chat`, `GET /health`
- `suggested_actions`는 활성화·검증된 액션만 반환한다. 현재 A/B/C 연동 전이라
  카탈로그 액션은 모두 비활성이다.
- UI 바로가기/플로팅 신호는 M08에서 별도 출력 계약으로 검토한다. 현재 D는
  화면 전환이나 외부 앱 조작을 수행하지 않는다.

## 데이터 한계

현재 확인된 표본은 A 홈 메뉴, C 키오스크 1개 코스/6레벨, B 똑닥 Native/Web
안내다. 자유로운 사용자 발화 전반의 검색 성능을 보장하지 않으며, 정부24·카카오T
등 폐기 자료는 현재 기능으로 안내하지 않는다.
