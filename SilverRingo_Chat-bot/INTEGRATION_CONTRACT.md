# D Chat/RAG 통합 계약

## 로컬 확장판 규칙

엔드포인트와 JSON 필드는 그대로다. 자료 없는 일반 디지털 사용법·가벼운 잡담은
Gemini 일반 답변으로 처리하며 sources/suggested_actions는 빈 목록이다. 앱 기능
존재는 근거 없이 확정하지 않는다. 기존 클라이언트도 응답을 읽을 수 있다.

현재 활성 액션은 open_kiosk_course → learning.kiosk와 open_guide_home → guides.home이다.
둘 다 기존 화면만 열며 학습 잠금·권한·외부 앱 선택은 해당 화면이 담당한다.
관련 검색 자료, 실제 답변의 출처, 점수·상태·활성 목록을 통과한 후보만 최대 2개 반환한다.
Android는 ID/type/target와 관련 출처를 다시 검증하고 로컬 문구로 버튼을 표시한다.
대화별 reply/sources/actions를 함께 저장하며 기능에서 나가면 기존 채팅으로 복귀한다.

open_learning_home은 중복 진입이므로 비활성, start_ddocdoc_companion은 서비스 자동 시작
계약이 없으므로 비활성이다. DHub 반영 전까지 운영 응답은 기존 정책일 수 있다.
아래 초기 문서의 '모든 액션 비활성' 설명보다 이 절을 우선한다.

이 파일은 D 컴포넌트를 다른 모듈과 연결할 때 사용하는 최소 계약이다.
D는 Android 화면이나 외부 앱을 직접 조작하지 않고, 질문을 받아 답변과
검증된 참고자료·액션 제안을 반환한다.

## 런타임 입력의 의미

여기서 `입력`은 Python 실행창의 `input()`이 아니다. 실행 중인 호출자가
전달하는 하나의 요청 메시지다.

호출 흐름은 다음과 같다.

```text
사용자 입력 이벤트
  -> A/UI 또는 다른 호출자
  -> HTTP JSON 요청
  -> D API가 ChatInput으로 검증
  -> ChatInput.to_core()
  -> ChatRequest
  -> ChatService.handle()
  -> ChatResponse
  -> HTTP JSON 응답
  -> 호출자가 화면·버튼·안내 문구로 표시
```

현재 D가 직접 받는 공개 런타임 진입점은 `POST /v1/chat`이다. 따라서 A의
Compose 채팅 이벤트, 테스트 클라이언트, 또는 다른 서버는 같은 HTTP 요청
형식으로 연결한다. D는 Android 이벤트 객체나 Compose 상태를 직접 알지
못한다.

## HTTP 요청

```http
POST /v1/chat
Content-Type: application/json
```

```json
{
  "message": "키오스크에서 카드 결제하는 법을 배우고 싶어",
  "history": [
    {"role": "user", "content": "키오스크가 어려워"},
    {"role": "assistant", "content": "어떤 기능이 궁금하세요?"}
  ]
}
```

제약:

- `message`: 공백이 아닌 문자열, 최대 1000자
- `history`: 생략 가능, 최대 6개
- history의 `role`: `user` 또는 `assistant`
- 각 history `content`: 1~1000자
- 정의되지 않은 JSON 필드는 거부된다

`history`는 최근 대화 문맥일 뿐이며, 사용자 권한·진도·외부 앱 상태를
표현하지 않는다. 그런 상태는 해당 기능을 소유한 A/B/C 모듈이 관리한다.

## HTTP 응답

정상 응답은 `200`이며 형태는 다음과 같다.

```json
{
  "reply": "카드 결제 방법을 단계별로 안내해 드릴게요.",
  "sources": [
    {
      "id": "course:kiosk:level:5",
      "category": "learning",
      "status": "implemented"
    }
  ],
  "suggested_actions": [],
  "request_id": "local-..."
}
```

- `reply`: 사용자에게 표시할 한국어 답변
- `sources`: 답변에 사용된 D 지식 자료의 식별자와 상태
- `suggested_actions`: 검증되어 활성화된 액션만 포함. 현재 통합 전에는
  빈 배열일 수 있다
- `request_id`: 로그 상관관계용 식별자. 사용자 인증 ID가 아니다

`suggested_actions`가 비어 있으면 UI는 일반 답변만 표시한다. D가 화면
전환, 플로팅 표시, 외부 앱 실행을 지시하거나 수행한다고 가정해서는 안 된다.
액션을 활성화하려면 해당 A/B/C 소유자가 실제 진입점·파라미터·상태를
확정한 뒤 `action_catalog.json`의 계약을 갱신해야 한다.

## 오류 응답

오류도 JSON으로 반환된다.

```json
{
  "error": "llm_unavailable",
  "message": "잠시 후 다시 시도해 주세요."
}
```

주요 상태:

- `422 invalid_request`: 요청 형식·문자 수 오류
- `413 request_too_large`: 요청 본문이 64 KiB 초과
- `429 rate_limited`: Gemini 호출 제한
- `503 llm_unavailable` 또는 `knowledge_unavailable`: 서버·지식·LLM 사용 불가

UI는 오류의 `message`를 사용자에게 표시하고, 재시도 여부와 로딩 상태를
소유한다. API 키, SDK 오류, 내부 경로는 응답 계약에 포함되지 않는다.

## Python 내부 계약

HTTP 경계 아래에서 테스트·서버 어댑터는 다음 타입을 사용한다.

```python
ChatInput.to_core() -> ChatRequest
ChatService.handle(ChatRequest) -> ChatResponse
```

`ChatRequest`와 `ChatResponse`는 `input()`과 연결된 함수가 아니라, 런타임
호출 사이의 검증된 데이터 객체다. 다른 Python 모듈이 같은 프로세스에서
D를 직접 사용할 때만 이 내부 계약을 사용하고, 별도 프로세스·Android
클라이언트는 HTTP 계약을 사용한다.

## 책임 경계

- D: 검색, 근거 기반 답변, 오류 형식, 활성 액션 allow-list
- A: 채팅 UI, 로딩·재시도, `reply`와 액션 표시, Compose navigation
- B: 똑닥 등 실제 외부 기능의 실행·권한·fallback
- C: 코스 진입·진도·잠금 상태

현재 D 지식 JSON은 자동으로 A/B/C 원본을 추출하지 않는다. 원본 변경 시
담당자 확인 후 D 지식을 갱신하고, KnowledgeStore 무결성 검사와 Tester를
실행해야 한다.
