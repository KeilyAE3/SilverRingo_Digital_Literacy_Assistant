# 테스트 및 품질 계획 / Team Sync 접속 명세

## 1. 필수 테스트 군
1. JSON 형식·필드·ID 중복·source 누락 검출
2. C 레벨 1~6 정확히 적재, 5단계 키오스크 결제 추출
3. B 똑닥 Native 단계와 Web 단계 ID 비혼동
4. A 미구현 메뉴를 동작 중이라 주장하지 않음
5. 키워드+유의어 검색: `기계로 햄버거 주문`, `키오스크 카드`, `병원 접수`, `실버링고 뭐야?`
6. 무관 질의에서는 낮은 점수 후보 억지 채택 금지
7. 기능 구상 질문과 실제 사용 질문 필터 분리 (`정부24`/`카카오T` 등)
8. `action_catalog`의 disabled/unknown ID는 100% 차단
9. C 레벨 잠금을 우회하는 액션 없음
10. Gemini mock 응답 사용, 프롬프트에 출처와 질문이 포함됨
11. Gemini 429/timeout/빈 결과/오류의 정상 JSON 실패 처리
12. POST /v1/chat 입력 제한, 응답 스키마, /health 보안
13. 민감한 요청·로그에 API 키/인증번호 재출력 금지(가벼운 마스킹 테스트)
14. 통합 테스트가 외부 인터넷 없이 성공

## 2. 검색 평가 fixture
`tests/fixtures/retrieval_cases.json`의 기대 ID/금지 ID를 기준으로 top_k=4에서 recall 및 forbidden hit 평가. 검색 미달 사례는 새 유의어 증거가 있는지 검토한 뒤 수정. "벡터라면 무조건 더 좋다"를 전제하지 않는다.

## 3. 테스트 위치 및 실행
권장 구현 위치: `silver-ringo-d/tests/test_*.py` (모듈 로컬 단위 테스트) + `Tester/tests/test_d_*.py` (공통 Team Sync 연결용). `Tester/tests/__init__.py`가 없으면 추가, 기존 구조는 보존.

모듈 import 이름은 `silver_ringo_d`로 일치시킨다. 저장소 경로 `silver-ringo-d/src/silver_ringo_d`는 `-`가 있으므로 직접 import 불가; 테스트 초반 `sys.path.insert(0, str(ROOT / "silver-ringo-d" / "src"))`하거나 editable install로 경로를 등록한다.

작업 브랜치에서 직접 실행:
```bash
python -m unittest discover -s silver-ringo-d/tests -p 'test_*.py'
python Tester/automation/check.py --mode local
python Tester/automation/check.py --mode workflow
```

`Tester/config.json` 신규 check 예시(기존 `checks` 배열에 **추가**, 덮어쓰지 않음):
```json
{
  "name": "D: RAG Retriever",
  "module": "silver-ringo-d/src/silver_ringo_d/retriever.py",
  "test": "tests/test_d_retriever.py",
  "local": {"enabled": true},
  "workflow": {"enabled": true}
}
```

위 경로의 `module`은 루트 기준, `test`는 **Tester/** 기준이다. `include` 생략=전체 테스트, `include: []`=0개 선택. 테스트 메서드명은 `test_` 시작하며 docstring 명세. 모듈 등록은 실제 파일이 존재하는 시점에만 한다.

## 4. CI 특이사항
- 현재 `.github/workflows/team-sync.yml`은 `main` push에서만 자동 실행된다. `ydy_workspace` push 테스트를 자동 확인했다고 주장하지 않는다.
- 워크플로에는 Python SDK/FastAPI 의존성 설치가 없다. 따라서 CI 대상으로 등록할 검사는 Python 표준 라이브러리만 import 가능한 **순수 핵심 모듈 및 mock 테스트**를 우선한다.
- HTTP TestClient 등 서드파티 의존 검사는 로컬 전용으로 따로 관리하거나 A/D 팀 합의 후 GitHub Actions에 설치 단계 추가.
- 현재 예시 Testers는 `workflow` 일부가 선택적이다. 기존 코드는 건드리지 않는다.
- `Tester/docs/TEAM_SYNC.md`에 구판 설명이 공존한다. 실제 코드 기준이 우선. 문서 개정은 별도 이슈로 제안한다.

## 5. 수락/보고 양식
- 통과: 테스트 개수/명령/수행 환경.
- 실패: 어떤 질의, 기대 ID, 실제 ID, 원인(자료 부족, alias 부족, 랭킹, 상태 정책, API 오류), 조치/보류.
- 성능: 외부 LLM 실측 토큰/지연은 수동 테스트 때만 확인. 비용/정확도 수치 임의 추정 금지.
