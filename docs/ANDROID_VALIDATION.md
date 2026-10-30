> 현재 팀 기준: [1.1.1 main 반영 기록](RELEASE_1_1_1.md). 아래 날짜별 내용은 과거 작업 당시 기록도 포함한다.

# 챗봇 연결 설정 수정 검증 — 2026-10-30

현재 검수판은 1.1.1/code3이며 사용자 주소 설정 화면이 없다. 아래 1.1.0의 주소 입력 검증은 과거 기록이다. [새 연결·운영 기준](CHATBOT_DEPLOYMENT.md)을 따른다.

| 검사 | 결과 |
|---|---|
| 기본 debug/release 빌드 | 성공. 두 CHAT_BASE_URL이 모두 빈 문자열. 임의 운영 주소 없음 |
| Kotlin 단위 테스트 | 기존 HTTP 클라이언트 4개 + 학습 완료 판정 1개 통과 |
| lint | 오류 0 / 기존 경고 51 |
| API37 기기 테스트 | 4개 통과. 미설정 준비 중/설정 UI 없음, 실패 시 질문·재시도, 기존 가이드/학습 왕복, 실제 WebView 완료·진도 보존 |
| 개발 자동 연결 | CHAT_DEV_BASE_URL=http://10.0.2.2:8000으로 빌드한 앱에서 주소 입력 없이 기존 FastAPI 응답 표시 확인 |
| 개발·배포 분리 | 개발 URL을 지정해 두 빌드를 만들었을 때 release CHAT_BASE_URL은 빈 값 유지 |
| release HTTPS | HTTP 운영 주소를 지정한 빌드가 명시적으로 거절됨. 의도한 보안 검사이며 앱 빌드 오류가 아님 |
| Python 환경 전달 | 자식 Python 프로세스에서 기본값과 다른 SR_D_TOP_K=3을 읽는 것과 기존 서버의 한국어 fake HTTP 응답을 확인. 새 로더 없음 |
| Team Sync workflow | 기존 계약·지식·학습 검사 포함 10/10 통과 |
| 최종 기본 APK 실제 실행 | 챗봇 서비스 준비 중과 주소 UI 부재 확인, 전송 비활성화. API 키·모델은 앱에 없음 |

일반 검수용 APK: `.local/artifacts/SilverRingo-v2-1.1.1-debug.apk` (17,668,021 bytes).
SHA-256: `8CC8A2EC0669B927C070947C7D1673953F0DF84FC9F8ABF4EC8A84D1461E1CD7`. APK 서명 검증 성공.
별도 개발용 APK: `.local/artifacts/SilverRingo-v2-1.1.1-dev-chat-debug.apk` — 에뮬레이터 개발 주소 포함, 실제 서비스 배포용 아님.

기본 `android/app/build/outputs/apk/debug/app-debug.apk`도 미설정 상태로 다시 빌드했다. 교수님 HTTPS URL을 받으면 개발자가 CHAT_PROD_BASE_URL로 release를 재빌드한다. 테스트 서버는 검증 후 종료했다. 실제 운영 HTTPS·Gemini 키/모델 실호출은 미검증이다. 이번 작업은 기존 로컬 변경을 보존했으며 Commit·Push·main 병합을 하지 않았다.

로그: `.local/chat-config-build.log`, `chat-config-device-tests.log`, `chat-dev-build.log`, `chat-default-final-build.log`, `chat-release-http-rejection.log`, `chat-config-team-sync.log`, `chat-config-http-response.json`. 화면 캡처는 `.local/artifacts/chat-auto-connect.png`, `chat-preparing.png`다. 기기 앱 업데이트 전환 중 첫 UI 덤프가 일시 실패했으나 재실행하여 위 최종 화면을 확인했다.

---

# 2차 통합 최종 검증 — 2026-10-29

로컬 `hjh-integration-v2`, 기준 main `2e8f75e517b09770f1629ce04bc579f691863cc0`.
버전 1.1.0 (code 2), 단일 applicationId `com.silverlingo.app`.
이번에는 커밋·Push·main 병합을 하지 않았다. 아래 1차 검수 기록은 과거 결과다.

## 실제 실행 결과

