import unittest

from Tester.example.progress import (
    calculate_progress,
    calculate_remaining,
)


class ProgressTests(unittest.TestCase):
    def test_progress_pass(self):
        """진행률 정상 사례: 4개 중 1개 완료 시 25%를 반환한다."""
        actual = calculate_progress(completed=1, total=4)
        expected = 25.0

        self.assertEqual(
            actual,
            expected,
            msg=(
                "진행률 계산 결과가 기대값과 다릅니다. "
                "입력: completed=1, total=4. "
                f"실제값={actual!r}, 기대값={expected!r}"
            ),
        )

    def test_progress_fail_demo(self):
        """진행률 실패 출력 예시: 정상 결과 25%에 잘못된 기대값 50%를 비교한다."""
        actual = calculate_progress(completed=1, total=4)
        expected = 50.0  # 보고서 확인을 위해 의도적으로 틀린 기대값

        self.assertEqual(
            actual,
            expected,
            msg=(
                "[의도된 실패] 진행률 오류 보고 형식을 확인합니다. "
                "입력: completed=1, total=4. "
                f"실제값={actual!r}, 기대값={expected!r}. "
                "정상적인 기대값은 25.0입니다."
            ),
        )

    def test_remaining_pass(self):
        """남은 개수 정상 사례: 4개 중 1개 완료 시 3개가 남는다."""
        actual = calculate_remaining(completed=1, total=4)
        expected = 3

        self.assertEqual(
            actual,
            expected,
            msg=(
                "남은 항목 개수가 기대값과 다릅니다. "
                "입력: completed=1, total=4. "
                f"실제값={actual!r}, 기대값={expected!r}"
            ),
        )

    def test_remaining_fail_demo(self):
        """남은 개수 실패 출력 예시: 정상 결과 3개에 잘못된 기대값 2개를 비교한다."""
        actual = calculate_remaining(completed=1, total=4)
        expected = 2  # 보고서 확인을 위해 의도적으로 틀린 기대값

        self.assertEqual(
            actual,
            expected,
            msg=(
                "[의도된 실패] 남은 개수 오류 보고 형식을 확인합니다. "
                "입력: completed=1, total=4. "
                f"실제값={actual!r}, 기대값={expected!r}. "
                "정상적인 기대값은 3입니다."
            ),
        )