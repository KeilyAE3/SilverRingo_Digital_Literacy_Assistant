# 챗봇 연결·서버 운영 기준 — 2026-10-31

> 최신 로컬 답변 확장·바로가기 구현과 VM에 적용할 파일은 [챗봇 확장 기록](CHATBOT_EXPANSION.md)을 따른다. 해당 확장판의 DHub 배포를 완료했으며 [배포 결과](DHUB_DEPLOYMENT_RESULT.md)에 요청 제한 보존과 실제 응답을 기록했다. 아래 DHub HTTPS 연결 검증은 이전 연동 작업 당시 결과다.

## 사용자 화면과 개발 설정

앱의 HTTP 주소 입력·연결 설정·주소 저장 화면을 제거했다. ChatScreen은 개발자가 빌드 시 지정한 BuildConfig.CHAT_BASE_URL을 사용한다. 과거 sr_chat/server_url 값은 읽지 않으며 사용자 데이터를 삭제하지 않는다.

| 빌드 | 개발자가 지정할 Gradle property | 현재 기본값 | 허용 |
|---|---|---|---|
| debug | CHAT_DEV_BASE_URL | android/gradle.properties의 DHub HTTPS 주소 | 개발용 HTTP 또는 HTTPS |
| release | CHAT_PROD_BASE_URL | android/gradle.properties의 DHub HTTPS 주소 | HTTPS만 |

두 값은 서로 대체하거나 fallback하지 않는다. debug 주소를 지정해도 release에는 전달되지 않는다. 2026-10-31에 제공받은 https://silverringo-api-gkswjdgh132.apps.dauai.org를 두 property에 각각 설정했다. 외부 주소에 :8000 또는 /v1/chat을 넣지 않는다. ChatClient가 /v1/chat을 한 번 붙인다. -P property로 개발 주소를 바꿀 수 있으며 두 설정은 여전히 독립적이다. URL은 비밀키가 아니지만 사용자명·비밀번호·query token·fragment가 있는 URL은 빌드에서 거절한다. Gemini 키는 이 property나 APK에 넣지 않는다.

미설정 앱에서는 '챗봇 서비스 준비 중'을 표시하고 전송을 비활성화한다. 설정된 서버가 응답하지 않으면 '지금은 답변을 받기 어려워요. 잠시 후 다시 시도해 주세요.'와 '다시 시도'를 표시한다. HTTP 429는 '질문이 잠시 몰렸어요. 조금 기다린 후 다시 시도해 주세요.', HTTP 503은 '도우미가 잠시 쉬고 있어요. 잠시 후 다시 시도해 주세요.'로 구분한다. 프록시가 JSON 없이 해당 상태를 반환해도 기술적인 본문은 표시하지 않는다. 사용자가 서버 주소나 기술적인 오류를 처리할 필요가 없다. 입력한 질문은 실패 후 유지된다.

## 개발용 서버와 자동 연결 검증

기존 Python FastAPI와 GeminiAdapter를 그대로 사용한다. Python 3.12 이상을 준비하고 저장소 루트에서 실행한다.

```powershell
python -m venv .local/chat-venv
.\.local\chat-venv\Scripts\python.exe -m pip install -e './SilverRingo_Chat-bot[api,test]'
$env:SR_D_LLM_MODE = 'fake'
.\.local\chat-venv\Scripts\python.exe -m uvicorn silver_ringo_d.api:app --host 127.0.0.1 --port 8000
```

다른 터미널에서 개발 APK를 빌드한다. 표준 에뮬레이터에서 10.0.2.2는 PC loopback을 가리킨다. 실제 운영 주소 예시가 아니다.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:ANDROID_HOME = 'C:\Users\Home\AppData\Local\Android\Sdk'
.\android\gradlew.bat -p android :app:assembleDebug -PCHAT_DEV_BASE_URL=http://10.0.2.2:8000
```

USB 기기에서 `adb reverse tcp:8000 tcp:8000`을 사용한다면 CHAT_DEV_BASE_URL을 `http://127.0.0.1:8000`으로 빌드한다. reverse 없이 스마트폰의 localhost를 PC 서버로 간주하면 안 된다. 같은 Wi-Fi 기기는 PC LAN 주소와 서버 바인딩·방화벽을 따로 확인한다. 개발 APK는 지정 주소를 포함하므로 일반 검수용 기본 APK와 분리한다.