| 검사 | 결과 |
|---|---|
| 수정 전 main APK·기존 Kotlin 검사 | 성공. 이 PC의 JDK17·SDK35/Build Tools34 사용 |
| 최종 APK / Kotlin / lint | 성공. 단위 테스트 5개(HTTP 4 + 기존 학습 완료 판정 1), 실패 0. lint 오류 0 / 경고 51 |
| API37 에뮬레이터 기기 검사 | 3개 모두 통과: 기존 홈·가이드·학습 왕복, 실제 WebView 완료/Activity 재생성 후 진도 유지, 신규 챗봇 연결 실패/질문 유지/홈 복귀 |
| 최초 기기 검사 | 구형 Espresso의 InputManager.getInstance 호환성 오류로 3개 중단. 테스트 의존성 수정 후 최종 재실행은 성공 |
| 학습 JavaScript 엔진 | 기존 11개 통과(Team Sync가 실제 Node 테스트를 호출) |
| 기존 Chrome 브라우저 회귀 | 6단계, 진도, 가격·옵션·수량·수정·오답, 작은 화면·확대 글꼴, embedded 메시지, 저장 차단, 단일 HTML 전체 통과 |
| Team Sync workflow 모드 | 10/10 통과. 기존 8개와 새 FastAPI HTTP 검사 2개 |
| Team Sync local 모드 | 25개 중 23개 통과. 기존 Example-01 의도된 실패 test_progress_fail_demo / test_remaining_fail_demo 2개만 실패. 실패를 숨기거나 예제를 변경하지 않음 |
| 실제 앱 → 서버 HTTP | 에뮬레이터에서 주소 저장 → 질문 전송 → 기존 FastAPI 답변 표시 확인. 로컬 fake 모드이며 원격 Gemini 성공이 아님 |
| 한국어 HTTP 응답 | 실제 로컬 API에 카드 결제 질문 전송, course:kiosk:level:5 근거와 [모의 응답] 확인 |
| APK 무결성 | debug APK v2 서명 검증 통과, 학습 자산 11개 모두 원본과 바이트 일치 |
| 원본 보존 | 가져온 서비스·XML은 최신 Juno blob과 일치. 기존 학습 Kotlin·완료 테스트·IntegratedAppTest·웹 원본은 main과 차이 없음 |

검수 APK: `.local/artifacts/SilverRingo-v2-1.1.0-debug.apk` (17,672,362 bytes).
SHA-256: `F34D82A202490A011BF17D7658EA800DD0C53C5DE767268F31BFCEFF87CFF9DE`.
기본 빌드 출력은 `android/app/build/outputs/apk/debug/app-debug.apk`다.
두 APK는 같은 파일 내용이며 Git에서 제외된다. 실제 기기에는 검수 APK를 직접 전달한다.

