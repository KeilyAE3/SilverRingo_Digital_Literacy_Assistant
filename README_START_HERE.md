# SilverRingo D파트 — Codex 실행 패키지 v0.1
작성: 2026-10-28. 기준 저장소: https://github.com/KeilyAE3/SilverRingo_Digital_Literacy_Assistant

## 목적
D 담당 영역(Chat & Knowledge Engine)을 **하나의 Codex 작업 = 전체 계획 + 모듈 설계서 한 개** 원칙으로 순차 구현한다. A(hjh)/B(Juno)/C(jib) 브랜치는 읽기 전용으로 참조한다. D 대상 브랜치: `ydy_workspace`.

## 패키지 사용 순서
1. 저장소를 로컬에 clone/fetch하고 `ydy_workspace` 브랜치를 checkout. `git status`로 사용자 변경사항을 먼저 확인한다. 변경사항을 임의로 덮어쓰지 않는다.
2. ZIP 내부 `silver-ringo-d/` 폴더를 **저장소 루트**에 복사한다. 다른 디렉터리를 덮어쓰지 않는다. 이 폴더에는 구현 코드가 아니라 설계 문서, 샘플 지식 JSON, 테스트 질문 데이터, 프롬프트만 있다.
3. 데스크톱 Codex에서 **저장소 루트**를 열고 `CODEX_KICKOFF_PROMPT.md` 내용을 첫 입력으로 보낸다.
4. 후속 작업마다 `silver-ringo-d/prompts/TASK_TEMPLATE.md`를 복사하여 모듈 ID 하나를 지정한다. 예: `M00` → `M01` → `M02`…
5. 매 작업 종료 시 Codex 보고를 받고 코드·테스트·Git diff를 직접 검토한다. 다음 단계 진행을 지시한다.

## 문서 빠른 길잡이
- 전체 목표/모듈 구조/다이어그램: `silver-ringo-d/docs/00_MASTER_PLAN.md`
- 데이터와 품질·상태 계약: `silver-ringo-d/docs/01_DATA_CONTRACT.md`
- HTTP 인터페이스/오류 계약: `silver-ringo-d/docs/02_API_INTEGRATION.md`
- 실제 팀원 브랜치에서 수집할 데이터: `silver-ringo-d/docs/03_SOURCE_CATALOG.md`
- 정확도·품질 검증과 Team Sync: `silver-ringo-d/docs/04_TEST_PLAN.md`
- M00~M08 각 1개: `silver-ringo-d/docs/modules/`
- 샘플 데이터: `silver-ringo-d/knowledge/*.json`
- 검색 품질 평가 질문: `silver-ringo-d/tests/fixtures/retrieval_cases.json`
- Codex 상시 로컬 지침: `silver-ringo-d/AGENTS.md`

## 구현 단계
| 모듈 | 핵심 | 기본 스프린트 |
|---|---|---|
| M00 | 프로젝트 골격·공통 타입 | 필수 |
| M01 | 원본 수집·4개 JSON 구축·검증·상태관리 | 필수 |
| M02 | JSON 로더·키워드/유의어 Retriever | 필수 |
| M03 | Gemini 어댑터·답변 지침·Fake 어댑터 | 필수 |
| M04 | 액션 레지스트리·허용 목록 검증 | 필수 |
| M05 | ChatService 통합 | 필수 |
| M06 | FastAPI 외부 인터페이스·예외 처리 | 필수 |
| M07 | 공통 Tester 등록·통합 테스트·데모 | 필수 |
| M08 | 벡터/임베딩 Retriever 실험 | **조건부 확장** |

## 유의사항
- 샘플 리소스는 실제 브랜치의 확인된 문구에서 만든 **초안**으로, 출시/지원 기능을 확증하지 않는다.
- `idea` / `retired`는 일반 질문의 실제 지원 기능으로 추천하지 않는다. 실험용 아이디어는 반드시 근거, `status=idea`, `is_executable=false`를 명시한다.
- Gemini 키는 `.env` 또는 환경변수만 사용. APK/소스/로그에 절대 기록하지 않는다.
- 모든 테스트는 외부 API 없이 통과해야 한다. 라이브 Gemini 호출은 선택적 수동 검증이다.
- 이 패키지에서 GitHub 원격 브랜치를 수정하거나 커밋한 작업은 없다.
