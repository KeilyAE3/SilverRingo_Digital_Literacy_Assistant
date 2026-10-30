# Silver Ringo 2차 통합 — 2026-10-29

1.1.1/code 3을 팀 공용 main에 반영하는 기준 문서다. 기준 main은 2e8f75e이며 기존 팀 작업과 개인 브랜치를 보존한다. APK는 Git에 업로드하지 않는다. 최신 반영 범위·검증·미해결 사항은 [릴리스 기록](RELEASE_1_1_1.md)을 따른다. 아래 브랜치 표는 10월 29일 조사 기록이다.

## 무엇을 실제로 바꿨나

| 기존 상태 | 변경 내용 | 이유 |
|---|---|---|
| main에는 Juno v3.0.5 기반 가이드가 있었다 | v3.0.6~3.0.7의 접근성 서비스와 설정 XML만 적용했다 | 스크롤 후 강조 위치 추적, 인증·가입·PASS 입력 화면 오인식 차단, 대상이 없을 때 이전 말풍선 제거를 반영한다 |
| 홈의 AI 입력은 준비 중 안내만 표시했다 | 같은 앱 안에 챗봇 화면과 HTTP 클라이언트를 연결했다 | 기존 D 서버의 실제 POST /v1/chat 계약으로 질문·최근 대화를 보내고 답변·근거 상태·오류를 표시한다 |
| 챗봇에 사용자용 주소 설정 화면이 있었다 | 해당 화면을 제거하고 debug/release의 개발자 빌드 주소로 자동 연결한다 | 운영 주소 미설정 시 서비스 준비 중으로 안내하고 일반 사용자에게 연결 설정을 요구하지 않는다 |
| D 지식의 홈 메뉴 설명이 과거 단독 UI의 미연결 상태였다 | 학습·가이드 설명의 근거를 main의 실제 MainActivity로 바꾸고, 채팅 설명은 기존 HTTP 계약에 맞췄다 | 이미 연결된 기능을 계속 준비 중이라고 설명하지 않도록 한다. 액션 카탈로그는 비활성 상태를 유지한다 |
| API 37에서 기존 Espresso가 InputManager.getInstance 오류로 시작하지 못했다 | 테스트 전용 Espresso 3.7.0, runner 1.7.0, ext.junit 1.3.0 적용 | 공식 수정에 따라 반사 호출 문제를 해소해 기존 기기 테스트까지 실행한다 |
| 테스트는 D 내부 서비스 위주였다 | 기존 FastAPI HTTP 경계와 Android 실제 HTTP 클라이언트의 계약·타임아웃·연결 실패 검사를 추가했다 | 화면이 있어도 통신이 깨진 상태를 성공으로 간주하지 않는다 |
| 밝은 기능 화면에서도 흰 상태 표시줄 글자가 보였다 | 홈 이외 화면의 상태 표시줄 글자를 어둡게 전환했다 | 최신 Android의 투명 상태 표시줄에서 대비를 확보한다 |

학습 웹 콘텐츠·엔진·브리지·학습 완료 판정·자산 복사 작업·가이드 홈 복귀는 기존 통합 것을 유지했다. 새로 확인되지 않은 콘텐츠나 음성 입력은 구현했다고 표시하지 않는다. 최신 연결 설정 수정판은 versionCode 3 / versionName 1.1.1이며 applicationId는 com.silverlingo.app 그대로다.

## 조사한 최신 브랜치와 반영 여부

| 브랜치 | 최신 커밋 | 판정 |
|---|---|---|
| main | 2e8f75e | 1차 통합 Android와 Tester, D 서버를 이미 포함. 이번 기준 |
| Juno | 35fe76d | 기준 main의 Juno 원본은 0025219. 이후 수정된 서비스·XML 2개만 선별 반영. 원본 MainActivity/Manifest로 덮어쓰지 않음 |
| jib | f011f17 | 1차 통합 시점과 동일. web/tests/scripts의 추가 변경 없음. 기존 Kotlin 완료 판정·테스트 보존 |
| hjh | 28772a8 | 1차 통합 시점과 동일. 원본 HomeScreen은 참고하고 통합본에 실제 채팅 콜백을 추가 |
| ydy_workspace_release | 7f74fc2 | 신규 D 챗봇 릴리스. 이미 main의 조상이므로 중복 복사하지 않음 |
| ydy_workspace | 1034fd0 | D 개발 이력. 릴리스 전 원본을 main 위에 덮어쓰지 않음 |
| copilot/implement-synchronization-agents | 63296a0 | 과거 협업 문서 작업. 최신 Tester를 과거 등록·계약 방식으로 교체하지 않음 |