## 이 데스크톱에서 재실행

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:ANDROID_HOME = 'C:\Users\Home\AppData\Local\Android\Sdk'
.\android\gradlew.bat -p android :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest
.\.local\chat-venv\Scripts\python.exe Tester/automation/check.py --mode workflow
.\.local\chat-venv\Scripts\python.exe Tester/automation/check.py --mode local
```

브라우저는 `silver-ringo-c`에서 `npm ci --ignore-scripts`, Chrome 경로를 SR_BROWSER_PATH로 지정한 뒤 `npm run test:browser`를 실행한다. Python은 3.12.14 로컬 가상환경, Node 24.15.0을 사용했다. API 테스트 환경은 FastAPI 0.162.0, uvicorn 0.54.0, httpx 0.28.1이다. API extra 설치는 [2차 통합 기준](ANDROID_INTEGRATION_V2.md)을 따른다.

실행 로그는 이 PC의 `.local/`에 있다: `baseline-build.log`, `final-verification.log`, `device-tests.log`(최초 오류), `device-tests-updated.log`, `browser-test.log`, `team-sync-workflow.log`, `team-sync-local.log`, 각 `*-results.json`, `live-chat-response.json`.
Android JUnit XML·HTML은 `android/app/build/`에 있으며 모두 로컬 산출물이다. 이번 결과를 이전 Actions·Discord 실행 성공으로 표시하지 않는다.

## 스마트폰 설치 전/설치 후 남은 확인

1. 기존 Juno 설치본과 debug 서명이 다르면 업데이트 설치가 거절될 수 있다. 사용자 데이터/기존 앱을 자동 삭제하지 않았다. 이전 서명 또는 별도 테스트 기기로 검수한다.
2. 교수님이 서버 제공 예정이라고 했으며 AWS는 예시로만 언급됐다. 아직 운영 URL·배포 방식·인증·모델·키가 없다. 앱 설정에 최종 HTTPS 기본 URL을 입력하고 실제 인증/인증서/방화벽/시간 제한을 검수해야 한다.
3. 실제 서버가 없을 때 홈·학습은 사용 가능하고, 가이드는 외부 서비스의 자체 네트워크 요구를 따른다. 챗봇은 오류와 재시도를 표시한다. 음성 입력은 기존 준비 중 안내 상태다.
4. 실제 ARM64 기기에서 똑닥 로그인/PASS/회원가입 화면 오버레이 차단, 스크롤 후 위치 추적, 후기 8초 전환, 접수/예약 안내, 버블 종료·재시작을 확인한다. 자동 테스트에서 실제 병원 접수는 실행하지 않았다.
5. 한국어 TTS 설치/미설치, 접근성·오버레이 권한 거부/복귀, 큰 글꼴·TalkBack·태블릿·가로 화면은 실기기 추가 검수가 필요하다.
6. Gemini 실호출과 앱-운영서버 HTTPS 왕복은 미검증이다. fake 테스트 통과가 모델 품질·운영 배포 성공을 뜻하지 않는다.

---

# 통합 APK 검수

검수일: 2026-10-28. 통합 앱은 `android/`, 개발 브랜치는 `hjh`, 승인된 팀 공용 반영 브랜치는 `main`이다.
원본 브랜치에 있던 과거 로그/APK는 이번 결과에 포함하지 않는다.

## main 반영 전 재검증 및 사용자 확인

사용자가 APK 설치·앱 실행 성공을 확인했다. 일부 가이드 버그는 원인 미확정이며
접근성·오버레이·TTS와 외부 앱/웹의 전체 호환성은 검증 완료로 간주하지 않는다.
이번 main 반영 전 APK/Kotlin/기기 테스트 APK를 --rerun-tasks로 재검증해 성공했다.
JS 엔진 11개와 브라우저 회귀 검사도 재실행해 전부 통과했다.
Team Sync local은 9개 중 7개 통과·기존 의도된 실패 2개, workflow 모드는 7개 전부 통과했다.
운영 분석·Actions/Discord 실제 결과는 [TEAM_SYNC_AUDIT.md](TEAM_SYNC_AUDIT.md),
개발자별 변경은 [ANDROID_FILE_CHANGES.md](ANDROID_FILE_CHANGES.md)를 확인한다.

main 병합 SHA `4acba96266fb75e6df4561658558eb96bddf8ed5` Push 완료.
[실제 Team Sync](https://github.com/KeilyAE3/SilverRingo_Digital_Literacy_Assistant/actions/runs/40649420675)는 7/7 통과,
[Discord 알림](https://github.com/KeilyAE3/SilverRingo_Digital_Literacy_Assistant/actions/runs/40649442529)은 메시지 생성 응답까지 확인했다.
다운로드한 artifact SHA도 일치한다. Android 앱 전체의 호환성이나 가이드 버그 해결을 의미하지 않는다.

## 자동 검증

| 검사 | 결과 |
| --- | --- |
| `node --test silver-ringo-c/tests/engine.test.cjs` | 11개 통과, 실패 0 |
| `silver-ringo-c`에서 `node tests/browser.cjs` | 전체 통과: 6단계, 진도 유지, 가격·수량·주문 수정·오답, 390/320px 및 큰 글자, embedded result/exit 브리지, 저장 차단 대응, 단일 HTML 데모 |
| `:app:assembleDebug` | 성공. 최종 소스 재빌드 `BUILD SUCCESSFUL` |
| `:app:testDebugUnitTest` | Kotlin 테스트 1개 통과 (학습/실전 승인, 다른 단계·실패·자유 실습·필드 누락·잘못된 JSON 거절), 실패/오류 0 |
| `:app:assembleDebugAndroidTest` | 성공. 통합 UI 테스트 APK 컴파일·패키징 완료 |
| `:app:connectedDebugAndroidTest` | 실행 시도했으나 `No connected devices!`로 실행 불가. 테스트 코드의 실패 결과가 아님 |
| 기기 UI·서비스·TTS 실제 실행 | 미검증: 연결된 기기 및 등록된 AVD 없음 |
| APK 자산·서명 | 웹 자산 11개 모두 소스와 바이트 단위 일치, APK v2 서명 검증 성공 |
| 콘텐츠 동기화 | content.js와 content.json 내용 일치 |

최종 APK 크기: 17,549,540 bytes (약 16.7 MiB).
main 반영 전 재빌드 SHA-256: `D7141338BECCD58AB783AD1491C36B8795EFAFD84D9497E6F77857E3DDE5D4D0`.

실행 로그: [Android 빌드](validation/android-build.log), [Kotlin XML 결과](../android/app/build/test-results/testDebugUnitTest/TEST-kr.silverringo.learning.LearningResultTest.xml),
[학습 엔진](validation/engine-test.log), [브라우저](validation/browser-test.log), [기기 테스트 시도](validation/device-test.log).
Android XML/HTML 보고서는 빌드 결과물이므로 Git에서 제외된다.

브라우저의 브리지 검증은 네이티브 객체를 모킹한다. 실제 Android WebView·TTS 성공을 의미하지 않는다.
브라우저가 생성한 모바일 화면을 육안으로도 확인했다.
접근성 서비스·플로팅 서비스·오버레이 스크립트·가이드 대시보드·동행 Activity·프로필 저장소는
`origin/Juno`의 Git blob과 해시가 동일함을 확인했다. 런타임 검증을 대체하지는 않는다.

## 이 노트북에서 빌드하기

PowerShell에서 저장소 루트로 이동한다. JDK와 SDK는 로컬 설치를 사용하며 Git에 포함하지 않는다.
기본 JDK 24 대신 다음 JDK 17을 지정한다. `android/local.properties`에는 로컬 SDK 경로를 기록했다.

```powershell
cd C:\study\swproj\SilverRingo_Digital_Literacy_Assistant
$env:JAVA_HOME = 'C:\Program Files\Unity\Hub\Editor\6000.3.15f1\Editor\Data\PlaybackEngines\AndroidPlayer\OpenJDK'
$env:ANDROID_HOME = "$PWD\.local\android-sdk"
$env:GRADLE_USER_HOME = "$env:USERPROFILE\.gradle"
cd android
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest --console=plain
```

Android Studio에서는 **저장소의 `android/`** 를 열고 Gradle JDK를 17로 지정한다.
별도의 프로젝트를 생성하거나 `silverRingo/`의 SDK/JDK 설정을 적용하지 않는다.
다른 개발자의 노트북은 자기 SDK 경로로 `android/local.properties`를 작성해야 한다.

웹 검사:

```powershell
cd C:\study\swproj\SilverRingo_Digital_Literacy_Assistant\silver-ringo-c
npm ci --ignore-scripts
npm test
$env:SR_BROWSER_PATH = 'C:\Program Files\Google\Chrome\Application\chrome.exe'
npm run test:browser
```

이 노트북에서는 기존 Chrome을 사용했다. Chrome이 없으면 Playwright Chromium을 설치하거나
`SR_BROWSER_PATH`를 설치된 호환 브라우저 경로로 바꾼다. Playwright는 개발 테스트 의존성이다.

## APK 설치·사용자 직접 검사

APK: `android/app/build/outputs/apk/debug/app-debug.apk` (개발용 debug 서명).
applicationId는 기존 Juno와 같은 `com.silverlingo.app`다.
기존 설치본과 서명이 다르면 업데이트 설치가 거절된다. 기존 앱/데이터를 자동 삭제하지 않는다.
이 경우 별도 테스트 기기/에뮬레이터를 사용하거나 기존 서명키를 확인한다.

1. USB 디버깅을 켠 Android 8.0 이상 기기를 연결하고 PC 연결 허용을 누른다.
2. 루트에서 다음 명령으로 기기 인식과 설치를 확인한다.

```powershell
& .\.local\android-sdk\platform-tools\adb.exe devices
& .\.local\android-sdk\platform-tools\adb.exe install -r .\android\app\build\outputs\apk\debug\app-debug.apk
& .\.local\android-sdk\platform-tools\adb.exe shell am start -n com.silverlingo.app/.MainActivity
```

3. 홈 → IT 기능 배우기 → 1단계. 주문 시작 → 매장에서 먹기 → 통과 결과 확인.
4. 학습 목록으로 복귀해 1단계 완료/2단계 열림을 확인한다. 홈으로 나간 뒤 다시 들어와도 유지되어야 한다.
5. 앱을 닫았다 다시 실행하고 진도가 유지되는지 확인한다. 2~6단계도 완료해 순차 잠금을 검사한다.
6. 실습에서 시스템 뒤로가기 → 목록, 목록에서 뒤로가기 → 홈. 학습 나가기 버튼도 검사한다.
7. 도움말의 음성 재생/자동 음성을 켜 한국어 TTS를 확인한다. 다른 화면으로 이동한 후 음성이 멈춰야 한다.
   한국어 TTS가 없으면 기기 TTS 설정에서 한국어 데이터를 설치하고 재시도한다.
8. 홈 → 생활 가이드. 내 정보 대화상자, 권한 안내, 똑닥 실행 선택 대화상자를 확인한다.
9. 권한 없이 웹 동행을 열어 페이지·오버레이·음성 안내를 확인한다. 웹 화면 뒤로가기 → 가이드 → 홈.
10. 실제 ARM64 기기에서는 다른 앱 위에 표시 및 접근성의 실버링고 서비스를 허용한다.
    Android 13 이상 제한된 설정이 있으면 앱 정보에서 해당 설정을 허용한다.
    똑닥 공식 앱을 열어 플로팅 버블·단계별 노란 링·읽어주기·위치 비키기·접기·종료를 검사한다.
    실제 로그인/접수는 사용자 본인이 진행하며 자동 검증에서 실제 접수를 실행하지 않는다.
11. 홈 복귀 시 플로팅 버블이 종료되는지, 다시 가이드에 진입해 재시작되는지 확인한다.
12. 회전·큰 글자·가로 화면에서 홈/목록/실습/가이드 버튼이 가려지지 않는지 확인한다.

기기 자동 테스트(위 환경변수 지정 후 `android/`에서):

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
```

