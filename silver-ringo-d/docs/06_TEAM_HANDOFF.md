# A/B/C 협업을 위한 최소 계약 확인 요청

## A에게 (채팅/UI)
- 채팅 UI가 `POST /v1/chat`의 `reply`, `sources`, `suggested_actions`를 표시할지 확인.
- `learning.home`와 `guides.home` 등 route ID는 D 임시 제안값. 실제 Compose nav route/콜백 이름 확인 필요.
- C `LearningEntry(...)`를 A의 `onLearnClick`에 연결할 계획 및 브랜치/통합 시점 확인.
- AI 오류/네트워크 불가 시 UX: 진행 표시, 다시 시도, 말풍선 등 A가 맡는 범위 확인.

## B에게 (오버레이)
- 현재 똑닥 단일 기능 외 지원 서비스 유무를 확인. 과거 정부24·카카오T 자료는 현재 미지원으로 취급.
- D 챗봇이 똑닥 도우미 시작을 제안할 때 호출 가능한 공개 진입점, Android 권한 검증 방식, 미설치 시 웹 대체 이동의 책임을 요청.
- Native 7단계/Web 4단계의 안내 데이터 중 어떤 것을 사용자에게 설명하는지 계약 정리.

## C에게 (학습 콘텐츠)
- 현재 `content.json`의 `schemaVersion`, `topicId`, `levels`, `guides`를 정식 출처로 삼아도 되는지 확인.
- 콘텐츠 갱신 시 어떤 파일이 원본이고 자동 생성인지 확인(`content.json` vs `content.js`).
- 코스 진입은 `LearningEntry(onExit,onResult)`로 충분한지, 코스별 `topicId` 확장 시 계약 확인.
- 레벨 잠금/진도 데이터의 소유권은 C. D는 시작 단계 직접 호출하지 않음.

## D가 보장할 것
- 사용자 질문을 받아 한국어 답변 + 출처 + 검증된 액션 제안 JSON 반환.
- 액션은 A/B/C의 확인된 기능에 한하여 `enabled=true`.
- 데이터 출처/현재 상태를 표시하고, 과거 아이디어로 현재 기능을 꾸미지 않음.
- Gemini 키는 D 서버에서 관리하며 모바일 앱에는 노출하지 않음.
