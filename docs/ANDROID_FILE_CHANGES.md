> 현재 팀 기준: [1.1.1 main 반영 기록](RELEASE_1_1_1.md). 아래 날짜별 내용은 과거 작업 당시 기록도 포함한다.

# 챗봇 연결 설정 수정 — 2026-10-30

일반 사용자용 주소 입력·저장 UI를 제거했다. 개발자 빌드 주소 자동 연결, 미설정 준비 중 안내, 쉬운 오류·재시도로 변경했다. 기존 Python 환경변수 로더가 실제 자식 프로세스 환경을 받으므로 새 로더와 API 변경은 추가하지 않았다. [운영 설정·검증](CHATBOT_DEPLOYMENT.md)을 따른다.

| 변경 파일 | 내용 |
|---|---|
| android/app/build.gradle.kts | debug/release 주소 property 분리, 빈 기본값, release HTTPS 검증, 앱 1.1.1/code3 |
| android/.../chat/ChatScreen.kt | 주소 UI·SharedPreferences 읽기 제거, BuildConfig 주소 자동 사용, 준비 중·일반 오류·재시도 |
| android/app/src/androidTest/.../ChatUiTest.kt | 미설정·설정 UI 부재·연결 실패와 재시도 검사. 기존 가이드·학습 테스트는 유지 |
| docs/CHATBOT_DEPLOYMENT.md | 개발·운영 빌드, 서버 환경변수·.env 동작, 교수님 서버 필수 값 |
| docs/ANDROID_INTEGRATION_V2.md, docs/ANDROID_VALIDATION.md | 최신 연결 방식과 검증 결과 반영 |

아래는 이전 통합 작업 기록이다.

---

# 2차 통합 변경과 담당자 검토 — 2026-10-29

핵심 변경은 최신 가이드 버그 수정과 실제 챗봇 HTTP/UI 연결이다. 학습을 새로 작성하거나 세 개의 앱을 묶은 것이 아니다. 기존 main의 단일 앱·화면 복귀·학습 완료 판정을 유지했다. [전체 구조/실행 규격](ANDROID_INTEGRATION_V2.md), [실제 검증](ANDROID_VALIDATION.md)을 먼저 읽는다.

## 개발 담당자별 확인사항

| 담당자 | 이번 변경 및 검토 요청 |
|---|---|
| 가이드 | 최신 Juno의 서비스·XML 2개만 반영했다. 실제 기기 인증/PASS 오인식 차단·스크롤·8초 전환을 확인하고 현재 키워드가 똑닥 버전과 맞는지 검토한다. 대시보드/브라우저/플로팅/홈 복귀는 통합본 유지 |
| 마이크로러닝 | jib 최신 커밋에 추가 업데이트가 없어 웹·콘텐츠·브리지·완료 판정을 보존했다. 기존 실제 WebView 완료/회전 진도 검사가 통과했다. 새 콘텐츠는 기존 sync/engine/browser 절차를 사용 |
| 메인 UI | 홈 입력/메뉴에서 실제 ChatScreen으로 이동한다. 서버 설정, 로딩/실패/재시도, 복귀, 상태 표시줄 대비를 추가했다. 실제 고령층 기기/큰 글자에서 화면 여백과 입력창을 검수한다 |
| 챗봇 | 릴리스가 이미 main에 있어 재복사하지 않았다. HTTP 계약 유지, 과거 홈 지식만 현재 main 연결 상태로 정정했다. 교수님 서버 URL·실행/배포·HTTPS·인증·Gemini 환경변수와 source/액션 정책을 확정해야 한다 |
| 자동 테스트 | 기존 check.py와 Discord 알림을 보존했다. HTTP 2개를 config에 등록하고 기존 workflow에 api/test extra 설치를 추가했다. Android·브라우저 실행은 현재 로컬 검증이며 CI 자동 수행 범위가 아님. 의도된 local 실패 2개 정책 검토 필요 |

## 실제 변경 파일

