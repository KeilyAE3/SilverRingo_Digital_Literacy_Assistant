# M04 — ActionRegistry (안전한 앱 기능 연결 허용목록)

## 목표
LLM이 설명한 기능이 실제로 사용자에게 표시되고 실행 가능할지 검증한다. `action_catalog.json`은 지식 본문과 분리하고 **실행 권한의 단일 원천**으로 사용한다.

## 인터페이스
- `ActionRegistry.get(action_id) -> Action | None`
- `ActionRegistry.validate(action_id, params: dict|None) -> bool`
- `ActionRegistry.suggest(hits: list[KnowledgeHit]) -> list[ActionSuggestion]`

## 요구
1. `enabled=false`·ID 미등록·status=`unintegrated`는 어떤 경우에도 실행 액션으로 반환 금지.
2. `suggest`는 문서가 명시한 `related_action_ids`만 참조. LLM 자유문장에서 새로운 action ID 유추 금지.
3. `kind=navigate`로 시작. `target`은 고정된 앱 내 route ID만; 임의 URL·Intent·패키지명 실행 금지.
4. `enabled=true` 승격 조건: A와 target/콜백 합의 + 앱에서 처리 가능 확인 + 연동 테스트.
5. C의 레벨 직접 열기는 잠금 우회 위험이 있으므로 `open_kiosk_course` 정도만 후보, 직접 레벨 실행은 A/C 협의 후 별도 권한 체크.
6. B 똑닥 도우미 실행은 권한 및 앱 설치 상태 등 플랫폼 조건이 필요; 계약 완료 전 `enabled=false`.

## 파일/검증
`src/silver_ringo_d/action_registry.py`, `tests/test_action_registry.py`; JSON은 M01 버전 사용.
- 아무 액션도 활성화되지 않았으면 빈 목록이 정상.
- 잘못된 ID·disabled·미지 파라미터·허용되지 않은 target은 거부.
- 실제 화면 이동은 A UI에서 수행; D는 JSON만 반환.
