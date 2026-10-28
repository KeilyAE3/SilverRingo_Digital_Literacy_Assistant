# Team Sync

Status: **passed**

Tests: 6 / Passed: 6 / Failure events: 0 / Skipped: 0 / Expected failures: 0

선택 설정: workflow
검사 범위: 현재 로컬 작업 파일: main과의 가상 병합 없음

## 이번 실행의 검사 목록

config.json에서 활성화·선택한 항목만 실행했습니다.
설정으로 제외한 항목은 테스트 수와 Skipped에 포함하지 않습니다.

| 검사 묶음 | 기능 모듈 식별명 | 테스트 항목 식별명 | 명세 | 결과 |
|---|---|---|---|---|
| D: Knowledge integrity | silver-ringo-d/src/silver_ringo_d/knowledge_store.py | Tester.tests.test_d_knowledge.DKnowledgeTests.test_json_resources_load_as_complete_snapshot | D-QA-06: 네 개 JSON 자료가 손상 없이 하나의 검증된 스냅샷으로 적재된다. | passed |
| D: ChatService QA | silver-ringo-d/src/silver_ringo_d/chat_service.py | Tester.tests.test_d_chat_service.DChatServiceTests.test_app_answer_contains_evidence | D-QA-01: 앱 설명 응답에 검색 근거가 포함된다. | passed |
| D: ChatService QA | silver-ringo-d/src/silver_ringo_d/chat_service.py | Tester.tests.test_d_chat_service.DChatServiceTests.test_learning_answer_contains_payment_evidence | D-QA-02: 마이크로러닝 응답이 카드 결제 학습 근거를 포함한다. | passed |
| D: ChatService QA | silver-ringo-d/src/silver_ringo_d/chat_service.py | Tester.tests.test_d_chat_service.DChatServiceTests.test_assistant_answer_contains_ddocdoc_evidence | D-QA-03: 병원 접수 안내 응답이 똑닥 근거를 포함한다. | passed |
| D: ChatService QA | silver-ringo-d/src/silver_ringo_d/chat_service.py | Tester.tests.test_d_chat_service.DChatServiceTests.test_unsupported_request_has_no_action | D-QA-04: 정부24 요청에 현재 지원하지 않는 기능임을 안전하게 안내한다. | passed |
| D: ChatService QA | silver-ringo-d/src/silver_ringo_d/chat_service.py | Tester.tests.test_d_chat_service.DChatServiceTests.test_gemini_failure_becomes_safe_error | D-QA-05: Gemini 호출 실패가 안전한 공통 오류로 변환된다. | passed |

## 실패 및 미통과 상세

없음
