# 구현 체크리스트와 작동 구조

필수 M00~M07 중 4개 모듈(M00~M03)의 코드가 작성되어 있다. 모듈 개수는 작업량이나 제품 완성도를 뜻하지 않는다.
M08은 선택 사항이다. 아직 채팅 API/생성/화면 실행은 연결되지 않았다.

## 모듈별 상태

| 모듈 | 구현 | 검증/남은 사항 |
|---|---|---|
| M00 패키지·타입·설정 | 구현됨 | Python 3.12.10 core import/fake 확인. 패키지 빌드·설치는 미검증. 이전 테스트 제거 유지 |
| M01 지식·변환·적재 | 구현됨 | 원본 대조, C 생성, 3.12 정상 적재 36개 확인. 실패 reload 등 전체 수락은 보류 |
| M02 키워드/유의어 검색 | 구현됨 | 3.12 fixture 10/10·입력/필터/순서 경계 확인. 일반화 품질 검증은 남음 |
| M03 Gemini/Fake 어댑터 | 구현됨 | 3.12 Fake/mock 검증 완료. 실제 SDK 설치·키·Gemini 호출 미검증 |
| M04 ActionRegistry | 미구현 | 4개 액션은 비활성, 실행 검증 로직 미구현 |
| M05 ChatService | 미구현 | 이력/검색 문맥/프롬프트/응답 통합 미구현 |
| M06 HTTP API | 미구현 | Android 요청/응답 경로 미연결 |
| M07 Tester·데모 | 미구현 | 사용자 테스트 항목 지시 대기 |
| M08 임베딩 | 선택, 미구현 | 정량적 검색 실패 근거 후 검토 |

## 현재 실행 가능한 경로

```mermaid
flowchart TD
  SRC["A/B/C 원본 · 읽기 전용"] --> JSON["출처·상태를 기록한 로컬 JSON"]
  JSON --> STORE["M01 구현 · KnowledgeStore 검증/적재"]
  STORE --> DOC["전체 36개 · 기본 후보 33개"]
  Q["Python 호출의 사용자 질의"] --> N["M02 구현 · 정규화/상태 필터"]
  DOC --> N
  N --> SCORE["제목·keyword·alias·본문 점수화"]
  SCORE --> HITS["최소 점수 통과 · 상위 4개 기본값<br/>문서/점수/일치 표현/출처"]
  HITS --> EMPTY["근거가 부족하면 0개"]
```

## 전체 계획에서 구현된 위치

실선은 현재 코드 경로, 점선은 아직 연결되지 않은 계획이다.

```mermaid
flowchart TD
  M00["✓ M00 기반 타입·설정"] --> M01["✓ M01 지식 적재"]
  J["지식 JSON"] --> M01
  M01 --> M02["✓ M02 관련 근거 검색"]
  Q["질의"] --> M02
  M02 --> H["KnowledgeHit 목록"]
  H -.-> M05["미구현 M05 요청 구성·응답 통합"]
  M05 -.-> M03["✓ M03 Gemini/Fake 경계 · 실제 API 미검증"]
  A["비활성 액션 카탈로그"] -.-> M04["미구현 M04 액션 검증"]
  M03 -.-> R["계획: 답변 + 출처 + 검증된 액션"]
  M04 -.-> R
  R -.-> M06["미구현 M06 HTTP API"]
  M06 -.-> UI["A Android UI · D 연동 미완료"]
  M07["미구현 M07 Tester·통합 데모"] -.-> M06
  M08["선택·미구현 M08 임베딩"] -.-> M02
```

M02는 근거 문서 수를 제한한다. 모델의 토큰 예산·이력·최종 프롬프트 구성은 M03/M05에서 다룬다.
M01은 지식 자료를 준비하고, M01~M06 연결 후 검색 증강 생성(RAG)의 전체 경로가 된다.

## 디렉터리

```text
silver-ringo-d/
├─ pyproject.toml, .env.example, .gitignore    [기반 구성]
├─ src/silver_ringo_d/
│  ├─ __init__.py, models.py, config.py       [M00]
│  ├─ knowledge_store.py                     [M01]
│  ├─ normalizer.py, retriever.py             [M02]
│  └─ system_prompt.py, llm.py, gemini_adapter.py, local_settings.py [M03]
├─ knowledge/*.json (4종)                    [M01 자료]
├─ scripts/
│  ├─ import_jib_content.py                  [M01 변환]
│  └─ validate_knowledge.py                  [M01 수동 검증 도구]
├─ tests/fixtures/retrieval_cases.json        [기존 평가 표본]
│  (test_*.py 없음, Tester 등록 없음)
└─ docs/
   ├─ M00_TEST_SELECTION.md
   ├─ M01_IMPLEMENTATION_REPORT.md
   ├─ M02_IMPLEMENTATION_REPORT.md
   └─ 07_SPRINT_TRACKER.md
```

## 환경 및 사용자 개입

- Python 3.12.10 설치/실행 확인. 현재 셸의 python은 3.11.8, py 기본값은 3.13.
  따라서 이 프로젝트에서는 py -3.12를 명시한다. 시스템 설정은 수정하지 않았다.
- 이번 승인 범위: Python 3.12 호환 확인과 M02 로컬 fixture/경계 질의 실행.
  외부 API를 호출하지 않았으며 API 키는 지금 필요하지 않다.
- Tester 항목은 후속 사용자 지시 전까지 추가하지 않는다.
- 다음 모듈은 M04. 사용자는 D의 .env 또는 서버 환경변수에 키/모델을 설정한다. 다음 단계에서 weak/rag 실제 호출 확인 예정이며 이번에는 호출하지 않았다.

자세한 가중치·제한·실행 결과는 [M02 보고](M02_IMPLEMENTATION_REPORT.md)를 참고한다.

## M03까지 확인한 실제 로컬 흐름

```mermaid
flowchart TD
  Q["질의"] --> RET["M02 검색"]
  DATA["M01 지식 적재"] --> RET
  RET --> CTX["M03 선택 근거 JSON"]
  HIST["최근 대화 최대 6개"] --> REQ["M03 요청 구성"]
  CTX --> REQ
  Q --> REQ
  SYS["앱의 고정 역할 정의"] --> CFG["별도 system_instruction 필드"]
  REQ --> FAKE["FakeLLM / SDK mock · 검증 완료"]
  CFG --> FAKE
  REQ -.-> REAL["Gemini API · 실제 호출 미검증"]
  CFG -.-> REAL
  FAKE --> JSON["reply + source_ids JSON 검증"]
  REAL -.-> JSON
  JSON -.-> SERVICE["M05 서비스 통합 · 미구현"]
```

M03 시스템 지침은 사용자 질의에서 분리해 관리하지만 generateContent 전송에는 매번 포함한다.
API에 영구 기억시키는 기능은 구현하지 않았다. source_ids는 근거 ID이며 실행 권한이 아니다.
[키 저장·API 규격·다음 실호출 절차](M03_IMPLEMENTATION_REPORT.md)를 참고한다.