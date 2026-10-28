# M03 구현 보고 — 역할 지침·Gemini 경계·Fake JSON

## 범위와 현재 상태

M03만 구현했다. 실제 Gemini 호출/키 열람/SDK 설치는 하지 않았다.
Python 3.12.10에서 FakeLLM과 SDK mock으로 검증했다. Tester 및 test_*.py는 추가하지 않았다.
Git 시작 상태는 clean, 브랜치는 ydy_workspace. fetch만 수행했으며 A/B/C 변경,
checkout, commit, push는 하지 않았다.

## 역할 지침은 어디에 있는가

system_prompt.py의 SYSTEM_INSTRUCTION이 앱의 고정 역할 정의다.
사용자 질문·검색 JSON·이력에 문자열로 덧붙이지 않고, 매 generate_content 호출의
config.system_instruction에 별도로 넣는다. 앱 소스에서 한 곳만 관리하며 모든 요청에 동일하게 적용한다.

generateContent는 이 지침을 영구 기억하는 챗봇 등록 API가 아니다. 네트워크 요청에서
systemInstruction을 완전히 생략하면서 동일 지침을 계속 적용하려면 별도 상태 저장/캐싱
설계가 필요하다. 컨텍스트 캐싱과 데이터 저장형 기억은 M03 비범위다.
따라서 사용자 질의에서 분리하되 전송 시에는 시스템 필드에 포함하는 구조다.

지침: SilverRingo 역할, 쉬운 존댓말, 근거 우선, prototype/placeholder/idea 구분,
결제·접수 완료 및 대리 실행 금지, 인증정보 요청 금지, 문서 내부 명령 무시.

## 추가·수정 파일

| 파일 | 역할 |
|---|---|
| src/silver_ringo_d/system_prompt.py | 고정 시스템 지침·JSON 응답 스키마 |
| src/silver_ringo_d/llm.py | LLMClient, FakeLLM, 선택 근거 직렬화, 요청/응답 검증, 공통 오류 |
| src/silver_ringo_d/gemini_adapter.py | lazy SDK 호출, 타임아웃·재시도, 팩토리 |
| src/silver_ringo_d/local_settings.py | 명시적으로 지정한 로컬 설정 파일 로딩 |
| scripts/probe_llm.py | fake 기본값의 수동 weak/rag 확인 도구 |
| pyproject.toml | 선택 의존성 gemini = google-genai |
| docs/07_SPRINT_TRACKER.md | 구현·검증 상태와 구조 갱신 |
| docs/M03_IMPLEMENTATION_REPORT.md | 본 보고 및 키 연결 안내 |

## 요청·응답 계약

명세의 generate(question, context, history) -> str 형태는 유지하되,
사용자 요청을 반영하여 반환 str은 일반 답변 문자열이 아니라 JSON 문자열로 구체화했다.
M05는 json.loads(raw) 후 reply/source_ids를 최종 ChatResponse로 변환해야 한다.

```json
{"reply":"모의 카드 결제 연습 자료가 있어요. 통합 상태는 확인이 필요해요.","source_ids":["course:kiosk:level:5"]}
```

이는 모델이 생성한 JSON 본문이다. Google의 전체 HTTP 응답 봉투(candidates/usageMetadata 등)를
원문으로 노출하는 것은 아니다. SDK 응답의 text를 검사한 뒤 그 JSON 문자열을 반환한다.
정상 JSON이어도 사실성까지 증명하는 것은 아니다. 모델 액션/URL 실행 필드는 허용하지 않는다.

요청은 다음처럼 분리된다.

```text
config.system_instruction = 고정 역할 지침
config.response_mime_type = application/json
config.response_json_schema = reply/source_ids 스키마
contents = 최근 이력(user/model 역할) + 이번 user 데이터
이번 user 데이터 = { question, retrieved_evidence_untrusted: [...] }
```

- serialize_context(hits): M02가 선택한 문서의 id/category/title/content/status/source만 전달.
  액션 ID·전체 저장소·점수는 보내지 않는다. 최대 10개·문맥 16000자.
- JSON 인코딩으로 문서에 들어 있는 경계 문자열을 데이터로 취급한다.
- history는 HistoryMessage 목록, 최근 6개만 사용. 각 1000자, 질문 1000자.
- 응답은 raw 16000자·reply 2000자 제한. 중복 JSON 필드, 추가 필드, 잘못된 타입,
  빈 답변, 중복/근거 밖 source_ids를 거절한다.
- 빈 근거 또는 탐지된 민감정보/비밀번호 요청은 외부 호출 없이 고정 JSON 안내를 반환한다.
- 전화번호·이메일·주민번호 형식·일부 인증정보와 설정된 API 키의 정확 일치를 탐지한다.
  이는 완전한 개인정보 탐지기가 아니다. 이름/자유 형식 주소/의료 서술 등의 포괄 처리는
  후속 서비스 정책에서 보완해야 한다. 프롬프트 주입 차단 역시 완벽성을 보장하지 않는다.
- 질문/이력/원문 응답을 파일·DB·로그에 누적하지 않는다. probe는 수동 시연 결과를 stdout으로 표시한다.

## SDK와 오류 처리

- Gemini Developer API의 generateContent 사용. OpenAI 호환 API나 Vertex 서비스 계정 경로가 아니다.
- 실제 호출 직전에 google-genai를 import. fake/core에는 SDK가 필요 없다.
- 모델 ID는 GEMINI_MODEL로 필수 지정하며 코드에 기본 모델을 고정하지 않는다.
- 기본 HTTP timeout 15000ms, SDK retry attempts=1로 중복 재시도를 억제.
- 어댑터는 최초 시도 포함 최대 3회. 429/5xx/timeout만 0.25초, 0.5초 간격으로 재시도.
  timeout은 전송 계층 옵션이며 전체 요청의 엄격한 벽시계 deadline은 아니다.
