# M05 구현 보고 — ChatService 통합 파이프라인

## 구현 범위

M02 Retriever, M03 LLM/FakeLLM, M04 ActionRegistry를 `ChatService` 하나의 순수
오케스트레이션 경로로 연결했다. 서버나 Android UI는 이번 모듈 범위에 포함하지 않았다.

처리 순서는 다음과 같다.

```text
ChatRequest
  -> 현재 기능/계획 질문 판별
  -> LexicalRetriever.search(top_k=4)
  -> 근거가 없으면 안전한 안내 반환
  -> serialize_context
  -> FakeLLM 또는 GeminiAdapter
  -> source_ids를 검색 근거와 교차 검증
  -> ActionRegistry.suggest(hits)
  -> ChatResponse
```

계획 표현(`앞으로`, `향후`, `예정`, `계획`, `구상`, `추가될`)이 있는 경우에만
Retriever가 `idea` 자료를 검색 대상으로 포함한다. 액션은 어느 경우에도 LLM
문장에서 추출하지 않고 검색 문서의 `related_action_ids`와 ActionRegistry만 사용한다.

## 변경 파일

- `src/silver_ringo_d/chat_service.py`
- `tests/test_chat_service.py`

## 테스트 질의와 결과

### 키오스크 결제 연습

질의:

```text
키오스크에서 카드 결제를 연습하고 싶어요.
```

Retriever 선택 근거:

```text
course:kiosk:level:5
```

FakeLLM 응답:

```json
{
  "reply": "[모의 응답] 관련 시험 구현 자료를 찾았어요. 앱 통합과 기기 검증 여부는 별도 확인이 필요해요.",
  "source_ids": ["course:kiosk:level:5"]
}
```

최종 결과는 해당 source를 `ChatResponse.sources`에 포함했고, 현재 모든 카탈로그
액션이 비활성/미통합이므로 `suggested_actions`는 빈 목록이다.

### 근거 없는 질문

질의:

```text
달에 가는 방법을 알려 주세요.
```

Retriever 결과는 빈 목록이었다. LLM을 호출하지 않고 다음 안전 응답을 반환했다.

```text
앱에서 확인된 정보로는 지원 여부를 알 수 없어요. 궁금한 기능을 조금 더 자세히 말씀해 주세요.
```

### 계획 질문

질의:

```text
앞으로 전자증명서 기능도 만들 계획이야?
```

계획 의도로 판별하여 `idea` 자료까지 검색하되, 실행 액션은 반환하지 않았다.
구상/폐기 자료를 현재 실행 기능으로 승격하지 않는 정책을 유지했다.

참고로 `카카오T`와 `정부24`는 현재 자료에서 `retired`로 관리되므로 기본 검색에서는
제외된다. 해당 기능을 현재 지원한다고 응답하지 않는 것이 정상이다.

## 테스트 실행

```text
unittest discover -s tests -p test_chat_service.py
```

결과:

```text
Ran 4 tests
OK
```

## 한계와 다음 단계

- FakeLLM은 네트워크 없는 구조 검증용 고정 응답이며 자연어 품질을 검증하지 않는다.
- 실제 HTTP 요청/응답, 상태 코드, FastAPI 오류 변환은 M06에서 구현한다.
- 현재 지식 표본과 액션 카탈로그가 제한적이므로 검색 일반화 평가는 M07에서 별도 수행한다.
- M06은 `ChatService.handle(ChatRequest)`를 HTTP 요청 모델과 연결하고 `LLMError`를
  계약된 429/503 응답으로 변환해야 한다.
