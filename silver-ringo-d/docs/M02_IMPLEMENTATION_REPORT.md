# M02 구현 보고 — 로컬 키워드/유의어 검색

## Python 확인

- 설치된 Python 3.12.10: `py -3.12`로 실행 성공.
- 현재 셸의 `python`은 여전히 3.11.8, `py` 기본 선택은 3.13이다.
- 시스템 PATH/기본 인터프리터는 변경하지 않았다. 프로젝트 명령은 `py -3.12`로 명시한다.
- Python 3.12의 `-S` 모드(서드파티 site-packages 제외)에서 M00 모델·설정 import,
  fake 설정, M01 지식 적재 성공. 36개 문서 중 기본 검색 후보 33개.
- M01의 실패 재적재/원본 변경 감지 등 모든 수락 조건을 재검증한 것은 아니다.
- 패키지 빌드·설치 검증은 하지 않았다. runtime dependencies는 비어 있지만,
  pyproject의 build-system은 setuptools>=68을 요구한다.

## 이번 변경 파일

- src/silver_ringo_d/normalizer.py: 공백/기호/NFKC/casefold 및 제한된 조사·어미 정리.
- src/silver_ringo_d/retriever.py: KnowledgeStore 기반 결정적 검색.
- docs/M02_IMPLEMENTATION_REPORT.md: 본 보고.
- docs/07_SPRINT_TRACKER.md: 구현·검증 상태와 다이어그램 갱신.
- .generated/m02-evaluation.json: 승인된 로컬 실행 결과. gitignore 대상이며 커밋용 아님.

기존 M01 수정 사항과 M00 테스트 삭제를 유지했다. tests/test_*.py, Tester,
GitHub Actions, A/B/C, 지식 JSON은 이번 M02에서 변경하지 않았다. commit/push 없음.
fetch 후 A/B/C 원본 참조는 M01에서 기록한 커밋과 같았다.

## M01/M02의 의미와 경계

RAG는 개별 JSON 파일의 이름이 아니라, 검색한 근거를 모델에 제공해 답변을 생성하는
전체 방식이다. 현재는 그중 지식 적재와 검색까지 연결되어 있다.

| 단계 | 책임 |
|---|---|
| M01 | 원본 대조, JSON 스키마·출처·상태·참조 검사, 문서 단위 적재 |
| M02 | 질의와 문서의 일치 정도를 산정하고 상위 k개 근거 반환 |
| M03 | Gemini/Fake 어댑터와 모델 호출 경계 |
| M05 | 질의·검색 근거·최근 이력으로 요청 구성 및 응답 통합 |

M02의 top_k는 문서 수 제한이다. 최종 프롬프트 길이/토큰 예산, 이력 포함 범위,
의도 판정, 개인정보 처리, 외부 API 전송은 아직 구현하지 않았다.
M01 스키마 통과는 지식의 사실성이나 검색 일반화 품질을 자동 보장하지 않는다.

## 공개 인터페이스

```python
from silver_ringo_d.knowledge_store import KnowledgeStore
from silver_ringo_d.retriever import LexicalRetriever  # Retriever도 같은 클래스

store = KnowledgeStore('silver-ringo-d/knowledge')
store.load()
retriever = LexicalRetriever(store)
hits = retriever.search('키오스크 카드 결제', top_k=4, include_ideas=False)
# hit.document: id/title/content/status/source/keywords/aliases/related_action_ids
# hit.score: 내부 랭킹 점수, 확률·신뢰도 백분율 아님
# hit.matched_terms: 일치한 정규화 표현
```

src 레이아웃이므로 설치하지 않은 셸에서는 import 전에 src 경로를 등록해야 한다.
API 키나 환경변수 없이 실행할 수 있다. store를 성공적으로 reload하면 다음 검색에 반영된다.
설정의 SR_D_TOP_K는 호출자가 search(top_k=settings.top_k)로 전달한다.
Retriever가 환경변수나 파일을 직접 읽지 않는다.

## 정규화·랭킹 규칙

1. Unicode NFKC + 영문 casefold. 기호를 공백으로 바꾸고 공백을 합친다.
2. 제한된 조사/어미 한 개를 제거하되 최소 2글자를 남긴다. 형태소 분석기는 아니다.
   일반적인 질의 표현은 불용어로 제외하고 한 글자 토큰은 사용하지 않는다.
3. 전체 토큰 일치만 사용한다. `병`을 `병원`으로, `병원비`를 `병원`으로 매치하지 않는다.
   n-gram/퍼지 검색은 오검출 억제를 위해 도입하지 않았다.
4. 기본 필드 가중치: 제목 6, keyword 5, alias 4, 본문 1.
   동일 단어가 여러 필드에 있으면 가장 높은 기여만 취한다.
