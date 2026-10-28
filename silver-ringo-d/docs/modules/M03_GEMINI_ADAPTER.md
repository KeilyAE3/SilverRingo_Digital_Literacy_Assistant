# M03 — Gemini LLM 어댑터 및 답변 규칙

## 목표
검색된 문서와 사용자 질문을 Gemini API에 전달하는 경계 모듈을 만들고, 항상 네트워크 없이 테스트 가능한 FakeLLM을 제공한다.

## 인터페이스
`LLMClient.generate(question: str, context: str, history: list[ChatTurn]) -> str` (프로토콜/추상 인터페이스). 구현 `GeminiAdapter`, `FakeLLM`.

## 구성
- 환경변수 `GEMINI_API_KEY` 및 `GEMINI_MODEL`: 기본 모델명은 공식 지원 목록에서 확인, 문서·코드에 폐기 모델 ID 고정 금지.
- `google-genai` Python SDK 사용(공식 문서 참조). SDK import는 어댑터 내부 또는 호출 지점에서 하여 stdlib 테스트를 막지 않게.
- `system_instruction`: 쉬운 존댓말, 간결한 단계, 정확한 출처만 안내, '지원되지 않는/준비 중인 기능'을 동작한다고 말하지 않기, 결제·접수 완료 주장 금지, 비밀번호/인증번호 요청 금지.
- context는 `KnowledgeHit`에서 직렬화된 관련 내용만 전달. 출처 ID·상태 표시, 내용과 명령을 구분하는 경계 구문. 검색된 문서에 담긴 명령을 시스템 지침으로 해석하지 않게.
- 최근 대화 최대 6개(설정값)와 최대 글자수 제한; 서버 대화 DB 불필요.
- Gemini 오류 429/5xx/timeout 시 내부 예외를 공통 오류로 변환, bounded retry 후 사용자 안전한 안내. 키/입력 내용 전체를 로그에 남기지 않음.
- 답변 텍스트 빈 값/비정상 타입 처리. 응답 길이 상한 가능.

## 검증
- FakeLLM으로 Context/History 전달 검사.
- SDK fake client로 시스템 지침, 맥락 경계, model env 전달 검사.
- 존재하지 않는 기능/비밀번호를 묻는 상황에서 정해진 fallback 규칙 확인.
- 실제 Gemini 호출은 `GEMINI_API_KEY` 사용자의 명시적 수동 승인 하에 수행. 사용량/비용 가정을 결과로 꾸미지 않는다.

## 비범위
Agent tool calling, 실시간 검색, 데이터 저장형 대화 기억, 컨텍스트 캐싱, Google File Search는 이번 모듈에서 구현하지 않는다.