삼중점 diff의 added 표시는 main에 기능이 없다는 뜻이 아니다. 공통 조상 이후 다른 방식으로 가져온 파일이 있으므로 현재 blob/실제 소스를 직접 비교했다. 10월 30일 fetch에서도 main은 2e8f75e였다. jib에는 별도 스마트폰 1주차 학습(5c7224c)이 추가되어 있었으나 이번 완성된 1.1.1 범위에는 포함하지 않았다. 해당 브랜치를 수정하지 않았다.

## 챗봇의 실제 구조와 연결 계약

D 소스는 `SilverRingo_Chat-bot/src/silver_ringo_d/`다. Python 3.12+, FastAPI/uvicorn, 로컬 JSON 지식, 키워드·유의어 검색, ChatService, FakeLLM 또는 선택적 GeminiAdapter로 구성된다. Android 내에서 Python이나 LLM을 실행하지 않는다. Spring Boot 가이드 서버와도 별개다.

- `GET /health`: `{status, knowledge_loaded}`.
- `POST /v1/chat`, Content-Type application/json.
- 요청: `{message, history:[{role,content}]}`. 질문 1~1000자, history 최대 6개, 역할 user/assistant, 각 content 최대 1000자. 별도 기능 파라미터·사용자 키를 임의로 추가하지 않는다.
- 응답: `{reply, sources:[{id,category,status}], suggested_actions:[{id,type,target,label}], request_id}`.
- 오류: `{error,message}`. 422 입력 오류, 413 큰 요청, 429 호출 제한, 503 LLM/지식 사용 불가를 처리한다.
- Android는 연결 제한 5초·읽기 제한 50초를 둔다. 서버 Gemini는 시도당 기본 15초, 최대 3회 재시도할 수 있어 읽기 제한을 더 길게 잡았다. 각 제한은 전체 절대 실행시간 보장이 아니다.
- 전송 중 중복 전송을 막고, 실패하면 질문을 유지하며 다시 시도를 제공한다. 다른 화면으로 나가면 UI coroutine은 취소되고 진행 중 IO는 타임아웃 범위에서 정리된다.
- 최근 화면 대화 12개를 유지하고 서버에는 최근 6개만 보낸다. 긴 답변은 화면에 그대로 표시하지만 다음 요청의 history에는 계약에 맞춰 1000자까지 보낸다. 화면 회전 저장은 제공하나 홈으로 나갔다 새 채팅에 들어오면 새 대화를 시작한다. 대화 DB는 추가하지 않았다.
- 서버 응답 최대 64 KiB, 필수 JSON 필드 확인, 자동 redirect 금지. URL에 사용자명·비밀번호·query token·fragment를 허용하지 않는다.
- 앱 인증키나 Gemini 키는 없다. 현재 D API 자체에도 사용자 인증 계약은 없다. 공개 배포 주소·인증 정책은 서버 담당자 합의가 필요하다.
- suggested_actions는 표시 데이터다. 현재 모든 카탈로그 액션이 disabled이며 서버의 target 문자열로 외부 앱을 실행하지 않는다.

가이드의 실제 웹 WebView와 학습의 로컬 자산 WebView는 서로 다른 설정을 유지한다. 챗봇은 HttpURLConnection을 사용하며 두 WebView의 JavaScript 브리지와 섞지 않는다. INTERNET 권한은 기존 가이드 앱에 이미 있어 추가 권한은 불필요했다.

## 서버 실행과 기기 주소

저장소 루트에서 Python 3.12 이상으로 별도 가상환경을 사용한다. 이 PC의 검증 환경은 `.local/chat-venv`이며 Git에서 제외된다.

```powershell
python -m venv .local/chat-venv
.\.local\chat-venv\Scripts\python.exe -m pip install -e './SilverRingo_Chat-bot[api,test]'
$env:SR_D_LLM_MODE = 'fake'
.\.local\chat-venv\Scripts\python.exe -m uvicorn silver_ringo_d.api:app --host 127.0.0.1 --port 8000
```

서버 기본 URL은 개발자가 빌드 시 지정한다. debug는 CHAT_DEV_BASE_URL, release는 CHAT_PROD_BASE_URL을 사용하며 기본값은 비어 있다. 일반 사용자 주소 입력 화면은 제거했다. `/v1/chat`은 앱이 붙인다. 자세한 명령과 서버 환경변수 전달은 [챗봇 운영 기준](CHATBOT_DEPLOYMENT.md)을 따른다.

