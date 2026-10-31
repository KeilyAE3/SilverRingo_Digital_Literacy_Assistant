> DHub 확장 코드 배포 완료. [실제 반영·백업·테스트·실호출 결과](DHUB_DEPLOYMENT_RESULT.md)를 먼저 확인한다. 아래는 배포 전 로컬 구현 기록이며 당시의 미배포 문구는 과거 상태다.

# 챗봇 답변 확장·바로가기 — 로컬 검수판

이번 변경은 로컬 코드·테스트·Debug APK까지만 수행했다. main 브랜치의 기존 DHub 연동 미커밋 변경사항을 보존했으며 Commit/Push/병합/VM 접속/서버 재시작은 하지 않았다. 실제 Gemini 호출도 하지 않았다.

## 무엇이 달라졌나

기존에는 자료가 없으면 모델 호출 전에 같은 확인 불가 문구로 끝났다. 이제 사진 보내기·와이파이·글자 크기·비밀번호 변경 같은 일반 사용법과 간단한 잡담은 빈 근거로도 기존 GeminiAdapter를 호출한다. 시스템 지침은 일반 지식과 Silver Ringo 기능 안내를 구분한다. 일반 설명에는 출처·버튼을 만들지 않으며 앱 기능의 존재는 검색 근거로만 설명한다. 명시적인 앱 지원 질문에서 자료가 없으면 기존 확인 불가 안내를 유지한다.

모델·키·환경변수·SDK 호출·재시도·오류 및 응답 검증은 재개발하지 않았다. GeminiAdapter, config.py, api.py, retriever.py, normalizer.py는 그대로다. POST /v1/chat 요청/응답 필드도 그대로이며 서버 rate limiter를 새로 만들지 않았다.

후속 질문은 짧은 '그다음은?', '그다음에는 어떻게 해?' 같은 지시형 표현일 때만 최근 6개 이력에서 가장 가까운 실질 사용자 질문을 검색 주제로 사용한다. 모든 이력을 이어 붙이지 않는다. 새 질문이나 '그럼 와이파이는 어떻게 연결해?'처럼 새 주제가 있으면 현재 질문을 검색한다. 최근 잡담을 넘어 더 오래된 키오스크 주제를 찾아가지 않는다. 원래 질문과 최근 이력은 그대로 Gemini에 전달한다.

비밀번호라는 단어 자체를 차단하는 처리를 제거했다. 변경 방법은 허용하고 실제 값으로 보이는 입력·이력은 기존 민감정보 검사로 외부 호출 전에 차단한다. 이 검사는 정규식이므로 모든 자유 형식 개인정보를 완벽하게 탐지하지 않는다. 앱은 실제 비밀번호·인증번호를 요구하지 않는다.

## 바로가기 조건과 실제 목적지

검색 hit 중 실제 답변이 인용한 자료만 후보가 된다. 구현/시험 구현 자료, 최소 점수 4 및 원래 검색 최고 점수의 75% 이상, 관련 기능 주제와 연결된 활성 액션만 사용한다. 관련 없는 하위 결과·미인용·placeholder/idea/retired는 제외한다. 서버에서 ID 중복 제거 후 최대 2개를 반환한다. 단어만 맞는 것으로는 버튼이 생기지 않는다.

| ID | target | 실제 Android 동작 | 상태 |
|---|---|---|---|
| open_kiosk_course | learning.kiosk | 기존 LearningEntry 학습 목록 열기. 레벨을 직접 지정하지 않고 잠금/진도는 기존 구현 유지 | 활성 |
| open_guide_home | guides.home | 기존 MainDashboardScreen 열기. 권한·앱 설치/웹 선택은 기존 UI에서 사용자가 진행 | 활성 |
| open_learning_home | learning.home | 현재 동일 학습 목록의 중복 진입이므로 별도 버튼을 만들지 않음 | 비활성 |
| start_ddocdoc_companion | guides.ddocdoc | 서비스 직접 시작 계약을 확정하지 않았으므로 실행하지 않음 | 비활성 |

