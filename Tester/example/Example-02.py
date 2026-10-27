"""Example-02: 퀴즈의 정답 수로 점수를 계산하는 예시."""


def calculate_quiz_score(
    correct_answers: int,
    total_questions: int,
) -> float:
    """정답 수를 0~100점으로 환산한다."""
    if (
        type(correct_answers) is not int
        or type(total_questions) is not int
    ):
        raise TypeError("정답 수와 전체 문항 수는 정수여야 합니다.")

    if total_questions <= 0:
        raise ValueError("전체 문항 수는 1 이상이어야 합니다.")

    if not 0 <= correct_answers <= total_questions:
        raise ValueError("정답 수는 0 이상, 전체 문항 수 이하여야 합니다.")

    return correct_answers / total_questions * 100