| 환경 | 개발자가 빌드에 사용할 주소/설정 |
|---|---|
| USB 실제 기기 또는 에뮬레이터 + adb reverse | PC에서 `adb reverse tcp:8000 tcp:8000` 실행 후 `http://127.0.0.1:8000`. reverse가 있어야 기기의 loopback이 PC 서버로 연결된다 |
| 표준 Android 에뮬레이터 | PC loopback 서버를 `http://10.0.2.2:8000`으로 접근 가능. 환경별 방화벽 확인 |
| 같은 Wi-Fi의 실제 기기 | 서버를 `--host 0.0.0.0`으로 시작하고 CHAT_DEV_BASE_URL에 실제 PC LAN 기본 URL을 지정한다. 0.0.0.0은 앱에 입력하는 목적지 주소가 아니다. 팀 테스트용 사설망에서 방화벽 허용 필요 |
| 배포 환경 | 실제 HTTPS 기본 URL 사용. 교수님이 서버 제공 예정이며 AWS는 예시로만 언급됨. 아직 실제 주소·배포 방식 없음 |

이번 debug APK만 로컬 HTTP를 허용한다. release 챗봇 클라이언트는 HTTPS만 허용한다. 주소는 개발자 빌드 설정으로만 관리하고 기존 sr_chat/server_url은 읽지 않는다. 운영 주소가 미설정이면 챗봇 서비스 준비 중을 표시한다. 주소가 설정됐지만 연결이 실패하면 쉬운 안내·재시도를 표시하며 학습·홈·가이드 진입은 독립적으로 동작한다. 가이드의 실제 사이트·외부 앱은 별도 네트워크가 필요하다.

실제 Gemini 사용 시 서버에 `SR_D_LLM_MODE=gemini`, `GEMINI_MODEL`, `GEMINI_API_KEY`를 환경변수로 제공하고 `[gemini]` extra를 설치한다. 값을 문서/명령 기록에 실제로 적지 않는다. 현재 uvicorn의 기본 로더는 `.env`를 자동으로 읽지 않는다. 기존 local_settings 함수가 있다는 이유로 자동 적용된다고 가정하지 않는다. 키와 실제 모델·운영 서버가 제공되지 않아 이번에는 Gemini 원격 호출을 하지 않았다. fake 응답은 모델 품질 검증이 아니다.

## 검증 및 앞으로 개발할 기준

- JDK 17 / Gradle 8.7 / AGP 8.6.1 / Kotlin 2.0.21 / Compose BOM 2024.09.03 / SDK 35 / minSdk 26 유지. 이 데스크톱에 부족했던 SDK35와 Build Tools34를 설치했으며 프로젝트 버전을 올리지 않았다.
- 학습 추가: 기존 `silver-ringo-c/web/content.js` 수정 → sync-content로 content.json 갱신 → 기존 engine/브라우저 테스트 → APK 자산 복사 확인. 새 주제는 저장 키와 잠금 정책을 먼저 합의한다.
- 새 화면 연결: 기준 MainActivity 목적지와 HomeScreen 콜백에 연결하고 기능이 onExit으로 복귀를 위임한다. 별도 런처 앱을 추가하지 않는다.
- D 지식 추가: 기존 JSON 스키마·source commit·상태·허용목록을 유지한다. 액션 실행을 켜려면 A/B/C 소유자의 진입점/권한/잠금 검사를 먼저 합의한다.
- 기존 Team Sync 등록·Python/Node 검사를 보존하고 HTTP 검사 2개를 추가했다. API 검사 의존성을 설치하는 단계만 기존 workflow에 추가했다. Android/브라우저 검사는 이번 로컬에서 별도로 실행했으며 현재 CI가 자동 실행한다고 주장하지 않는다.
- 키·운영 서버 미제공, 똑닥 실제 ARM64 기기와 한국어 TTS·인증·접수 최종 동작은 미검증이다. 합격 수가 실서비스 호환성을 의미하지 않는다.

상세 결과와 담당자별 검토는 ANDROID_VALIDATION.md / ANDROID_FILE_CHANGES.md / TEAM_SYNC_AUDIT.md의 2차 통합 항목을 본다. 과거 1차 기록은 삭제하지 않았다.