현재는 추가 property 인자 없이 빌드하면 DHub에 연결된다. 준비 중 상태를 확인하려면 해당 property를 명시적으로 빈 값으로 빌드한다(예: -PCHAT_DEV_BASE_URL=). 기존 미설정 처리는 유지한다. 이번 Debug 검수판도 실제 DHub 주소를 사용한다.

## 실제 서버의 Gemini 설정

확인한 호출 경로는 api.build_service → config.load_settings → create_llm → GeminiAdapter.generate다. 실제 SDK 호출은 기존 gemini_adapter.py의 client.models.generate_content다.

기존 config.py는 os.environ을 읽는다. uvicorn을 시작한 셸이나 서비스/컨테이너에서 전달한 환경변수가 Python 자식 프로세스에 상속된다. fake 모드와 SR_D_TOP_K를 지정해 자식 프로세스의 load_settings가 실제로 해당 값을 읽는지 확인했고, 실행한 서버의 HTTP 응답도 확인했다. 따라서 새 환경변수 로더를 추가하지 않았다.

.env 파일은 현재 기본 API 실행 경로에서 자동으로 읽지 않는다. 기존 load_local_settings(path) 함수는 별도 호출용이며 api.build_service에 연결되어 있지 않다. .env를 만들어 두기만 하면 적용된다고 가정하지 않는다. 이번 실행에는 --env-file이나 dotenv 로더를 추가하지 않았다.

실제 Gemini 모드에는 서버 측에서 다음 환경을 제공한다.

- SR_D_LLM_MODE=gemini
- GEMINI_MODEL: 챗봇 담당자와 합의한 실제 모델 ID
- GEMINI_API_KEY: 서버 운영 환경의 비밀값 주입 기능으로 전달
- 선택 사항: SR_D_TOP_K, SR_D_KNOWLEDGE_DIR

서버 가상환경에 `pip install -e './SilverRingo_Chat-bot[api,gemini]'`를 실행한다. PowerShell이라면 환경변수를 설정한 같은 터미널에서 uvicorn을 실행한다. 키를 명령 기록·Git·APK·로그에 적지 말고 운영 환경의 secret 주입을 사용한다. 서비스 관리자로 시작할 때는 그 서비스/컨테이너의 실행 환경에 지정해야 하며, 다른 터미널에만 설정한 값은 전달되지 않는다. 환경을 변경한 후 서버 worker를 재시작한다. 모델·키·모드 변경에는 Android 재빌드가 필요 없다.

fake는 다른 로컬 LLM이 아니라 모의 응답이다. 이번 검증에서 실제 Gemini 키나 모델을 사용하지 않았다.

## 교수님 서버 운영 전에 필요한 값

1. HTTPS 기본 URL과 TLS 인증서, 실제 API 경로/prefix.
2. 기존 FastAPI 실행 환경, Python 3.12+, 런타임 지식 JSON 배치 및 필요한 extras 설치.
3. 서버에 전달할 Gemini 모델·키·gemini 모드 환경변수.
4. 외부 앱에서 접근할 수 있는 포트·방화벽·프록시·요청 시간 제한. 저장소의 Gemini 어댑터는 시도당 기본 15초/최대 3회이며 Android 연결 5초/읽기 50초를 사용한다.
5. 별도 사용자 인증·게이트웨이가 필요하다면 계약 합의. 현재 코드에는 앱 인증 토큰 프로토콜이 없으므로 임의로 키를 추가하지 않았다.

현재 CHAT_PROD_BASE_URL에는 제공된 DHub HTTPS 기본 URL을 설정했다. 별도 주소로 빌드할 경우 다음처럼 덮어쓸 수 있다. CLI 예시는 다음과 같으며, 입력받는 값은 공개 기본 URL이다.

```powershell
$env:ORG_GRADLE_PROJECT_CHAT_PROD_BASE_URL = Read-Host '교수님이 제공한 HTTPS 기본 URL'
.\android\gradlew.bat -p android :app:assembleRelease
```

