# D파트 HTTP·A/B/C 연동 계약 v0.1

## 1. 외부 API (초안)
`POST /v1/chat` JSON 요청:
```json
{"message":"키오스크에서 카드 결제를 연습하고 싶어요","history":[]}
```
`200` JSON 응답:
```json
{
 "reply":"키오스크 학습에서 카드 결제를 모의로 연습할 수 있어요. 이전 단계부터 차례로 진행합니다.",
 "sources":[{"id":"course:kiosk:level:5","category":"learning"}],
 "suggested_actions":[],
 "request_id":"local-..."
}
```
예시 `suggested_actions`가 빈 이유는 현 시점 A와 연동된 기능 ID가 아직 없기 때문이다. 미래에 연결 완료되면 `[{"id":"open_kiosk_course","type":"navigate","target":"learning.kiosk","label":"키오스크 배우기 열기"}]`처럼 반환 가능.

- `history`: 최근 최대 6개 메시지(`role`=`user`/`assistant`, `content` 길이 제한). 서버 영구 대화 저장 없음.
- `message`: 공백만 있는 요청 거절, 최대 1000자(조정 가능).
- `sources`: 검색된 **실제 근거** ID만 반환. 계획 자료면 상태 표시.
- `suggested_actions`: 서버에서 활성화·검증된 액션만 포함. 사용자 확인 후 A가 실행, D는 직접 화면 전환 불가.
- 실패: `400`(요청 오류), `429`(호출 제한), `503`(LLM 일시 불능), `500`(예상치 못한 오류). 사용자에게는 고령층 친화적 문구를 반환하고 내부 오류/키/개인정보는 숨긴다.
- `GET /health`: API 작동/지식 로딩 상태; 민감 환경변수·키·개인정보 노출 금지.
- `GET /v1/catalog` 선택: 현재 공개해도 되는 활성 기능의 읽기전용 목록. 없더라도 MVP 가능.

## 2. Android 연결 (A)
- Retrofit/Ktor/OkHttp 중 A가 선호하는 HTTP 클라이언트로 호출. D는 UI를 만들지 않는다.
- 연결점: `HomeScreen`의 전송 버튼에서 POST /v1/chat 호출, 로딩/오류/텍스트 표시, 활성 액션 클릭 콜백 A가 처리.
- 개발용 Android 에뮬레이터에서 PC localhost 연결은 보통 `http://10.0.2.2:<port>`; 실기기는 PC IP/HTTPS 배포 필요. Android `INTERNET` 권한 및 디버그 cleartext 설정 필요 시 A와 협의.
- A `hjh`: 현재 `onLearnClick`/`onGuideClick` 콜백만 확인; 실제 학습 상세 내비게이션 ID는 미확정.

## 3. C 연동
- 현재 `LearningEntry(onExit:()->Unit, onResult:(String)->Unit={})` 존재. 코스 `topicId=kiosk`, 레벨 `1~6`.
- 실제 레벨 잠금 판정은 C 소유. D는 직접 레벨을 강제로 열지 않고 코스 진입만 추천.
- C 코스 갱신 시 `content.json`/`content.js`의 생성 관계를 확인하고 D 리소스를 갱신.

## 4. B 연동
- 서비스 키 `ddocdoc`, Android package `com.bbros.sayup`; 웹 대체 URL `https://www.ddocdoc.com/`.
- B에서 오버레이 실행 액션 ID/권한/상태 조회 계약이 별도로 제공될 때까지 D는 실제 오버레이 직접 실행을 제안하지 않음.
- B Native 7단계와 Web `ddocdoc-step-1..4`는 서로 별개 식별 체계.

## 5. 비기능 요구사항
- API 키는 서버 환경변수만 사용하고 `.env`는 gitignore.
- API 서버가 외부에 공개될 경우 인증, rate limit, HTTPS 필수. 로컬 시연은 루프백 바인딩 기본.
- Gemini rate limit, timeout, SDK 오류에 대비; bounded retry/exponential backoff, 중복 재시도 최대 횟수 제한.
- 질문·응답 로깅은 기본 비활성 또는 익명/축약 정보만; 개인/금융/의료정보를 외부 LLM에 보내지 않도록 필터·정책 마련.
