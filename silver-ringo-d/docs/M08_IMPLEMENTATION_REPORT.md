# M08 검색 보완 보고 — 검토된 어휘 확장

## 결정

M08 명세의 수치 벡터 임베딩 Retriever는 구현하지 않았다. 현재 지식 표본은
작고, 외부 임베딩 모델·API·캐시를 추가할 만큼 반복적인 검색 실패가 확인되지
않았다.

대신 현재 규모에서 필요한 범위만 적용했다. 질의 정규화 단계에 검토된 소수의
표현을 추가하고, 기존 lexical Retriever가 질의에만 보조 표현을 확장하도록
보완했다. 지식 원본, 모델, 네트워크, 벡터 저장소는 추가하지 않았다.

## 변경 사항

- `src/silver_ringo_d/normalizer.py`
  - 계산/결제, 무인계산대/키오스크, 등록/접수 등 제한된 질의 표현 확장
- `src/silver_ringo_d/retriever.py`
  - 기존 점수화·상태 필터·top-k 구조 유지
  - 확장된 질의 어휘를 lexical 매칭에만 사용
- `Tester/tests/test_d_retriever.py`
  - 새 표현의 검색 적중을 검증
- `Tester/config.json`
  - main workflow는 D-QA-01~06을 대표하는 정확히 6개로 제한
- `Tester/tests/test_d_chat_service.py`
  - D-QA-04가 현재 지원하지 않는 기능이라는 안전 응답을 검증

## 검증한 추가 질의

```text
무인계산대에서 카드로 계산하는 법
→ course:kiosk:level:5

병원 등록을 도와줘
→ guide:ddocdoc
```

두 질의 모두 외부 AI나 임베딩 계산 없이 기존 로컬 JSON과 lexical Retriever로
처리되었다.

## Workflow 최종 구성

main workflow 실행 항목은 다음 6개다.

1. D-QA-06 지식 JSON 완결성
2. D-QA-01 앱 기능 안내
3. D-QA-02 마이크로러닝 탐색
4. D-QA-03 똑닥 안내
5. D-QA-04 미지원 기능 구분
6. D-QA-05 Gemini 실패 대응

상세 회귀 테스트와 기존 Example 테스트는 local 실행에만 남겨 팀 개발 중
검증에 사용하고, main workflow 결과에는 포함하지 않는다.

## 한계

이 방식은 정해진 표현 변형을 보완할 뿐, 자유로운 한국어 전체의 의미 검색을
보장하지 않는다. 향후 실제 사용자 발화에서 반복 실패가 확인될 때만 alias
추가를 검토하며, 그때도 임베딩 도입 필요성과 데이터를 별도로 평가한다.
