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
