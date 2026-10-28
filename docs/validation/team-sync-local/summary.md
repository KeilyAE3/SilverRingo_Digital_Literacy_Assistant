# Team Sync

Status: **failed**

Tests: 9 / Passed: 7 / Failure events: 2 / Skipped: 0 / Expected failures: 0

선택 설정: local
검사 범위: 현재 로컬 작업 파일: main과의 가상 병합 없음

## 이번 실행의 검사 목록

config.json에서 활성화·선택한 항목만 실행했습니다.
설정으로 제외한 항목은 테스트 수와 Skipped에 포함하지 않습니다.

| 검사 묶음 | 기능 모듈 식별명 | 테스트 항목 식별명 | 명세 | 결과 |
|---|---|---|---|---|
| 통합 앱: 실제 키오스크 학습 엔진·콘텐츠 | silver-ringo-c/web/engine.js | Tester.tests.test_learning_engine.LearningEngineTests.test_existing_learning_engine | 실제 JS 엔진: 주문·가격·단계 완료·잠금 등 기존 11개 회귀 검사가 통과한다. | passed |
| 통합 앱: 실제 키오스크 학습 엔진·콘텐츠 | silver-ringo-c/web/engine.js | Tester.tests.test_learning_engine.LearningEngineTests.test_web_and_android_lesson_content_match | 콘텐츠 연결: 웹이 쓰는 content.js와 Android 목록용 content.json이 일치한다. | passed |
| Example-01: 학습 진행률 | Tester/example/Example-01.py | Tester.example.tests.test_example_01.ProgressTests.test_progress_fail_demo | 진행률 실패 출력 예시: 정상 결과 25%에 잘못된 기대값 50%를 비교한다. | failed |
| Example-01: 학습 진행률 | Tester/example/Example-01.py | Tester.example.tests.test_example_01.ProgressTests.test_progress_pass | 진행률 정상 사례: 4개 중 1개 완료 시 25%를 반환한다. | passed |
| Example-01: 학습 진행률 | Tester/example/Example-01.py | Tester.example.tests.test_example_01.ProgressTests.test_remaining_fail_demo | 남은 개수 실패 출력 예시: 정상 결과 3개에 잘못된 기대값 2개를 비교한다. | failed |
| Example-01: 학습 진행률 | Tester/example/Example-01.py | Tester.example.tests.test_example_01.ProgressTests.test_remaining_pass | 남은 개수 정상 사례: 4개 중 1개 완료 시 3개가 남는다. | passed |
| Example-02: 퀴즈 점수 | Tester/example/Example-02.py | Tester.example.tests.test_example_02.Example02Tests.test_all_correct | 전체 정답: 5문항을 모두 맞히면 100점이다. | passed |
| Example-02: 퀴즈 점수 | Tester/example/Example-02.py | Tester.example.tests.test_example_02.Example02Tests.test_correct_answers_exceed_total | 잘못된 입력: 정답 수가 전체 문항 수보다 크면 거부한다. | passed |
| Example-02: 퀴즈 점수 | Tester/example/Example-02.py | Tester.example.tests.test_example_02.Example02Tests.test_partial_correct | 일부 정답: 5문항 중 3문항을 맞히면 60점이다. | passed |

## 실패 및 미통과 상세

### Tester.example.tests.test_example_01.ProgressTests.test_progress_fail_demo

- 검사 묶음: Example-01: 학습 진행률
- 기능 모듈 식별명: Tester/example/Example-01.py
- 테스트 파일: example/tests/test_example_01.py
- 명세: 진행률 실패 출력 예시: 정상 결과 25%에 잘못된 기대값 50%를 비교한다.
- 결과: failed

    Traceback (most recent call last):
      File "C:\study\swproj\SilverRingo_Digital_Literacy_Assistant\Tester\example\tests\test_example_01.py", line 32, in test_progress_fail_demo
        self.assertEqual(
        ~~~~~~~~~~~~~~~~^
            actual,
            ^^^^^^^
        ...<6 lines>...
            ),
            ^^
        )
        ^
    AssertionError: 25.0 != 50.0 : [의도된 실패] 진행률 오류 보고 형식을 확인합니다. 입력: completed=1, total=4. 실제값=25.0, 기대값=50.0. 정상적인 기대값은 25.0입니다.

### Tester.example.tests.test_example_01.ProgressTests.test_remaining_fail_demo

- 검사 묶음: Example-01: 학습 진행률
- 기능 모듈 식별명: Tester/example/Example-01.py
- 테스트 파일: example/tests/test_example_01.py
- 명세: 남은 개수 실패 출력 예시: 정상 결과 3개에 잘못된 기대값 2개를 비교한다.
- 결과: failed

    Traceback (most recent call last):
      File "C:\study\swproj\SilverRingo_Digital_Literacy_Assistant\Tester\example\tests\test_example_01.py", line 63, in test_remaining_fail_demo
        self.assertEqual(
        ~~~~~~~~~~~~~~~~^
            actual,
            ^^^^^^^
        ...<6 lines>...
            ),
            ^^
        )
        ^
    AssertionError: 3 != 2 : [의도된 실패] 남은 개수 오류 보고 형식을 확인합니다. 입력: completed=1, total=4. 실제값=3, 기대값=2. 정상적인 기대값은 3입니다.

