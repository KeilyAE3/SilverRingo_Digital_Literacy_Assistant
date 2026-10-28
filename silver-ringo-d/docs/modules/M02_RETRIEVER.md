# M02 — JSON Retriever (키워드/유의어 기반)

## 목표
외부 DB·LLM·임베딩 없이 로컬 지식 문서에서 질의와 관련 있는 결과를 **결정론적으로** 선택한다. JSON의 전체 내용을 API에 보내지 말고 상위 k개 문서만 반환한다.

## 입력/출력
`Retriever.search(query: str, top_k: int=4, include_ideas: bool=False) -> list[KnowledgeHit]`.
- 각 Hit는 안정적 ID, 점수, 일치 표현, 원본 출처 포함.
- `include_ideas`는 질문이 실제로 미래 기획을 묻는 경우에만 ChatService가 활성화. `retired`는 명시적 과거 질문에서만 별도 논리로 검색.

## 요구 설계
1. 한국어 공백/기호 정규화 + 영문 casefold. 단순 부분문자열의 가짜 양성 방지(예: `병`만으로 병원 매칭).
2. 점수 예: ID/제목 정확 일치 > keyword 정확 일치 > alias > 설명의 단어 일치 > 문자 n-gram 근접. 실제 가중치는 테스트 기반 문서화.
3. `title`이 길거나 일반적 표현이 많다고 과대 랭킹하지 않도록 길이 보정 및 최소 점수 threshold.
4. `aliases`는 수동 정제된 말뭉치. 벡터를 **생성하는 척**하지 않는다. 의미 벡터 검색은 M08에서 별도 어댑터.
5. 동일 데이터·질문이면 결과 순서 동일(점수 동률은 문서 ID 순).
6. 부적절한/관련 없는 검색 결과는 0개 반환할 수 있어야 한다. 근거 없는 답변보다 0개가 낫다.
7. query length cap, top_k clamp, empty query 명확한 처리. 악성 문자열을 파일 경로나 명령으로 해석하지 않는다.

## 파일
`src/silver_ringo_d/retriever.py`, `tests/test_retriever.py`, 필요시 `normalizer.py`. KnowledgeStore 인터페이스에만 의존.

## 필수 시연
- `키오스크 카드 결제` → `course:kiosk:level:5`.
- `기계로 햄버거 주문하기` → kiosk course/level.
- `병원 접수 도우미` → guide:ddocdoc.
- `실버링고는 어떤 앱` → app:overview.
- `정부24로 민원서류 지금 발급해줘` → 현재 실행 가능한 기능이 아님. 일반 응답에서 아이디어를 활성 문서처럼 제공하지 않음.

## 평가/수락
`tests/fixtures/retrieval_cases.json`을 기반으로 검색 결과 기대 ID/금지 ID 검증. 목표: 지정된 active queries 적중 100%; forbidden ID는 0. 테스트 실패는 실제 질의/점수/원인 보고하고, 데이터가 부족하면 무작정 메꿔 넣지 않는다.
