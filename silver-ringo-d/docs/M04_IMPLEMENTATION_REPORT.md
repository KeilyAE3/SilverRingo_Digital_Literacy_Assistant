# M04 구현 보고 — ActionRegistry 허용목록

## 실행 정책

M04는 지식 문서의 관련 액션을 실행 권한으로 승격하지 않고, `action_catalog.json`의
등록·활성·연동 상태와 고정된 내부 route 형식만 검증하는 범위로 구현했다.
외부 URL, 패키지명, Intent, 레벨 직접 실행은 허용하지 않는다.
실제 A/B/C 화면 이동은 수행하지 않았으며, 액션 카탈로그의 현재 상태를 그대로 사용했다.

## 변경 파일

- `src/silver_ringo_d/action_registry.py`
  - JSON 또는 레코드 목록 로딩
  - schema, 중복 ID, 필수 필드 검증
  - `enabled=true`, `status != unintegrated`, `kind=navigate`,
    내부 route 형식을 모두 만족하는 액션만 노출
  - `get`, `validate`, `suggest` 구현
  - `related_action_ids`에 명시된 액션만 후보로 사용
  - 중복 제안 제거
- `tests/test_action_registry.py`
  - 기본 카탈로그의 비활성 액션 검증
  - 활성 내부 route 액션 검증
  - 미등록 ID·비활성 액션·파라미터 거부
  - 외부 URL 거부
  - 중복 ID와 잘못된 schema 거부

## 사용한 테스트 입력과 결과

### 기본 카탈로그 검증

현재 `knowledge/action_catalog.json`에는 다음 액션이 등록되어 있으나 모두
`enabled=false`, `status=unintegrated`이다.

- `open_learning_home`
- `open_guide_home`
- `open_kiosk_course`
- `start_ddocdoc_companion`

테스트 입력:

```text
related_action_ids = ["open_kiosk_course"]
```

결과:

```json
{
  "catalog_enabled": false,
  "suggested_actions": []
}
```

따라서 현재 A/B/C 통합 계약이 완료되지 않은 기능은 사용자에게 실행 버튼으로 반환되지 않는다.

### 활성화·파라미터·외부 대상 경계 테스트

검증한 입력:

- 활성 내부 route:
  - `id=open_kiosk_course`
  - `kind=navigate`
  - `target=learning.kiosk`
  - `enabled=true`
  - `status=integrated`
- 미등록 ID:
  - `missing`
- 잘못된 파라미터:
  - `{"level": 5}`
- 외부 대상:
  - `https://example.com`
- 중복 ID가 포함된 카탈로그
- `schema_version=2`인 잘못된 카탈로그

결과:

- 활성 내부 route만 `validate=True` 및 제안 가능
- 미등록 ID, 파라미터, 외부 URL은 모두 거부
- 중복 ID와 잘못된 schema는 `ActionRegistryError`로 명시적 실패

## 테스트 실행 결과

실행:

```text
unittest discover -s tests -p test_action_registry.py
```

결과:

```text
Ran 5 tests in 0.012s
OK
```

테스트 중 생성한 임시 잘못된 카탈로그 파일은 테스트 종료 시 삭제되었다.

## 수락 기준

- 비활성·미등록·미통합 액션 실행 차단: 통과
- 문서의 `related_action_ids`만 사용: 통과
- 내부 route 외 대상 차단: 통과
- 잘못된 파라미터 차단: 통과
- 현재 카탈로그에 대한 빈 제안 결과: 통과
- 실제 A/B/C 화면 이동 및 연동 테스트: M04 범위 밖이며 미수행

## 한계와 다음 모듈 계약

- 현재 카탈로그에는 활성 액션이 없으므로 실제 UI 이동 제안은 반환되지 않는다.
- M04는 LLM 자유 문장에서 액션 ID를 추출하지 않는다.
- M05는 `KnowledgeHit`를 `ActionRegistry.suggest()`에 전달하고, 그 결과만
  `ChatResponse.suggested_actions`로 변환해야 한다.
- M05/M06에서 실제 API 응답을 구성하더라도 D가 화면 전환이나 외부 앱 실행을 수행하지 않는다.