- 400/401/403/404는 설정 오류로 취급하고 재시도하지 않는다.
- LLMError: rate_limit(429), unavailable/configuration/invalid_response(503).
  이는 후속 HTTP 계층에 전달할 정보다. 실제 HTTP 서버는 아직 없다.
- 공급자 원문 예외를 노출하지 않고 안전한 한국어 메시지로 변환한다.
- 현재 SDK가 설치되어 있지 않다. 선택 의존성은 아직 버전 고정하지 않았으며
  실제 설치 버전·SDK 형식·키 권한·모델 가용성은 다음 실호출 때 확인해야 한다.

## 검증 결과

Python 3.12.10의 -S 환경에서 임시 mock 검사 7묶음 통과:

1. 실제 로컬 Retriever → 선택 근거 → FakeLLM JSON 반환.
2. 시스템/사용자 데이터 분리, 최근 6개 이력·model 역할, 모델 설정 전달, 연속 호출의 시스템 지침 유지.
3. 빈 근거·민감정보 입력 시 API 호출 0회.
4. 비정상 JSON·빈 응답·긴 응답·알 수 없는 출처·키 재출력 거절.
5. 429/503/timeout 제한 재시도, 재시도 성공, 401 즉시 중단, 안전한 예외.
6. 로컬 설정 파일 읽기와 환경변수 우선순위·오류 메시지 비밀값 비노출.
7. mock SDK 생성 시 timeout/retry 설정 및 close 전달.

수동 도구도 fake로만 실행했다:

```powershell
py -3.12 -S silver-ringo-d/scripts/probe_llm.py --probe weak
py -3.12 -S silver-ringo-d/scripts/probe_llm.py --probe rag
```

weak는 app:overview, rag는 course:kiosk:level:5를 근거로 모의 JSON을 반환했다.
FakeLLM은 시험용 고정 안내이며 실제 자연어 추론 품질을 흉내 내지 않는다.

## 사용자가 준비할 키 저장 위치

권장 로컬 파일: 저장소의 silver-ringo-d/.env. 현재 .gitignore가 해당 파일을 제외한다.
파일은 평문이며 암호화 저장소가 아니다. 앱 배포 시에는 D 서버의 환경변수/비밀 저장소로 이전한다.
APK, A/B/C 소스, .env.example, 채팅 메시지에 실제 키를 넣지 않는다.
이번 작업은 실제 .env를 생성하거나 기존 키 값을 읽지 않았다.

```dotenv
SR_D_LLM_MODE=gemini
GEMINI_API_KEY=여기에_사용자가_로컬에서_직접_입력
GEMINI_MODEL=사용_권한이_있는_generateContent_JSON_지원_모델_ID
SR_D_TOP_K=4
```

GEMINI_API_KEY는 Google AI Studio에서 발급한 Gemini Developer API용 키다.
2026-10-28 확인한 공식 목록에는 gemini-3.8-flash 등이 있으나, 실제 계정 권한과
선택 모델의 generateContent·구조화 출력 지원 여부는 다음 호출에서 확인한다.
코드/기본 설정에는 모델 이름을 고정하지 않았다.

.env는 자동으로 읽지 않는다. probe에서 --env-file을 명시하거나
load_local_settings(path)를 호출한다. 이미 설정된 환경변수가 파일 값보다 우선한다.
허용된 KEY=VALUE만 읽고 셸 실행·변수 치환은 하지 않는다. export/인라인 주석은 지원하지 않는다.

## 다음 M04 중 예정한 실호출 검증

아래는 다음 작업용 명령이며 이번에는 실행하지 않았다. 저장소 루트에서 실행한다.

```powershell
py -3.12 -m venv silver-ringo-d/.venv
.\silver-ringo-d\.venv\Scripts\python.exe -m pip install -e "./silver-ringo-d[gemini]"

# 약한 검증: retriever 없이 앱 소개 근거 하나와 고정 질문으로 서버 수락·JSON 확인
.\silver-ringo-d\.venv\Scripts\python.exe silver-ringo-d/scripts/probe_llm.py --probe weak --live --env-file silver-ringo-d/.env

# 강한 검증: 고정 비개인정보 질의 → M01 → M02 → M03 → 실제 Gemini JSON
.\silver-ringo-d\.venv\Scripts\python.exe silver-ringo-d/scripts/probe_llm.py --probe rag --live --env-file silver-ringo-d/.env
```

--live가 없으면 환경변수/키 파일을 읽지 않고 fake로 실행한다.
약한 검증에도 출처 있는 최소 문맥을 넣는다. 빈 문맥은 안전 fallback으로 처리되어
실제 API 호출 검증이 되지 않기 때문이다.
두 경로 모두 같은 GeminiAdapter를 사용한다. 강한 검증은 현재 구현된 데이터 흐름의
실호출 확인이며 아직 없는 M05 ChatService·M06 HTTP·Android UI까지의 통합 검증은 아니다.
성공 판정은 실제 공급자 응답 수신, JSON 계약, 반환 출처의 선택 근거 포함 여부다.
추후 키가 준비되면 위 범위로 진행하며 사용자에게 키 내용을 채팅으로 요청하지 않는다.

## 공식 규격 근거

- [generateContent / systemInstruction](https://ai.google.dev/api/generate-content)
- [google-genai SDK](https://googleapis.github.io/python-genai/)
- [구조화 출력](https://ai.google.dev/gemini-api/docs/structured-output)
- [API 키](https://ai.google.dev/gemini-api/docs/api-key)
- [모델 목록](https://ai.google.dev/gemini-api/docs/models)
