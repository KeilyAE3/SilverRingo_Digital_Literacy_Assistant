# Team Sync

Status: **failed**

Tests: 4 / Passed: 2 / Failure events: 2 / Skipped: 0 / Expected failures: 0

검사 범위: 현재 작업 파일. main과의 가상 병합은 수행하지 않음.

## 전체 검사 목록

| 검사 묶음 | 기능 모듈 식별명 | 테스트 항목 식별명 | 명세 | 결과 |
|---|---|---|---|---|
| 예시: 학습 진행률 계산 | Tester/example/progress.py | Tester.example.tests.test_progress.ProgressTests.test_progress_fail_demo | 진행률 실패 출력 예시: 정상 결과 25%에 잘못된 기대값 50%를 비교한다. | failed |
| 예시: 학습 진행률 계산 | Tester/example/progress.py | Tester.example.tests.test_progress.ProgressTests.test_progress_pass | 진행률 정상 사례: 4개 중 1개 완료 시 25%를 반환한다. | passed |
| 예시: 학습 진행률 계산 | Tester/example/progress.py | Tester.example.tests.test_progress.ProgressTests.test_remaining_fail_demo | 남은 개수 실패 출력 예시: 정상 결과 3개에 잘못된 기대값 2개를 비교한다. | failed |
| 예시: 학습 진행률 계산 | Tester/example/progress.py | Tester.example.tests.test_progress.ProgressTests.test_remaining_pass | 남은 개수 정상 사례: 4개 중 1개 완료 시 3개가 남는다. | passed |

## 실패 및 미통과 상세

### Tester.example.tests.test_progress.ProgressTests.test_progress_fail_demo

- 검사 묶음: 예시: 학습 진행률 계산
- 기능 모듈 식별명: Tester/example/progress.py
- 테스트 파일: example/tests/test_progress.py
- 명세: 진행률 실패 출력 예시: 정상 결과 25%에 잘못된 기대값 50%를 비교한다.
- 결과: failed

    Traceback (most recent call last):
      File "C:\Users\user\바탕 화면\작업실\SilverRingo_Assistant\Tester\example\tests\test_progress.py", line 30, in test_progress_fail_demo
        self.assertEqual(
    AssertionError: 25.0 != 50.0 : [의도된 실패] 진행률 오류 보고 형식을 확인합니다. 입력: completed=1, total=4. 실제값=25.0, 기대값=50.0. 정상적인 기대값은 25.0입니다.

### Tester.example.tests.test_progress.ProgressTests.test_remaining_fail_demo

- 검사 묶음: 예시: 학습 진행률 계산
- 기능 모듈 식별명: Tester/example/progress.py
- 테스트 파일: example/tests/test_progress.py
- 명세: 남은 개수 실패 출력 예시: 정상 결과 3개에 잘못된 기대값 2개를 비교한다.
- 결과: failed

    Traceback (most recent call last):
      File "C:\Users\user\바탕 화면\작업실\SilverRingo_Assistant\Tester\example\tests\test_progress.py", line 61, in test_remaining_fail_demo
        self.assertEqual(
    AssertionError: 3 != 2 : [의도된 실패] 남은 개수 오류 보고 형식을 확인합니다. 입력: completed=1, total=4. 실제값=3, 기대값=2. 정상적인 기대값은 3입니다.

