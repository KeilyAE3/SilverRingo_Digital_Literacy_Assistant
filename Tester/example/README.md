# 테스트 작성 예시

| 기능 모듈 | 테스트 파일 | 구성 |
|---|---|---|
| Example-01.py | tests/test_example_01.py | 학습 진행률·남은 개수: 성공 2개, 의도된 실패 2개 |
| Example-02.py | tests/test_example_02.py | 퀴즈 점수: 정상 입력 2개, 잘못된 입력 1개, 모두 통과 |

검사 등록은 Tester/config.json에서 관리합니다.
파일명에 하이픈이 있어 테스트에서는 importlib.import_module로 불러옵니다.

저장소 최상위에서 실행:

python Tester/automation/check.py

Example-01의 의도된 실패 때문에 전체 결과는 failed가 됩니다.
이 예시는 실제 애플리케이션의 정상 작동을 검증하지 않습니다.