`IntegratedAppTest`는 홈·가이드 왕복, 학습 왕복, 실제 로컬 WebView의 1단계 완료 메시지와
Activity 재생성 후 완료 유지를 검사한다. 테스트 기기의 1단계 완료 진도를 기록한다.
오버레이 권한 허용·외부 똑닥 앱·한국어 소리 출력은 이 테스트로 자동 확인하지 않는다.

## 알려진 한계

- 기기·에뮬레이터에서 실제 실행하지 못한 항목은 성공으로 보고하지 않는다.
- 한국어 TTS 엔진/데이터, 최신 Android System WebView, 가이드 웹 이용 시 네트워크가 필요하다.
- 실습 중 회전/프로세스 재생성 시 세부 주문은 자동 복원하지 않고 단계 소개부터 다시 시작한다.
- 진도는 해당 기기의 로컬 저장소이며 계정 간/기기 간 동기화는 없다.
- 챗봇·음성 입력·새 가이드 콘텐츠는 이번 범위에 포함하지 않는다.
- 사용자가 설치·실행 확인 후 main Commit·병합·Push를 승인했다. PR·Force Push·보호 우회는 하지 않는다.
- Juno 원본의 VolumeUp 아이콘·WebView databaseEnabled·statusBarColor 사용 중단 예정 경고가 남아 있다.
  빌드는 성공했으며 이번에는 가이드 핵심 코드를 변경하지 않았다.
- 첫 빌드에서 `libandroidx.graphics.path.so` 심볼 제거 도구 경고가 있었고 라이브러리는 그대로 패키징되었다.
  서명 검증은 통과했으며 실제 기기 동작은 별도 검수 항목이다.