Gradle은 위 ORG_GRADLE_PROJECT 환경변수를 해당 project property로 전달한다. 직접 `-PCHAT_PROD_BASE_URL=...`을 사용해도 된다. 운영 주소 자체가 변경되면 앱 재빌드가 필요하다. 서버를 비활성화할 빌드는 property를 명시적으로 빈 값으로 설정한다. release 서명·배포 방식은 별도 준비가 필요하다.

POST /v1/chat의 message/history 요청 및 reply/sources/suggested_actions/request_id 응답은 변경하지 않았다. Gemini 실행·검색·오류 계약은 기존 Python 모듈 책임이며 앱에는 API 키·모델 설정 UI가 없다.

## 2026-10-30 검증 기록 (서버 미설정 당시)

- debug/release APK 빌드, Kotlin 단위 테스트 5개, lint 성공.
- API37 기기 테스트 4개 통과: 서비스 준비 중/설정 UI 부재, 연결 실패·질문 유지·재시도, 기존 가이드/학습 왕복, 실제 학습 WebView 완료·재생성 후 진도 보존.
- 개발 APK가 사전 주입된 주소로 실제 FastAPI 응답을 표시함을 확인. 앱에서 주소 입력·저장 작업을 하지 않았다.
- 한국어 카드 결제 질문으로 기존 HTTP 응답과 course:kiosk:level:5 근거·모의 응답을 확인.
- release HTTP property는 빌드에서 거절됨을 확인. 개발 URL을 넣어 debug와 release를 같이 빌드했을 때 release의 CHAT_BASE_URL은 빈 값으로 유지됨을 확인.
- Team Sync workflow 10/10 통과. Python LLM 설정/어댑터, 가이드·학습 소스, HTTP 계약을 이번 작업에서 수정하지 않았다.
- 실제 교수님 HTTPS 서버·Gemini 실호출·실기기 TTS는 미검증.

로그는 .local/chat-config-build.log, chat-config-device-tests.log, chat-dev-build.log, chat-release-http-rejection.log, chat-config-team-sync.log, chat-config-http-response.json에 있다. 빌드 산출물과 테스트용 주소가 포함된 APK는 Git에서 제외한다. 위 내용은 로컬 검증 기록이며 현재 main 반영은 RELEASE_1_1_1.md를 따른다.

## DHub 승인 전 계획 (2026-10-30 과거 기록)

당시에는 계정 승인 대기 상태였다. 2026-10-31 사용자 제공 정보로 VM/systemd/Gemini 배포 완료를 전달받았다. 이번 Android 작업에서는 VM에 접속하거나 비공개 설정·API 키를 읽지 않는다.

승인 후 VM/OS·접속 권한을 확인하고 Python 3.12 가상환경과 기존 api/gemini 의존성을 설치한다. 지식 JSON을 배치하고 서비스 관리자의 환경변수로 Gemini 키·모델·gemini 모드를 전달한다. FastAPI를 서비스로 등록한 뒤 HTTPS 프록시·인증서·방화벽·외부 접근 정책을 설정한다. /health와 POST /v1/chat을 실제 스마트폰에서 검사한 후 확정된 HTTPS 기본 URL로 release 앱을 다시 빌드·서명한다. 키는 서버에만 두며 .env를 저장소에 올리지 않는다. 인증/이용 제한 정책과 운영 담당자도 배포 전에 합의해야 한다.

## 2026-10-31 DHub 연결 검증