5. 단어 희소도는 `1 + log((문서 수+1)/(해당 단어 문서 수+1))`.
   필드 길이는 `sqrt(1 + 고유 단어 수/12)`로 보정한다.
   단어 기여 합을 질의의 고유 유효 단어 수로 나눈다.
6. 2개 이상 단어로 된 연속 표현 일치에 제목 +12, keyword +8, alias +5 중
   최대 하나만 가산한다. 제목 전체 정확 일치 +30, ID 정확 일치 +50.
7. 제목/keyword/alias 일치가 없으면 본문에서 최소 2개 단어가 일치해야 한다.
   질의에 유효 단어가 3개 이상이면 최소 2개 일치를 요구한다(정확 ID/제목은 예외).
8. 6자리 반올림 점수가 4.0 이상인 결과만 채택한다. 점수 내림차순,
   동점이면 문서 ID 오름차순. top_k까지 반환하며 부족분을 억지로 채우지 않는다.

가중치와 임계값은 현재 작은 fixture에서 확인한 초기값이며 통계적으로 학습하거나
고령 사용자 발화 전반에서 최적화한 값이 아니다. 최종 규칙으로 아래 평가를 실행했다.

## 입력·상태 처리

- 빈 문자열/공백/기호만 있는 질의는 빈 결과.
- 문자열이 아닌 질의 또는 1000자 초과는 ValueError. 오류에 원문을 넣지 않는다.
- top_k는 정수만 허용(bool 거절), 0..10으로 clamp. 0/음수는 빈 결과.
- include_ideas는 bool만 허용. 기본은 implemented/prototype/placeholder만.
- include_ideas=True는 idea를 추가한다. 미래 질문인지는 후속 ChatService가 판단해야 한다.
- retired는 항상 제외한다. 역사 질의 전용 경로는 미구현.
- 악성 문자열은 텍스트로만 다룬다. 파일/명령 실행·외부 통신·질문 로깅이 없다.

## 승인된 로컬 검증 결과

사용자가 로컬 검색 검증을 승인했다. Python 3.12.10에서 PowerShell here-string을
`py -3.12 -`로 전달하여 임시 검증을 수행했다. 영구 테스트 파일/Tester 등록은 만들지 않았다.

- 기존 retrieval_cases.json 10개: 기대 ID·금지 ID 조건 10/10 통과.
- current 범위에서 기대 ID가 있는 질의 7개: top-4 내 적중 7/7.
- roadmap 기대 ID 1개: include_ideas=True에서 적중.
- 금지 ID 출현: 0.
- 입력 경계·상태 필터·정확 ID·반복 순서·동점 ID 정렬 등 assertion 21개 통과.
  이는 21개 테스트 파일/Tester 항목을 만든다는 뜻이 아니다.
- 최초 결과의 '병원 접수 도우미' 하위에 앱 소개가 섞여 다중 단어 질의의
  최소 2단어 일치 규칙을 보완한 뒤 위 결과를 다시 확인했다.

| 대표 질의 | 최상위 근거 | 점수 |
|---|---|---:|
| 키오스크 카드 결제 | course:kiosk:level:5 | 9.602931 |
| 기계로 햄버거 주문하기 | course:kiosk | 19.781367 |
| 병원 접수 도우미 | guide:ddocdoc | 12.311229 |
| 실버링고는 어떤 앱 | app:overview | 16.598301 |
| 정부24로 민원서류 지금 발급해줘 | 결과 없음 | — |

`병`, `병원비`, `오늘 날씨`, 공백, 명령 모양의 무관 텍스트도 결과가 없었다.
security fixture의 통과는 금지 ID가 없다는 뜻일 뿐, 개인정보 마스킹·대리 접수 차단을
구현했다는 뜻이 아니다. 검색 결과는 실행 허가가 아니다.

## 한계와 다음 작업

- 작은 고정 표본에서의 결과다. 띄어쓰기 없는 복합어, 오타, 새로운 구어체는 놓칠 수 있다.
- 한 글자 질의와 일반 단어는 의도적으로 보수적으로 처리한다.
- matched_terms는 일치한 정규화 표현이며 원문 문자 위치/인용 범위를 반환하지 않는다.
- 전체 문서 단위로 반환한다. 문서 중 일부 문장을 잘라 보내는 기능은 없다.
- 공통 어휘가 있는 하위 결과의 적합성은 추가 사용자 평가가 필요하다.
- 출처 SHA/hash는 M01 JSON에 남고 Hit의 SourceRef에는 branch/path/kind만 포함된다.
- 다음 구현은 M03 한 개. 실제 API 사용 시에만 사용자의 서버 키/모델 설정이 필요하다.
  Tester 검사 구성은 사용자 후속 지시를 기다린다.
