# 지식 리소스 및 공통 데이터 계약 v0.1

## 1. 네 파일의 역할 및 스키마
- `knowledge/app_info.json`: `{schema_version:1, entries:[AppEntry]}`
- `knowledge/learning_courses.json`: `{schema_version:1, courses:[Course]}`. `levels[]`는 단계 배열.
- `knowledge/assistant_guides.json`: `{schema_version:1, services:[GuideService]}`. `native_steps[]`와 `web_steps[]`는 **서로 다른 원본 단계체계**이므로 유지.
- `knowledge/action_catalog.json`: `{schema_version:1, actions:[Action]}`. 검색 문서가 아닌 실행 검증 원본.

## 2. 모든 사용자 지식 레코드의 공통 필드
| 필드 | 타입 | 필수 | 의미 |
|---|---|---|---|
| id | string | 예 | 전역 고유 ID (`app:overview`, `course:kiosk:level:5`, `guide:ddocdoc:step:7`) |
| title | string | 예 | 사용자에게 보여도 되는 명칭 |
| summary / content | string | 예 | RAG 검색과 설명의 본문 |
| keywords | string[] | 권장 | 검색용 명시적 표현 |
| aliases | string[] | 권장 | 동의 표현·어르신 구어체 (출처/가설 구별) |
| status | string | 예 | `implemented`/`prototype`/`placeholder`/`idea`/`retired` |
| source | object | 예 | `{branch,path,kind}` (실제 코드/명세/아이디어) |
| related_action_ids | string[] | 권장 | action_catalog의 키에만 연결, 없으면 빈 배열 |

`status` 정의:
- `implemented`: 해당 브랜치 실제 구현 코드에 존재(통합/실기기 검증을 의미하지 않음)
- `prototype`: 실험·데모 또는 검증 한계가 있는 구현
- `placeholder`: UI 버튼/명칭은 존재하나 목적지·동작이 미구현
- `idea`: 문서상 구상, **실제 서비스라고 말하지 않음**
- `retired`: 폐기/과거 구현, 일반 지원 목록에서는 제외

`source.kind`: `code`, `integration_doc`, `design_doc` 중 하나. 같은 사실이 코드와 오래된 문서에서 충돌하면 코드 우선, 갈등 로그 작성.

## 3. 정규화된 검색 단위: KnowledgeDocument
```json
{
  "id": "course:kiosk:level:5",
  "category": "learning",
  "title": "결제 방법 배우기",
  "content": "모의 카드 결제를 연습합니다. 실제 카드나 돈이 필요하지 않습니다.",
  "keywords": ["카드", "결제", "키오스크"],
  "aliases": ["무인 주문 결제", "기계로 계산"],
  "status": "prototype",
  "source": {"branch": "jib", "path": "silver-ringo-c/web/content.json", "kind": "code"},
  "related_action_ids": ["open_kiosk_course"]
}
```

위의 `aliases`는 *D파트 검색 보완 제안*으로, 원본이 직접 제공한 표현이라고 간주하지 않는다. M01에서 `curated` 표시와 검증이 필요.

## 4. Action 스키마
```json
{
  "id": "open_kiosk_course",
  "kind": "navigate",
  "target": "learning.kiosk",
  "label": "키오스크 배우기 열기",
  "enabled": false,
  "status": "unintegrated",
  "owner": "A+C",
  "requires": [],
  "source": {"branch":"jib","path":"silver-ringo-c/android/app/src/main/java/kr/silverringo/learning/LearningEntry.kt","kind":"code"}
}
```

- `target`은 현재 **제안 식별자**로 A가 실제 내비게이션 계약에 합의한 ID가 아니다.
- `enabled`는 실제 A의 클릭 처리까지 연결되고 테스트될 때만 true. LLM 문자열을 action ID로 직접 실행하지 않는다.
- 파라미터가 필요한 경우 명시된 스키마/화이트리스트로 검증. 외부 URL, package name 임의 실행 금지.
- 기능 설명용 `related_action_ids`는 액션 권한을 부여하지 않는다.

## 5. 중복·자료 변경·버전
- C `content.json` 원본을 진실의 원천으로 한다. D 파생 리소스는 수작업 수정이 아니라 추출/검증 스크립트 갱신을 우선.
- A/B Kotlin 소스는 JSON으로 공개된 계약이 없어 **처음에는 정리된 스냅샷**으로 관리. 향후 각 팀이 JSON 계약을 주면 전환.
- 원본 해시나 commit SHA를 기록할 수 있으면 기록; 없으면 브랜치와 경로라도 유지.
- JSON은 UTF-8. 최상위 `schema_version=1`, ID 유일성, 배열 타입, source 존재, 액션 FK의 유효성 검사.
- 잘못된 JSON 시 서비스 부팅 실패(명확한 오류) 또는 이전 정상 스냅샷 유지; 절대 빈 데이터로 조용히 응답하지 않는다.

## 6. 정보 노출/검색 필터 정책
- 기본 질문: `implemented`, `prototype`, `placeholder` 정보 검색 허용. `placeholder`는 '준비 중'으로 표현.
- 계획 질문: `idea`도 검색하되 반드시 '구상/미지원'으로 표시.
- `retired`: 기본 검색과 추천 모두 제외. 역사 질문에서만 부연 가능.
- 사용자의 증상/건강/계좌번호/주소/인증정보를 지식 JSON이나 로그에 누적하지 않는다.
- 오버레이는 실제 의료 판단/접수 실행의 대리자가 아니다. 진행 상태를 확인할 수 없으면 완료라고 표현하지 않는다.

## 7. 지식 표본 확보 정책
실제로 확인된 정보만 `implemented`/`prototype`으로 등록한다. 임의 아이디어가 필요하면 사용자의 승인을 받거나 이미 B 설계 문서에 명시된 것만 `idea`로 추가한다. 아이디어를 넣더라도 검색·액션 검증 테스트에서 실제 기능과 혼동되지 않도록 검증한다.