- 현재 브랜치 main, 시작 HEAD e5fe417, 시작 작업 폴더 깨끗함. 원격과 같다고 가정하거나 fetch/merge하지 않았고 기존 로컬 코드를 기준으로 작업했다.
- 외부 HTTPS GET /health: HTTP 200, status=ok, knowledge_loaded=true.
- 외부 HTTPS POST /v1/chat: HTTP 200. 카드 결제 질문 1회, history=[]로 실행했다. reply/sources/suggested_actions/request_id 계약에 맞고 카드 결제 안내와 학습 근거를 받았다. 모의 응답 표시는 없었다. 서버가 실제 Gemini 모드라는 점은 사용자 제공 운영 정보와 일치하며, 비공개 설정을 열어 확인하지 않았다.
- 시스템 기본 인증서 검증을 사용했으며 HTTPS 오류나 다른 URL로의 이동 없이 응답했다. 인증서 검증을 끄거나 별도 신뢰 설정을 추가하지 않았다.
- 반복 검사는 실제 Gemini를 호출하지 않는 로컬 모의 HTTP와 기존 테스트로 진행한다. HTTP 429/503 구분 안내·질문 유지·재시도를 검사한다. 서버 요청 제한을 일부러 소진하지 않는다.
- 서버 내부 Python 로직·키·모델·환경 설정과 기존 학습/가이드 기능은 변경하지 않는다. Commit/Push/main 병합도 하지 않는다.
- 스마트폰 설치, 기기 네트워크에서 실제 답변, Wi-Fi/모바일 데이터, 대화 후속 문맥, 비행기 모드/복구·재시도는 사용자가 별도 확인해야 한다. 에뮬레이터 검사는 실기기 검증을 대신하지 않는다.

### Android 빌드·검사 결과

- Debug APK 빌드와 lint 성공(오류 0, 기존 경고 51). 버전 1.1.1/code 3 유지.
- Kotlin 6/6 통과. 정상 HTTP·최근 대화 6개·잘못된 JSON·타임아웃·연결 실패·429/503의 비JSON 프록시 오류까지 로컬 모의 서버로 확인했다.
- API37 에뮬레이터 5/5 통과. 429/503 안내·재시도, 연결 실패·질문 유지, 미설정 안내, 가이드/학습 복귀, WebView 결과/진도 복원을 확인했다.
- 에뮬레이터에서 생성 APK 설치 후 홈→챗봇 화면 진입과 서버 주소 UI 부재를 추가 확인했다. 실제 DHub 질문을 앱에서 추가 전송하지 않았다. 스마트폰 설치·응답 확인은 미완료다.
- Team Sync workflow 10/10 통과. local 25개 중 23개 통과와 기존 Example-01 의도적 실패 2개. 검사 삭제/비활성화는 없다. GitHub Actions를 새로 실행하거나 원격에 반영하지 않았다.
- 생성된 debug 및 release BuildConfig의 CHAT_BASE_URL이 제공된 DHub 기본 URL과 일치함을 확인했다. release APK는 이번에 새로 빌드하지 않았다.
- APK: `.local/artifacts/SilverRingo-1.1.1-DHub-debug.apk` (저장소 루트 기준, Git 제외). 서명 검증 성공.
- SHA256: `FDCE860F532E4B75CC24CAB70622C288CF18150A6F09385EE7577B81F6211288`.
- 빌드/검사 로그는 .local/dhub-build.log, dhub-device-tests.log, dhub-team-local.log, dhub-team-workflow.log. API 키나 서버 비공개 환경 파일은 읽지 않았다.

## 팀 공용 main의 요청 제한 코드

VM의 기존 api.py를 읽기 전용으로 가져와 로컬에 그대로 반영했다. IP별/전체 SlidingWindowRateLimiter와 HTTP 429가 포함된다. 설정 이름은 SR_D_RATE_LIMIT_PER_MINUTE, SR_D_RATE_LIMIT_WINDOW_SECONDS, SR_D_GLOBAL_RATE_LIMIT_PER_MINUTE이며 기본값은 코드 기준 30/60초/180이다. 운영 환경의 실제 설정값은 읽지 않았다. /health는 이 제한의 대상이 아니다. 현재 제한 상태는 프로세스 메모리에 있으며 다중 worker/인스턴스 전체를 공유하는 저장소는 없다.

기존 VM과 동일한 api.py를 Git에 포함하므로 후속 배포에서 요청 제한을 잃지 않는다. 이번 main 반영 작업은 VM 파일과 systemd/환경을 변경하지 않고 서비스를 재시작하지 않았다. 개발자는 외부 모델 없이 Fake 모드로 테스트하고 실제 서비스 키는 각 서버 환경에서만 설정한다. 최신 실행 결과는 MAIN_CHAT_UPDATE.md를 따른다.
