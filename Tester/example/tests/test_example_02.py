import unittest
from importlib import import_module

example_02 = import_module("Tester.example.Example-02")

calculate_quiz_score = example_02.calculate_quiz_score


class Example02Tests(unittest.TestCase):
    def test_partial_correct(self):
        """일부 정답: 5문항 중 3문항을 맞히면 60점이다."""
        actual = calculate_quiz_score(
            correct_answers=3,
            total_questions=5,
        )
        expected = 60.0

        self.assertEqual(
            actual,
            expected,
            msg=(
                "퀴즈 점수 계산 결과가 기대값과 다릅니다. "
                "입력: correct_answers=3, total_questions=5. "
                f"실제값={actual!r}, 기대값={expected!r}"
            ),
        )

    def test_all_correct(self):
        """전체 정답: 5문항을 모두 맞히면 100점이다."""
        actual = calculate_quiz_score(
            correct_answers=5,
            total_questions=5,
        )
        expected = 100.0

        self.assertEqual(
            actual,
            expected,
            msg=(
                "모든 문항을 맞히면 100점이어야 합니다. "
                "입력: correct_answers=5, total_questions=5. "
                f"실제값={actual!r}, 기대값={expected!r}"
            ),
        )

    def test_correct_answers_exceed_total(self):
        """잘못된 입력: 정답 수가 전체 문항 수보다 크면 거부한다."""
        with self.assertRaises(
            ValueError,
            msg=(
                "정답 수가 전체 문항 수를 초과하면 "
                "ValueError가 발생해야 합니다. "
                "입력: correct_answers=6, total_questions=5."
            ),
        ):
            calculate_quiz_score(
                correct_answers=6,
                total_questions=5,
            )