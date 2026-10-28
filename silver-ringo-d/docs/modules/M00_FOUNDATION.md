# M00 — 패키지 골격·공통 계약

## 목표
`silver-ringo-d/`에 실제 import 가능한 Python 프로젝트 골격을 만들고 이후 M01~M07이 의존할 공통 데이터 타입/설정 체계를 고정한다. **Gemini 호출, 검색 구현, API 엔드포인트는 만들지 않는다.**

## 선행 문서
`docs/00_MASTER_PLAN.md` + `docs/01_DATA_CONTRACT.md` + 본 문서만 우선. `docs/02_API_INTEGRATION.md` 인터페이스 참고.

## 생성 파일
- `pyproject.toml` 또는 `requirements.txt`(최소; 의존성은 선택 import)
- `src/silver_ringo_d/__init__.py`, `src/silver_ringo_d/models.py`, `config.py`
- `tests/test_models.py` 표준 라이브러리 기반
- `.env.example`, `.gitignore`(기존 사용자 설정 보존)

## 모델 제안
- `SourceRef(branch: str,path: str,kind: Literal[...])`
- `KnowledgeDocument(id,category,title,content,keywords,aliases,status,source,related_action_ids)`
- `KnowledgeHit(document,score,matched_terms)`
- `ActionSuggestion(id,type,target,label)`
- `ChatRequest(message,history)` / `ChatResponse(reply,sources,suggested_actions,request_id)`
공통 핵심은 `dataclass`로 시작해도 된다. FastAPI 경계의 Pydantic 모델은 M06에서 별도 작성.

## 요구·설계 결정
- ID는 소문자·콜론·언더스코어 등 기존 프로젝트 문맥에 맞는 안정적인 문자열(파일명/배열 인덱스 불사용).
- 환경변수: `GEMINI_API_KEY`, `GEMINI_MODEL`, `SR_D_LLM_MODE=fake|gemini`, `SR_D_TOP_K=4`, `SR_D_KNOWLEDGE_DIR`(선택).
- 테스트 실행 시 API 키 불필요. import만으로 Gemini SDK가 로드되지 않아도 동작해야 함.
- 실제 코드 위치는 `silver-ringo-d/src/silver_ringo_d`; `silver-ringo-d`를 import하지 않는다.

## 테스트/수락
- 타입 생성 및 기본 필드 검증.
- 모델 데이터 직렬화 용이성.
- 키 미설정이라도 fake/core 테스트가 실행.
- 기존 저장소 및 `Tester`는 건드리지 않음.

## Codex 종료 보고
변경 파일 목록, 로컬 import 성공, 실행된 테스트 및 결과, 다음 M01이 가정해야 할 공통 타입명을 보고. 이 단계는 자동 commit/push하지 않는다.
