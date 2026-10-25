# Team Sync

Status: **passed**

Tests: 5 / Passed: 5 / Failure events: 0 / Skipped: 0 / Expected failures: 0

검사 범위: 현재 작업 파일. main과의 가상 병합은 수행하지 않음.

## 이번 실행의 검사 목록

config.json에서 활성화·선택한 항목만 실행했습니다.
설정으로 제외한 항목은 테스트 수와 Skipped에 포함하지 않습니다.

| 검사 묶음 | 기능 모듈 식별명 | 테스트 항목 식별명 | 명세 | 결과 |
|---|---|---|---|---|
| Example-01: 학습 진행률 | Tester/example/Example-01.py | Tester.example.tests.test_example_01.ProgressTests.test_progress_pass | 진행률 정상 사례: 4개 중 1개 완료 시 25%를 반환한다. | passed |
| Example-01: 학습 진행률 | Tester/example/Example-01.py | Tester.example.tests.test_example_01.ProgressTests.test_remaining_pass | 남은 개수 정상 사례: 4개 중 1개 완료 시 3개가 남는다. | passed |
| Example-02: 퀴즈 점수 | Tester/example/Example-02.py | Tester.example.tests.test_example_02.Example02Tests.test_all_correct | 전체 정답: 5문항을 모두 맞히면 100점이다. | passed |
| Example-02: 퀴즈 점수 | Tester/example/Example-02.py | Tester.example.tests.test_example_02.Example02Tests.test_correct_answers_exceed_total | 잘못된 입력: 정답 수가 전체 문항 수보다 크면 거부한다. | passed |
| Example-02: 퀴즈 점수 | Tester/example/Example-02.py | Tester.example.tests.test_example_02.Example02Tests.test_partial_correct | 일부 정답: 5문항 중 3문항을 맞히면 60점이다. | passed |

## 실패 및 미통과 상세

없음