똑닥의 개별 설명 자료가 인용되어도 가이드 화면만 열도록 부모와 Native/Web 단계의 related_action_ids를 연결했다. 병원 접수나 서비스 실행을 자동으로 수행하는 연결은 아니다.

Android ChatActions는 ID/type/target 조합과 해당 기능의 현재 출처를 다시 확인한다. 임의 URL·액션·목적지는 거부하고 버튼 문구도 로컬 문구를 사용한다. reply/sources/actions를 답변별로 함께 저장하므로 다음 질문에 버튼이 옮겨가지 않는다. 학습·가이드에서 나가면 채팅으로 돌아오며, 일반 홈 진입 동작은 유지한다. SaveableStateHolder와 직렬화 가능한 메시지를 사용해 화면 왕복·상태 복원 후에도 대화를 유지한다. 화면에 최근 12개 메시지, 요청에 최근 6개 이력이라는 기존 범위는 유지한다.

## 변경 파일

| 위치 | 변경 이유 |
|---|---|
| SilverRingo_Chat-bot/src/silver_ringo_d/chat_service.py | 일반 질문 호출, 제한된 후속 검색, 인용·관련성에 근거한 액션 후보 |
| SilverRingo_Chat-bot/src/silver_ringo_d/llm.py | 빈 근거 일반 요청 허용, 정상 비밀번호 사용법 허용, Fake의 일반 응답 경로 |
| SilverRingo_Chat-bot/src/silver_ringo_d/system_prompt.py | 일반 디지털 안내/앱 지원 구분, 후속 질문·잡담·안전 정책 |
| SilverRingo_Chat-bot/src/silver_ringo_d/action_registry.py | 확인된 두 route만 허용, 상태/점수·중복·최대 개수 검사 |
| SilverRingo_Chat-bot/src/silver_ringo_d/knowledge_store.py | 확인된 integrated 액션의 적재 검증 |
| SilverRingo_Chat-bot/knowledge/action_catalog.json | 두 화면만 활성화, 실제 통합 MainActivity 근거 |
| SilverRingo_Chat-bot/knowledge/app_info.json | 학습 메뉴의 중복 액션 대신 확인된 학습 진입 연결 |
| SilverRingo_Chat-bot/knowledge/assistant_guides.json | 똑닥 자료와 확인된 가이드 화면 연결 |
| android/app/src/main/java/com/silverlingo/app/chat/ChatScreen.kt | 답변별 메타데이터·버튼, 기존 입력·오류·재시도 보존 |
| android/app/src/main/java/com/silverlingo/app/chat/ChatClient.kt | 기존 HTTP 유지, 저장 가능한 응답·메시지 타입. 이전 DHub 오류 처리도 보존 |
| android/app/src/main/java/com/silverlingo/app/chat/ChatActions.kt | Android 자체 ID/목적지/출처 허용 목록 |
| android/app/src/main/java/com/silverlingo/app/MainActivity.kt | 기존 목적지 연결과 기능→채팅 복귀·채팅 상태 보존 |
| Tester/tests/test_d_chat_expansion.py, 기존 test_d_*.py, Tester/config.json | 일반/후속/미지원/민감정보/SDK Mock/액션·기존 계약 회귀 검사 등록 및 기대값 갱신 |
| android/app/src/test/.../ChatActionsTest.kt, android/app/src/androidTest/.../ChatNavigationTest.kt | 위조 액션 차단·답변별 버튼·화면 왕복/복원 검사 |
| SilverRingo_Chat-bot/README.md, INTEGRATION_CONTRACT.md, docs/CHATBOT_DEPLOYMENT.md | 기존 MVP 설명과 확장판·VM 배포 상태 구분 |

기존 DHub 주소가 들어 있는 android/gradle.properties와 이전 ChatUiTest/ChatClientTest 변경사항을 보존했다. 가이드·학습 내부 코드, 권한·서비스·WebView 엔진은 수정하지 않았다.

## 검증과 APK

