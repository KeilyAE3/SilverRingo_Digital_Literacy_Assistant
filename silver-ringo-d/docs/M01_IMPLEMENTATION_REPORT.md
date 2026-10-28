# M01 구현 보고 및 검증 보류

## 실행 정책

사용자 지시에 따라 실행형 검증은 추후로 보류한다. 긴급한 실행 검증이
필요하면 이유와 실행 범위를 설명하고 승인을 요청한다. API 키는 필요하지 않다.
이번에 실행한 Python은 C JSON을 생성하기 위한 변환 스크립트뿐이다.
KnowledgeStore, 검증 CLI, 테스트, 서비스 및 외부 API는 실행하지 않았다.
따라서 아래 구현 항목은 코드 작성 상태이며 실행 수락 완료를 뜻하지 않는다.

## 원본 조사

작업 시작 Git 상태는 clean, 브랜치는 ydy_workspace였다. git fetch origin 후
아래 커밋을 git show로 읽었으며 다른 팀 브랜치를 checkout하거나 변경하지 않았다.

| 팀 | 브랜치 | 고정 커밋 |
|---|---|---|
| A | hjh | 28772a853240c2a4794233547c9a44ca5f587492 |
| B | Juno | 0025219bc56c20a9a59d98e5cc2cfc81d348df18 |
| C | jib | f011f17f8c422f104ed8ba72ed8480713958b7e7 |

- A HomeScreen/MainActivity: 학습·생활 가이드 목적지는 임시 화면, 채팅·음성은 준비 중 알림이다.
- B MainDashboardScreen/SilverAccessibilityService/OverlayScriptProvider: 똑닥 단일 진입,
  Native 7단계와 Web 4단계를 별개로 확인했다. Native 단계2는 키워드 매칭이 아니라
  FOCUS_INPUT + isEditable 판정이다. 초안의 키워드 누락을 보완했다.
- B docs/ideas_and_design.md의 과거 다중 서비스 지원 주장과 AGENTS.md의
  똑닥 단일 집중·폐기 기록이 충돌한다. 현 코드와 최신 결정 기록을 우선하여
  정부24·카카오T는 retired, 전자증명서는 보류된 idea로 유지한다.
  과거 문서의 법적 적합성·100% 작동 주장은 지식에 채택하지 않았다.
- C content.json의 레벨/안내 문구는 초안과 일치한다. scripts/sync-content.cjs는
  content.js → content.json으로 생성한다. D는 생성된 JSON을 입력으로 사용한다.
- C LearningEntry에서 앞 레벨 통과 판정과 코스 진입점을 확인했다.
  VALIDATION.md의 Android 빌드·기기 미검증 한계도 유지한다.

## 변경과 계약

- knowledge/*.json: 출처별 commit 추가. A 소개와 B 서비스/단계는 미통합 한계를
  드러내도록 prototype으로 정리. A 미구현 목적지는 placeholder 유지.
- learning_courses.json: 직접 편집하지 않고 import_jib_content.py로 재생성.
  guides가 문자열 사전에서 9개 레코드 배열로 바뀐다. 각 레코드에
  id, step_id, title, content, status, source가 있어 독립 검색 단위가 된다.
  기존 level_id와 웹/네이티브 단계 식별자는 유지한다.
- C 각 출처에 입력 파일 SHA-256을 기록. alias는 curated_by_d를 유지하며
  코스 요약과 가이드 제목도 D 편집값임을 표시한다. 상품·가격은 제외한다.
- action_catalog의 4개 액션은 모두 disabled/unintegrated 유지.
  idea/retired는 is_executable=false와 빈 related_action_ids를 가진다.
- knowledge_store.py: 4종 스키마·필수 필드·배열·ID 유일성·상태·출처·액션 FK,
  코스 1/레벨 6/가이드 9 및 B 단계 7/4를 검사한다.
  load/reload 전체 성공 후 스냅샷을 교체하고 실패 시 이전 스냅샷을 유지한다.
  반환값은 복사본이며 기본 documents()는 idea/retired를 제외한다.
- source_changes(repository): 고정 커밋의 파일 blob을 로컬 origin 참조와 비교한다.
  네트워크 접속은 없으며 참조 최신화는 별도 git fetch가 필요하다.
- M00 SourceRef는 변경하지 않았다. 상세 commit/hash는 원본 JSON에 보존되며,
  정규화된 KnowledgeDocument에는 M00의 branch/path/kind를 사용한다.
- M01 단계에서 활성 액션은 거절한다. M04에서 검증된 활성화 계약에 맞춰 확장한다.
- M00 테스트 파일은 사용자 요청으로 제거했다. M01 자동 테스트도 선별 대기한다.

## 자료 구성 (정적 집계 예상값; 적재 실행 결과 아님)

| 자료 | 문서 수 |
|---|---:|
| A 앱/메뉴 | 5 |
| C 코스 1 + 레벨 6 + 가이드 9 | 16 |
| B 서비스 1 + Native 7 + Web 4 | 12 |
| 과거 구상 | 1 |
| 폐기 기록 | 2 |
| 합계 | 36 |

예상 상태: implemented 0, prototype 29, placeholder 4, idea 1, retired 2.
기본 검색 후보는 33개이며 액션 4개는 검색 문서 수에 포함하지 않는다.

추출 예:
- A app:learning_menu: IT 기능 배우기 — 임시 안내 화면, C 미연결, placeholder.
- C course:kiosk:level:5: 결제 방법 배우기 — 실제 카드나 돈이 필요 없는 모의 결제, prototype.
- B guide:ddocdoc:native:7: 접수 신청 — 사용자가 버튼을 누르는 안내, prototype.
  guide:ddocdoc:web:4와 별개이며 실제 접수 완료를 확인한 사실로 해석하지 않는다.

## 보류한 실행 검증

아래 명령은 저장소 루트 기준이며 이번에는 실행하지 않았다.

```powershell
py -3.12 silver-ringo-d/scripts/validate_knowledge.py --repository .
```

성공 시 상태별/전체/기본 검색 문서 수와 원본 변화 목록을 출력한다.
종료 코드 0=유효·원본 변화 없음(비교 옵션 사용 시), 1=적재/검사 실패,
2=원본 변경 감지. 실행 환경에는 Python 3.12가 아직 없다.

추후 핵심 검증 후보는 정상 적재·기본 상태 제외, 잘못된 자료/FK 거절,
실패한 reload 후 이전 데이터 보존이다. 지금은 테스트 코드를 추가하지 않았다.
테스트가 없다는 이유로 성공 처리하거나 M01 수락 완료로 표시하지 않는다.

## 다음 단계 및 사용자 개입

현재 API 키·계정 설정은 불필요하다. 테스트 선별과 추후 Python 3.12 환경 검증이 남았다.
다음 모듈은 M02 키워드/유의어 검색이며 아직 착수하지 않았다.
M03 실제 API 시연 때 서버 환경변수의 키/모델 설정, M04~M06 연동 때 A/B/C 계약,
M07 통합 시 최종 실행 검증이 필요하다. 키를 채팅에 제출할 필요는 없다.
