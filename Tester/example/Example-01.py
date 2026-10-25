def calculate_progress(completed: int, total: int) -> float:
    """완료 개수와 전체 개수로 진행률(0~100)을 계산한다."""
    if type(completed) is not int or type(total) is not int:
        raise TypeError("완료 개수와 전체 개수는 정수여야 합니다.")

    if total <= 0:
        raise ValueError("전체 개수는 1 이상이어야 합니다.")

    if not 0 <= completed <= total:
        raise ValueError("완료 개수는 0 이상, 전체 개수 이하여야 합니다.")

    return completed / total * 100


def calculate_remaining(completed: int, total: int) -> int:
    """아직 완료하지 않은 항목의 개수를 계산한다."""
    if type(completed) is not int or type(total) is not int:
        raise TypeError("완료 개수와 전체 개수는 정수여야 합니다.")

    if total <= 0:
        raise ValueError("전체 개수는 1 이상이어야 합니다.")

    if not 0 <= completed <= total:
        raise ValueError("완료 개수는 0 이상, 전체 개수 이하여야 합니다.")

    return total - completed