- Debug APK 빌드·서명 검증 성공, 버전 1.1.1/code 3 유지.
- Kotlin 9/9: 기존 실제 로컬 HTTP 계약·타임아웃·429/503·학습 판정 + 액션 허용 목록·메시지 직렬화.
- API37 에뮬레이터 6/6: 기존 검사와 새 학습/가이드 버튼 이동, 시스템 뒤로가기·가이드 복귀 버튼, 채팅 상태 복원, 메시지별 버튼 위치, 일반 응답의 위험 액션 차단. 전부 로컬 Mock이며 실제 DHub/Gemini 요청은 하지 않았다.
- Team Sync workflow 18/18 통과. local 33개 중 31개 통과, 기존 Example-01 의도적 실패 2개만 남아 있다. 검사 삭제·비활성화 없이 새 검사 8개를 양쪽에 추가했다.
- GeminiAdapter는 SDK Mock으로 빈 근거/검색 근거/이력/시스템 지침 전달·한 번 생성·민감정보 시 호출 차단·기존 재시도/위조 출처 거절을 검사했다. 실제 모델의 문장 품질을 검증한 것은 아니다.
- lint 오류 0, 기존 경고 51. 로그는 .local/chat-expansion-android.log, chat-expansion-local.log, chat-expansion-workflow.log.
- APK: C:\study\swproj\SilverRingo_Digital_Literacy_Assistant\.local\artifacts\SilverRingo-1.1.1-chat-expanded-debug.apk (Git 제외).
- SHA256: D94AA2974109D1B6D635A9BBED9C7996B5B253337A0A09F33484F06E7F1D501F.

## DHub 반영 시 별도 적용 방법

**이 APK만 설치해도 아직 배포하지 않은 새 서버 정책이 적용되지는 않는다.** 기본 주소는 기존 DHub HTTPS URL 그대로다. 현재 VM이 이전 코드를 실행한다면 기존 답변과 비활성 액션 응답을 받는다.

서버 반영 대상은 위 표의 Python 5개 파일과 knowledge JSON 3개다. 문서·테스트는 검토/검증용이며 Android 파일은 서버에 배치하지 않는다. 새 라이브러리는 필요 없다.

1. 사용자가 배포를 승인한 별도 작업에서 VM 현재 커밋·파일 변경과 요청 제한 패치를 먼저 확인·보존한다. VM의 파일이 로컬과 같다고 가정하지 않는다.
2. 현재 실행 코드·지식과 로컬 패치를 비교한다. 디렉터리 전체 복사나 Git 강제 초기화 대신 해당 8개 파일의 변경만 적용하고, 같은 파일에 VM 수정이 있으면 병합 검토한다.
3. 특히 api.py/프록시의 요청 제한, systemd, .env/비밀 설정·모델·키·HTTPS 설정을 덮어쓰지 않는다. 로컬 api.py는 이번 변경 대상이 아니다. VM의 제한 위치는 확인하지 못했으므로 임의로 단정하지 않는다.
4. 서버 소스 5개와 지식 3개는 서로 의존하므로 함께 검증한다. KnowledgeStore 적재와 Mock 테스트를 통과한 후 별도 승인 범위에서 서비스 재시작·상태 검사를 한다. 이번 작업에서는 수행하지 않았다.
5. 배포 후에만 최소 실제 Gemini 요청과 스마트폰으로 일반 질문·후속 문맥·RAG 버튼·권한 화면·복귀를 확인한다. 기존 rate limit이 유지되는지도 운영 담당자가 확인한다.

## 남은 제한

후속 주제 선택과 바로가기 주제 판정은 보수적인 규칙이며 자유로운 모든 표현을 분류하지는 않는다. 실제 모델의 앱 기능 오안내·일반 답변 출처 사용·기기별 안내 정확성은 별도 평가해야 한다. 민감정보 검사는 포괄적 탐지기가 아니다. 스마트폰 실기기, 실제 Gemini의 확장 정책, VM 요청 제한 보존 및 배포는 미검증이다.
