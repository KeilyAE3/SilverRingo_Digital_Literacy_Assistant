# 제거한 M00 테스트 11개 — 사용자 선별용

사용자 요청에 따라 tests/test_models.py를 제거했다. 아래는 과거 작성한 테스트의
목적 목록이며 현재 실행 가능한 테스트 목록이 아니다. 이전 Python 3.11/3.13의
11/11 결과는 당시 코드에 대한 기록이며 Python 3.12 또는 현재 M01 검증 결과가 아니다.

| 번호 | 테스트 메서드 | 검사 내용 |
|---|---|---|
| 1 | test_json_serialization | 중첩 지식·응답 dataclass를 JSON으로 변환하고 출처/상태 필드 보존 |
| 2 | test_invalid_documents | 잘못된 ID·상태·출처·키워드/alias 배열·액션 참조 형식 거절 |
| 3 | test_source_kind | 허용되지 않은 출처 종류 거절 |
| 4 | test_scores | 검색 점수의 음수·NaN·무한대·문자열·bool 거절 |
| 5 | test_request_limits | 메시지 1000자, 이력 6개, 각 이력 1000자 경계와 role/빈 내용 검증 |
| 6 | test_independent_defaults | 서로 다른 객체의 기본 리스트가 공유되지 않음 |
| 7 | test_response_validation | 응답 sources 타입과 액션 ID 형식 검증 |
| 8 | test_fake_defaults | 키 없이 fake/top_k=4 기본값 및 지식 기본 경로 |
| 9 | test_bad_settings | 잘못된 모드/top_k 및 Gemini 필수 설정 누락 거절 |
| 10 | test_config_override_and_secret_repr | 환경변수 주입·경로 덮어쓰기 및 repr에서 가상 키 숨김 |
| 11 | test_import_without_site_packages | 별도 Python -S 프로세스에서 외부 SDK 없이 core import/fake 설정 |

선별은 사용자가 결정한다. 아직 어떤 항목도 복원하지 않았다.
긴급한 실행형 검증이 필요하면 사전에 이유와 범위를 설명하고 승인을 요청한다.
