# M07 구현 보고 — Tester 통합·QA 데모·예시 지식 분리

## 구현 범위

M07은 D 기능을 공통 `Tester/automation/check.py`로 지속 검증할 수 있도록
등록하고, 대표 QA 질문을 재현 가능한 FakeLLM 테스트로 구성했다. `check.py`,
`notify.py`, GitHub Actions workflow는 수정하지 않았다.

## 변경 사항

- `Tester/config.json`
  - 기존 Example 검사 2개를 보존
  - D Knowledge, Retriever, Action, ChatService 검사 4개 추가
- `Tester/tests/test_d_knowledge.py`
  - D-QA-06 JSON/KnowledgeStore 적재·액션 상태·예시 분리
- `Tester/tests/test_d_retriever.py`
  - D-QA-01~04 검색 적중 및 retired 차단
- `Tester/tests/test_d_action.py`
  - D-QA-04 비활성·미등록·파라미터 액션 차단
- `Tester/tests/test_d_chat_service.py`
  - D-QA-01~05의 ChatService/FakeLLM 경로
- `silver-ringo-d/scripts/demo_queries.py`
  - 4개 대표 질문의 로컬 데모
- `silver-ringo-d/README.md`
  - 실행법, 내부/외부 진입점, 데이터 한계, M08 플로팅 신호 고려사항
- `Tester/example/knowledge/m07_examples.json`
  - QA 전용 더미 지식
- `silver-ringo-d/tests/`
  - 기존 모듈별 테스트 3개를 Tester로 이전한 뒤 삭제

## 예시 지식 분리 정책

사용자가 요청한 표본 부족 보완용 더미 자료는 실제 실행 지식과 섞지 않았다.
[m07_examples.json](../../Tester/example/knowledge/m07_examples.json)은
`example_only=true`이며 다음을 포함한다.

- A 앱 설명/UI 맥락 예시 1개
- B 병원 접수 안내 예시 2개
- C 키오스크 코스 예시 3개

파일은 `silver-ringo-d/knowledge/` 밖에 있으므로 `KnowledgeStore` 기본 로더가
읽지 않는다. 예시 자료의 상태는 모두 `prototype`이지만 실제 A/B/C 연동 증거를
의미하지 않으며, 프로덕션 응답 근거로 사용하지 않는다.

## QA 질의와 기대 결과

| ID | 질의 | 검증 |
|---|---|---|
| D-QA-01 | 실버링고는 어떤 앱이야? | `app:overview` 검색·출처 응답 |
| D-QA-02 | 키오스크에서 카드 결제하는 법을 배우고 싶어 | `course:kiosk:level:5` 검색 |
| D-QA-03 | 똑닥으로 병원 접수를 하려는데 어려워 | `guide:ddocdoc:*` 검색 |
| D-QA-04 | 정부24로 등본을 발급해 줘 | 현재 지원하지 않는 기능이라는 안전한 응답·임의 액션 없음 |
| D-QA-05 | Gemini API 호출 실패 | `LLMError`가 안전한 공통 오류로 유지 |
| D-QA-06 | KnowledgeStore와 4개 JSON | 스냅샷 적재·자료 존재·액션 상태 확인 |

D-QA-04는 정부24 기능을 현재 지원하지 않는다는 맥락의 안전한 응답을 검증한다.
현재 계약상 D는 관련 없는 키오스크 이동 액션을 임의로 붙이면 안 되고,
`open_kiosk_course`도 비활성이다. UI 바로가기/플로팅 신호는 M08에서 별도
출력 계약으로 다룬다.

## 검색 보완

M08 명세의 벡터 임베딩은 구현하지 않았다. 현재 규모에서는 `normalizer.py`에
검토된 소수의 질의 표현(`계산`/`결제`, `무인계산대`/`키오스크`, `등록`/`접수`
등)을 추가하고 Retriever가 질의에만 적용하도록 보완했다. 외부 AI 호출, 수치
벡터, 임베딩 캐시, 모델 의존성은 추가하지 않았다.

## 최종 workflow 범위

main workflow에는 D-QA-01~06을 대표하는 정확히 6개 테스트만 실행하도록 설정했다.
기존 Example 테스트와 상세 D 회귀 테스트는 local에서만 실행한다. 따라서 팀원이
main의 Team Sync 결과를 볼 때 최종 기능 검증 6개만 확인할 수 있다.

## 실행 결과

공통 실행 명령:

```text
python Tester/automation/check.py --mode local
python Tester/automation/check.py --mode workflow
```

이번 구현에서는 Tester 실행을 통해 기존 Example 검사와 D 검사 전체 결과를
확인해야 한다. 기존 Example-01의 의도된 실패 항목이 local 전체 결과에 포함될
경우, 이는 M07 D 검사 실패가 아닌 기존 예시 설정으로 분리 보고해야 한다.

## 인터페이스 인수

- D 내부 진입점: `ChatService.handle(ChatRequest) -> ChatResponse`
- 외부 진입점: `POST /v1/chat`, `GET /health`
- Tester는 핵심 모듈을 실제 객체로 연결하고 외부 네트워크·Gemini 키를 사용하지 않는다.
- M08에서 필요할 경우 `ChatResponse`에 UI 보조 신호를 추가하되, 현재 M07은 액션
  허용목록과 응답 출처만 검증한다.
