# M07 — Tester 연동·통합 검증·데모 완료

## 목표
독립 로컬 단위 테스트에 더하여 기존 `Tester/automation/check.py`가 D 주요 모듈을 검사하게 하고, 데모 질문을 재현 가능한 형태로 제공한다.

## 실제 Tester 구조 확인 사항
- 등록 파일: `Tester/config.json` -> `checks` 배열.
- `module`: 저장소 루트 기준의 **실존 파일**, `test`: `Tester` 기준 상대 경로.
- `Tester/tests/test_d_*.py`를 추가, `Tester/tests/__init__.py` 존재/생성 확인.
- `local.enabled`, `workflow.enabled` 설정; include 생략=전체 메서드 실행. 등록하지 않은 파일은 자동 검색되지 않음.
- 현재 workflow는 `main` push + `workflow_dispatch`(main 조건)에만 실행. D 브랜치 push만으로 CI 자동 실행되지 않음.
- CI는 Python 3.12만 설치하고 패키지 의존성 설치 없음: 표준라이브러리 중심 코어 테스트를 등록하거나 CI 변경 승인을 받아 추가.

## 작업 범위
- `Tester/tests/test_d_knowledge.py`, `test_d_retriever.py`, `test_d_action.py`, `test_d_chat_service.py`(실제 구현 파일에 맞춰 조정).
- `Tester/config.json` 기존 checks 보존·D checks만 추가.
- `scripts/demo_queries.py` 등 fake 모드 로컬 질문 4개 시연.
- `README.md` 실행법, 키 설정, 테스트 명령, 미연동 상태 기록.
- Team Sync의 check.py와 기존 워크플로우 변경 금지. 정말 필요한 의존성 변경은 사전 보고 후 별도 합의.

## 테스트 조건
1. `python -m unittest discover -s silver-ringo-d/tests -p 'test_*.py'` 통과.
2. `python Tester/automation/check.py --mode local` 통과 (기존 intentional failing demo가 있다면 **기존 실패를 D 실패로 오인하지 말고** 분리 보고; 기존 테스트 수정 금지).
3. `python Tester/automation/check.py --mode workflow` 통과, D tests가 선택되었는지 `Tester/.team-sync/results.json`에서 검사.
4. API fake 모드 smoke test. 실제 Gemini는 요청 한도/키 보유자 승인을 받은 뒤 수동 실행, 아니면 '미검증' 표시.
5. 4개 JSON의 상태/출처와 샘플 부족 경고를 README에 문서화.

## 최종 인수 기준
- 현재 근거/기능과 아이디어를 분리.
- 정상 질문 답변 JSON이 A에서 표시 가능.
- 근거 없는 질문은 모르겠다고 답변.
- 비활성 액션은 출력되지 않음.
- 데이터/검색/모델/인터페이스 관련 미해결 사항을 짧은 인수표로 보고.