| 파일 (저장소 기준) | 변경 |
|---|---|
| android/app/src/main/java/com/silverlingo/app/service/SilverAccessibilityService.kt | 최신 Juno의 스크롤 추적·인증 화면 차단·잔류 말풍선 정리 |
| android/app/src/main/res/xml/accessibility_service_config.xml | 스크롤 이벤트·50ms notificationTimeout |
| android/app/src/main/java/com/silverlingo/app/chat/ChatClient.kt | 표준 HTTP 요청·응답·주소 검증·타임아웃·안전한 오류 |
| android/app/src/main/java/com/silverlingo/app/chat/ChatScreen.kt | 주소 설정·채팅·최근 문맥·근거 상태·오류/재시도·복귀 |
| android/app/src/main/java/com/silverlingo/app/MainActivity.kt | 기존 목적지 유지, chat 분기·서비스 종료·밝은 화면의 상태 표시줄 대비 |
| android/app/src/main/java/com/silverringo/app/HomeScreen.kt | 실제 챗봇 콜백과 메뉴/진입 버튼. 음성 입력 구현은 추가하지 않음 |
| android/app/src/debug/AndroidManifest.xml | debug 로컬 HTTP 허용, release 설정은 유지 |
| android/app/build.gradle.kts | 앱 1.1.0/code2, BuildConfig 생성, 테스트 라이브러리 호환성 수정. 런타임 스택·라이브러리는 유지 |
| android/app/src/test/java/com/silverlingo/app/chat/ChatClientTest.kt | 실제 로컬 HTTP 교환, 최근 6개, JSON 오류, 서버 실패·타임아웃·주소 보안 검사 |
| android/app/src/androidTest/java/com/silverlingo/app/ChatUiTest.kt | 연결 실패 후 질문·재시도 유지와 시스템 뒤로가기 |
| Tester/tests/test_d_http.py | 기존 FastAPI 라우트의 정상/입력 오류/LLM 실패 검사 |
| Tester/config.json | 기존 등록 그대로 + HTTP 검사 묶음 추가 |
| .github/workflows/team-sync.yml | 기존 실행/브랜치/알림 흐름 유지, HTTP 테스트 의존성 설치만 추가 |
| SilverRingo_Chat-bot/pyproject.toml | test extra에 HTTP 테스트 도구 추가 |
| SilverRingo_Chat-bot/knowledge/app_info.json | 학습·가이드의 오래된 미통합 설명과 채팅 통신 설명 정정 |
| SilverRingo_Chat-bot/.gitignore | 로컬 pip 설치의 egg-info 제외 |
| README.md, docs/ANDROID_INTEGRATION*.md, docs/ANDROID_VALIDATION.md, docs/ANDROID_FILE_CHANGES.md, docs/TEAM_SYNC_AUDIT.md | 현재 기준·실행/기기 주소·결과·담당자 확인. 과거 기록은 아래 보존 |

테스트 의존성 수정 근거: [AndroidX Test 공식 릴리스](https://developer.android.com/jetpack/androidx/releases/test)의 Espresso 3.7.0에는 반사 InputManager.getInstance 대신 getSystemService를 사용하는 수정이 포함된다. 실제 이 PC의 API37 에뮬레이터에서 수정 전 오류와 수정 후 3개 통과를 모두 확인했다. Kotlin/Compose/SDK 업그레이드로 해결한 것이 아니다.

---

# 개발자별 통합 변경 및 파일 목록

기준 main: `4a3b52ee2a6ab85df99d7f4922f2f752b8bab3da`. 목록은 실제 `git diff --cached --name-status 4a3b52ee2a6ab85df99d7f4922f2f752b8bab3da`으로 생성했다.
통합 앱 소스는 main에 없었으므로 대부분 A(신규)다. 원본 대비 수정 여부는 아래의 별도 비교로 구분한다.

## Juno: 가이드 담당

원본 `origin/Juno:android/` → 통합 `android/` (패키지·applicationId·도구 버전 유지).
실제 원본 대비 변경: MainActivity.kt(홈/학습/가이드 라우팅과 홈 복귀 시 플로팅 종료),
app/build.gradle.kts(자산 Sync·학습/테스트 의존성), AndroidManifest.xml(TTS queries), README.md(통합 안내).
접근성·플로팅 서비스·오버레이 스크립트·권한 선언·대시보드·웹 동행·프로필 코드는 원본 유지.
알려진 문제: 사용자 확인 가이드 버그, 원인 미확정. 홈 복귀 때의 서비스 종료는 의도한 통합 변경이므로
가이드 재진입·외부 앱 복귀·권한 재진입·웹 종료·한국어 TTS·접근성 링/말풍선·위치 비키기를 다시 검사한다.
원본 Juno APK와 같은 기기·앱/웹 버전에서 재현 순서 및 logcat을 비교해 회귀인지 판단한다.

## jib: 마이크로러닝 담당

원본 `origin/jib:silver-ringo-c/android/app/src/main/java/kr/silverringo/learning/`의 LearningEntry.kt와
KioskPracticeScreen.kt → `android/app/src/main/java/kr/silverringo/learning/`.
LearningEntry는 결과 승인 조건을 새 LearningResult.kt의 completedLesson으로 분리했다. WebView 호스트는 원본 유지.
`silver-ringo-c/web/`, scripts/, tests/는 원본 경로 유지. engine.js/app.js/콘텐츠/이미지는 원본과 같다.
원본 독립 Android 호스트는 main 대상에서 제외해 두 번째 앱을 만들지 않는다.
`LearningEntry(onExit, onResult)` / `KioskPracticeScreen(levelId, onExit, onResult)`와 origin 제한 JSON 브리지를 유지한다.
새 콘텐츠는 content.js → npm run sync → content.json → 기존 엔진/브라우저 테스트 → android APK 순서로 반영한다.
생성된 assets/kiosk를 직접 편집하지 않는다. 새 topic은 진도 키 분리 필요. 자세한 계약은 ANDROID_INTEGRATION.md 참조.

## hjh: 메인 UI 담당

원본 `origin/hjh:silverRingo/app/src/main/java/com/silverringo/app/`의 HomeScreen.kt·ui/theme/ →
`android/app/src/main/java/com/silverringo/app/`. HomeScreen의 기능 없는 바로가기 예정 영역만 제거했다.
원본 임시 목적지를 복사하지 않고 Juno MainActivity의 SilverRingoApp에서 실제 학습/가이드를 호출한다.
새 기능은 HomeScreen 콜백 → 루트 목적지 → 기능 화면 onExit로 연결한다. 뒤로가기는 기능 내부에서 목록, 다음은 홈.
원본 silverRingo/는 기존 hjh 커밋에서 main에 들어오는 참고 프로젝트로 보존하고 README에 새 진입점을 안내한다.

## 자동 테스트 담당

check.py/notify.py/discord-notify.yml/기존 예제는 변경하지 않았다.
config.json에 실제 엔진/콘텐츠 검사 2개 등록, team-sync.yml에 Node 22 준비 단계만 추가했다.
local 9개: 7 통과·의도된 예제 2 실패. workflow 7개: 전부 통과. JS 하위 회귀 검사는 11개.
Python 등록 테스트가 Kotlin을 검증하지 않는다. Android 빌드·브라우저는 로컬 별도 수행, 기기/오버레이/TTS는 별도 검수.
문서와 코드 차이 및 원격 증거는 TEAM_SYNC_AUDIT.md 참조. Android CI·브라우저 CI·실기기 검사는 향후 합의 필요.

## 원본 비교 (Git blob / 실제 파일 diff)

| 원본 → 통합 경로 | 결과 |
| --- | --- |
| `origin/Juno:android/README.md` → `android/README.md` | [수정 diff](validation/source-diffs/Juno-README.md.diff) |
| `origin/Juno:android/app/build.gradle.kts` → `android/app/build.gradle.kts` | [수정 diff](validation/source-diffs/Juno-build.gradle.kts.diff) |
| `origin/Juno:android/app/src/main/AndroidManifest.xml` → `android/app/src/main/AndroidManifest.xml` | [수정 diff](validation/source-diffs/Juno-AndroidManifest.xml.diff) |
| `origin/Juno:android/app/src/main/java/com/silverlingo/app/MainActivity.kt` → `android/app/src/main/java/com/silverlingo/app/MainActivity.kt` | [수정 diff](validation/source-diffs/Juno-MainActivity.kt.diff) |
| `origin/jib:silver-ringo-c/android/app/src/main/java/kr/silverringo/learning/LearningEntry.kt` → `android/app/src/main/java/kr/silverringo/learning/LearningEntry.kt` | [수정 diff](validation/source-diffs/jib-LearningEntry.kt.diff) |
| `origin/jib:silver-ringo-c/android/app/src/main/java/kr/silverringo/learning/KioskPracticeScreen.kt` → `android/app/src/main/java/kr/silverringo/learning/KioskPracticeScreen.kt` | 동일 |
| `origin/hjh:silverRingo/app/src/main/java/com/silverringo/app/HomeScreen.kt` → `android/app/src/main/java/com/silverringo/app/HomeScreen.kt` | [수정 diff](validation/source-diffs/hjh-HomeScreen.kt.diff) |
| `origin/Juno:android/app/src/main/java/com/silverlingo/app/service/FloatingCompanionService.kt` → `android/app/src/main/java/com/silverlingo/app/service/FloatingCompanionService.kt` | 동일 |
| `origin/Juno:android/app/src/main/java/com/silverlingo/app/service/SilverAccessibilityService.kt` → `android/app/src/main/java/com/silverlingo/app/service/SilverAccessibilityService.kt` | 동일 |
| `origin/Juno:android/app/src/main/java/com/silverlingo/app/service/OverlayScriptProvider.kt` → `android/app/src/main/java/com/silverlingo/app/service/OverlayScriptProvider.kt` | 동일 |
| `origin/Juno:android/app/src/main/java/com/silverlingo/app/service/ServiceLifecycleOwner.kt` → `android/app/src/main/java/com/silverlingo/app/service/ServiceLifecycleOwner.kt` | 동일 |
| `origin/Juno:android/app/src/main/java/com/silverlingo/app/ui/screens/MainDashboardScreen.kt` → `android/app/src/main/java/com/silverlingo/app/ui/screens/MainDashboardScreen.kt` | 동일 |
| `origin/Juno:android/app/src/main/java/com/silverlingo/app/ui/screens/CompanionBrowserActivity.kt` → `android/app/src/main/java/com/silverlingo/app/ui/screens/CompanionBrowserActivity.kt` | 동일 |
| `origin/Juno:android/app/src/main/java/com/silverlingo/app/data/UserProfileManager.kt` → `android/app/src/main/java/com/silverlingo/app/data/UserProfileManager.kt` | 동일 |
| `origin/jib:silver-ringo-c/scripts/build-demo.cjs` → `silver-ringo-c/scripts/build-demo.cjs` | 동일 |
| `origin/jib:silver-ringo-c/scripts/sync-content.cjs` → `silver-ringo-c/scripts/sync-content.cjs` | 동일 |
| `origin/jib:silver-ringo-c/tests/browser.cjs` → `silver-ringo-c/tests/browser.cjs` | 동일 |
| `origin/jib:silver-ringo-c/tests/engine.test.cjs` → `silver-ringo-c/tests/engine.test.cjs` | 동일 |
| `origin/jib:silver-ringo-c/web/app.js` → `silver-ringo-c/web/app.js` | 동일 |
| `origin/jib:silver-ringo-c/web/assets/burger.svg` → `silver-ringo-c/web/assets/burger.svg` | 동일 |
| `origin/jib:silver-ringo-c/web/assets/chicken.svg` → `silver-ringo-c/web/assets/chicken.svg` | 동일 |
| `origin/jib:silver-ringo-c/web/assets/cola.svg` → `silver-ringo-c/web/assets/cola.svg` | 동일 |
| `origin/jib:silver-ringo-c/web/assets/fries.svg` → `silver-ringo-c/web/assets/fries.svg` | 동일 |
| `origin/jib:silver-ringo-c/web/assets/shrimp.svg` → `silver-ringo-c/web/assets/shrimp.svg` | 동일 |
| `origin/jib:silver-ringo-c/web/content.js` → `silver-ringo-c/web/content.js` | 동일 |
| `origin/jib:silver-ringo-c/web/content.json` → `silver-ringo-c/web/content.json` | 동일 |
| `origin/jib:silver-ringo-c/web/engine.js` → `silver-ringo-c/web/engine.js` | 동일 |
| `origin/jib:silver-ringo-c/web/index.html` → `silver-ringo-c/web/index.html` | 동일 |
| `origin/jib:silver-ringo-c/web/styles.css` → `silver-ringo-c/web/styles.css` | 동일 |

## 최신 main 대비 신규·수정 파일 전체

아래 목록에는 이 파일과 첫 Push 후 원격 검증 기록 문서·로그도 포함된다.
현재 생성 시점: {'M': 2, 'A': 131}. 삭제 파일 없음.

| Git 상태 | 파일 |
| --- | --- |
| M | `.github/workflows/team-sync.yml` |
| A | `.gitignore` |
| A | `README.md` |
| M | `Tester/config.json` |
| A | `Tester/tests/test_learning_engine.py` |
| A | `android/README.md` |
| A | `android/app/build.gradle.kts` |
| A | `android/app/proguard-rules.pro` |
| A | `android/app/src/androidTest/java/com/silverlingo/app/IntegratedAppTest.kt` |
| A | `android/app/src/main/AndroidManifest.xml` |
| A | `android/app/src/main/java/com/silverlingo/app/MainActivity.kt` |
| A | `android/app/src/main/java/com/silverlingo/app/data/UserProfileManager.kt` |
| A | `android/app/src/main/java/com/silverlingo/app/service/FloatingCompanionService.kt` |
| A | `android/app/src/main/java/com/silverlingo/app/service/OverlayScriptProvider.kt` |
| A | `android/app/src/main/java/com/silverlingo/app/service/ServiceLifecycleOwner.kt` |
| A | `android/app/src/main/java/com/silverlingo/app/service/SilverAccessibilityService.kt` |
| A | `android/app/src/main/java/com/silverlingo/app/ui/screens/CompanionBrowserActivity.kt` |
| A | `android/app/src/main/java/com/silverlingo/app/ui/screens/MainDashboardScreen.kt` |
| A | `android/app/src/main/java/com/silverlingo/app/ui/screens/UserProfileDialog.kt` |
| A | `android/app/src/main/java/com/silverlingo/app/ui/theme/Color.kt` |
| A | `android/app/src/main/java/com/silverlingo/app/ui/theme/Theme.kt` |
| A | `android/app/src/main/java/com/silverlingo/app/ui/theme/Type.kt` |
| A | `android/app/src/main/java/com/silverringo/app/HomeScreen.kt` |
| A | `android/app/src/main/java/com/silverringo/app/ui/theme/Color.kt` |
| A | `android/app/src/main/java/com/silverringo/app/ui/theme/Theme.kt` |
| A | `android/app/src/main/java/com/silverringo/app/ui/theme/Type.kt` |
| A | `android/app/src/main/java/kr/silverringo/learning/KioskPracticeScreen.kt` |
| A | `android/app/src/main/java/kr/silverringo/learning/LearningEntry.kt` |
| A | `android/app/src/main/java/kr/silverringo/learning/LearningResult.kt` |
| A | `android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` |
| A | `android/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` |
| A | `android/app/src/main/res/values/strings.xml` |
| A | `android/app/src/main/res/values/themes.xml` |
| A | `android/app/src/main/res/xml/accessibility_service_config.xml` |
| A | `android/app/src/main/res/xml/backup_rules.xml` |
| A | `android/app/src/main/res/xml/data_extraction_rules.xml` |
| A | `android/app/src/test/java/kr/silverringo/learning/LearningResultTest.kt` |
| A | `android/build.gradle.kts` |
| A | `android/gradle.properties` |
| A | `android/gradle/libs.versions.toml` |
| A | `android/gradle/wrapper/gradle-wrapper.jar` |
| A | `android/gradle/wrapper/gradle-wrapper.properties` |
| A | `android/gradlew` |
| A | `android/gradlew.bat` |
| A | `android/settings.gradle.kts` |
| A | `docs/ANDROID_FILE_CHANGES.md` |
| A | `docs/ANDROID_INTEGRATION.md` |
| A | `docs/ANDROID_VALIDATION.md` |
| A | `docs/TEAM_SYNC_AUDIT.md` |
| A | `docs/validation/android-build.log` |
| A | `docs/validation/browser-test.log` |
| A | `docs/validation/device-test.log` |
| A | `docs/validation/engine-test.log` |
| A | `docs/validation/github-discord-evidence.log` |
| A | `docs/validation/github-main-4acba96/results.json` |
| A | `docs/validation/github-main-4acba96/summary.md` |
| A | `docs/validation/github-main-4acba96/tests.log` |
| A | `docs/validation/github-team-sync.log` |
| A | `docs/validation/source-diffs/Juno-AndroidManifest.xml.diff` |
| A | `docs/validation/source-diffs/Juno-MainActivity.kt.diff` |
| A | `docs/validation/source-diffs/Juno-README.md.diff` |
| A | `docs/validation/source-diffs/Juno-build.gradle.kts.diff` |
| A | `docs/validation/source-diffs/hjh-HomeScreen.kt.diff` |
| A | `docs/validation/source-diffs/jib-LearningEntry.kt.diff` |
| A | `docs/validation/team-sync-local/results.json` |
| A | `docs/validation/team-sync-local/summary.md` |
| A | `docs/validation/team-sync-local/tests.log` |
| A | `docs/validation/team-sync-workflow/results.json` |
| A | `docs/validation/team-sync-workflow/summary.md` |
| A | `docs/validation/team-sync-workflow/tests.log` |
| A | `silver-ringo-c/.gitignore` |
| A | `silver-ringo-c/README.md` |
| A | `silver-ringo-c/docs/INTEGRATION.md` |
| A | `silver-ringo-c/package-lock.json` |
| A | `silver-ringo-c/package.json` |
| A | `silver-ringo-c/scripts/build-demo.cjs` |
| A | `silver-ringo-c/scripts/sync-content.cjs` |
| A | `silver-ringo-c/silver-ringo-demo.html` |
| A | `silver-ringo-c/tests/browser.cjs` |
| A | `silver-ringo-c/tests/engine.test.cjs` |
| A | `silver-ringo-c/web/app.js` |
| A | `silver-ringo-c/web/assets/burger.svg` |
| A | `silver-ringo-c/web/assets/chicken.svg` |
| A | `silver-ringo-c/web/assets/cola.svg` |
| A | `silver-ringo-c/web/assets/fries.svg` |
| A | `silver-ringo-c/web/assets/shrimp.svg` |
| A | `silver-ringo-c/web/content.js` |
| A | `silver-ringo-c/web/content.json` |
| A | `silver-ringo-c/web/engine.js` |
| A | `silver-ringo-c/web/index.html` |
| A | `silver-ringo-c/web/styles.css` |
| A | `silverRingo/.gitignore` |
| A | `silverRingo/README.md` |
| A | `silverRingo/app/.gitignore` |
| A | `silverRingo/app/build.gradle.kts` |
| A | `silverRingo/app/src/androidTest/java/com/silverringo/app/ExampleInstrumentedTest.kt` |
| A | `silverRingo/app/src/androidTest/java/com/silverringo/app/HomeScreenTest.kt` |
| A | `silverRingo/app/src/main/AndroidManifest.xml` |
| A | `silverRingo/app/src/main/java/com/silverringo/app/HomeScreen.kt` |
| A | `silverRingo/app/src/main/java/com/silverringo/app/MainActivity.kt` |
| A | `silverRingo/app/src/main/java/com/silverringo/app/ui/theme/Color.kt` |
| A | `silverRingo/app/src/main/java/com/silverringo/app/ui/theme/Theme.kt` |
| A | `silverRingo/app/src/main/java/com/silverringo/app/ui/theme/Type.kt` |
| A | `silverRingo/app/src/main/keepRules/rules.keep` |
| A | `silverRingo/app/src/main/res/drawable/ic_launcher_background.xml` |
| A | `silverRingo/app/src/main/res/drawable/ic_launcher_foreground.xml` |
| A | `silverRingo/app/src/main/res/mipmap-anydpi/ic_launcher.xml` |
| A | `silverRingo/app/src/main/res/mipmap-anydpi/ic_launcher_round.xml` |
| A | `silverRingo/app/src/main/res/mipmap-hdpi/ic_launcher.webp` |
| A | `silverRingo/app/src/main/res/mipmap-hdpi/ic_launcher_round.webp` |
| A | `silverRingo/app/src/main/res/mipmap-mdpi/ic_launcher.webp` |
| A | `silverRingo/app/src/main/res/mipmap-mdpi/ic_launcher_round.webp` |
| A | `silverRingo/app/src/main/res/mipmap-xhdpi/ic_launcher.webp` |
| A | `silverRingo/app/src/main/res/mipmap-xhdpi/ic_launcher_round.webp` |
| A | `silverRingo/app/src/main/res/mipmap-xxhdpi/ic_launcher.webp` |
| A | `silverRingo/app/src/main/res/mipmap-xxhdpi/ic_launcher_round.webp` |
| A | `silverRingo/app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp` |
| A | `silverRingo/app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp` |
| A | `silverRingo/app/src/main/res/values/colors.xml` |
| A | `silverRingo/app/src/main/res/values/strings.xml` |
| A | `silverRingo/app/src/main/res/values/themes.xml` |
| A | `silverRingo/app/src/main/res/xml/backup_rules.xml` |
| A | `silverRingo/app/src/main/res/xml/data_extraction_rules.xml` |
| A | `silverRingo/app/src/test/java/com/silverringo/app/ExampleUnitTest.kt` |
| A | `silverRingo/build.gradle.kts` |
| A | `silverRingo/gradle.properties` |
| A | `silverRingo/gradle/gradle-daemon-jvm.properties` |
| A | `silverRingo/gradle/libs.versions.toml` |
| A | `silverRingo/gradle/wrapper/gradle-wrapper.jar` |
| A | `silverRingo/gradle/wrapper/gradle-wrapper.properties` |
| A | `silverRingo/gradlew` |
| A | `silverRingo/gradlew.bat` |
| A | `silverRingo/settings.gradle.kts` |

## main 반영 제외 (로컬 파일 보존)

- APK: `**/*.apk`. Gradle wrapper JAR는 빌드 필수 소스이므로 포함.
- `.local/` 전체(SDK·인증 API 보조 스크립트·캐시·초기 결과 백업), `.gradle/`, `**/build/`, `**/local.properties`.
- `node_modules/`, 새 `__pycache__/`·pyc, 브라우저 tests/artifacts/.
- `android/emulator*.png`: Juno 과거 검수 스크린샷. 이번 검수 증거가 아니므로 제외.
- `silver-ringo-c/android/`: jib 원본 독립 호스트. 실제 통합 소스는 android/로 이식.
- `silver-ringo-c/docs/REVIEW.md`, VALIDATION.md, browser-test.log, engine-test.log: 원본 과거 검수 자료.
- tracked `Tester/.team-sync/` 및 기존 pyc는 원본 main 상태 보존. 생성 결과는 모드별 docs/validation에 별도 기록.
- 다른 팀원 코드·예제를 삭제하지 않았다. 루트 main에는 원래 Android 앱이 없어 Juno/jib와의 파일 충돌은 없었다.
