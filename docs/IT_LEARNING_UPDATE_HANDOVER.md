# IT 배우기 업데이트 인수인계 (2026-10-31)

## 기준과 적용 범위

- 기준 팀 jib: `5c7224c057e2be7790c040dc8ca5f0452ec91be0`
- 반영한 main: `e5fe417c5e3a8209879d97f760f7e79d2a18a3ca`
- 두 이력을 보존한 로컬 통합 커밋: `75546669e5e58faa64d790128aef01ddf2535ec3`
- 앱: versionCode 4 / versionName 1.1.2-c. 원격 저장소에는 자동으로 push하지 않았습니다.

## C파트 변경

- 키오스크: 매장 선택 / 메뉴 선택 / 옵션·주문담기 / 결제·영수증 / 혼자 주문하기의 5레벨. 안내 모드와 혼자 연습 모드, TTS, 진도 및 결과 연결 유지.
- 공식 메뉴·UI 이미지와 프레임·좌측 분류·옵션 팝업을 적용했습니다. 전체 공식 서비스의 복제나 실제 주문이 아닙니다. 세부 출처는 silver-ringo-c/web/IMAGE_SOURCES.md.
- 가독성: 학습 전용 테마에서 부모의 검정 글자 스타일 상속을 차단하고 Text에 현재 contentColor를 명시했습니다. 남색 버튼은 흰 글자, 비활성 버튼은 밝은 회색 배경과 어두운 글자입니다. 주차 버튼은 모서리 20dp로 조정했습니다.
- 실제 밝기 감지는 필수 연습 판정에서 제거했습니다. 밝기 교육은 남아 있으며 실제 연습은 음량 올림·내림과 글자 크기 선택 확인으로 완료합니다. 글자 크기는 현재 설정 유지 선택도 인정합니다.
- 스마트폰 교육 courseVersion 3, 키오스크 courseVersion 4. 키오스크 진도 키는 kiosk4_done_N, 웹은 silver-ringo-kiosk-course4. 이전 진도는 삭제하지 않지만 새로운 5레벨로 자동 이관하지 않습니다.

## 타 파트와 공용 파일

A/B/D 구현 파일은 이번 C 수정에서 직접 편집하지 않았습니다. 아래 파일은 기존 jib에서 최신 main을 통합하면서 들어온 팀 변경입니다. 이를 C파트 신규 수정으로 오해하지 마세요.

- `.github/workflows/team-sync.yml`
- `README.md`
- `SilverRingo_Chat-bot/.gitignore`
- `SilverRingo_Chat-bot/knowledge/app_info.json`
- `SilverRingo_Chat-bot/pyproject.toml`
- `Tester/config.json`
- `Tester/docs/TEAM_SYNC.md`
- `Tester/tests/test_d_http.py`
- `android/app/build.gradle.kts`
- `android/app/src/androidTest/java/com/silverlingo/app/ChatUiTest.kt`
- `android/app/src/debug/AndroidManifest.xml`
- `android/app/src/main/java/com/silverlingo/app/MainActivity.kt`
- `android/app/src/main/java/com/silverlingo/app/chat/ChatClient.kt`
- `android/app/src/main/java/com/silverlingo/app/chat/ChatScreen.kt`
- `android/app/src/main/java/com/silverlingo/app/service/SilverAccessibilityService.kt`
- `android/app/src/main/java/com/silverringo/app/HomeScreen.kt`
- `android/app/src/main/res/xml/accessibility_service_config.xml`
- `android/app/src/test/java/com/silverlingo/app/chat/ChatClientTest.kt`
- `docs/ANDROID_FILE_CHANGES.md`
- `docs/ANDROID_INTEGRATION.md`
- `docs/ANDROID_INTEGRATION_V2.md`
- `docs/ANDROID_VALIDATION.md`
- `docs/CHATBOT_DEPLOYMENT.md`
- `docs/RELEASE_1_1_1.md`
- `docs/TEAM_SYNC_AUDIT.md`

이번 작업에서 직접 수정한 공용 파일:

| 파일 | 변경 이유 |
|---|---|
| android/app/build.gradle.kts | 앱 버전 증가, Robolectric와 Compose JVM 렌더링 테스트 의존성 및 Android 리소스 테스트 설정 |
| android/app/src/androidTest/java/com/silverlingo/app/IntegratedAppTest.kt | 5레벨 첫 제목·진입 단계·진도 키에 맞춰 기존 통합 테스트 변경 |
| Tester/tests/test_learning_engine.py | 새 5레벨 검사 포함, 총 18개 JS 테스트 실행 |
| .gitignore | Kotlin 빌드 캐시 제외 |

## GitHub 적용 (PowerShell)

ZIP을 기존 저장소 위에 덮어쓰기보다 함께 제공한 bundle을 사용하세요. bundle은 기준 jib 이후의 main 통합 이력과 C 수정 커밋을 포함합니다.

```powershell
git switch jib
git status --short
# 출력이 있으면 기존 변경을 먼저 커밋 또는 별도 보관하세요.
git branch backup-before-it-update
git fetch "C:\Users\jj600\Downloads\silver-ringo-it-update.bundle" refs/heads/codex/it-update-v2
git merge --ff-only FETCH_HEAD
git push -u team jib
```

경로는 실제 다운로드 위치로 바꾸세요. fast-forward가 거절되면 강제 push하거나 reset하지 말고 양쪽 추가 커밋을 확인해야 합니다. bundle은 기준 커밋이 있는 저장소에서 적용하는 용도입니다. 신규 환경에는 전체 소스 ZIP을 사용하세요.

## Android Studio 실행

전체 소스의 android/ 폴더를 프로젝트로 엽니다. JDK 17, Android SDK 35 / Build Tools 34가 필요합니다. SDK 경로는 각 PC의 local.properties 또는 Android Studio가 설정합니다.

```powershell
cd android
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

APK: android/app/build/outputs/apk/debug/app-debug.apk. 키오스크 웹 assets는 silver-ringo-c/web에서 빌드 때 자동 복사합니다. 웹 단독 시연은 해당 폴더의 index.html을 여세요.

챗봇 서버 주소·배포 및 접근성 서비스 설정은 최신 main의 문서를 그대로 따릅니다. 이번 작업은 서버를 배포하거나 타 팀 기능을 실기기로 검증하지 않았습니다.

## 설치 시 서명

제공 APK는 개발용 서명입니다. 이전 설치본과 서명이 다르면 덮어 설치가 거절될 수 있습니다. 앱 삭제는 진도를 지우므로 먼저 보존 여부를 결정하고, 기존 개발 환경의 동일 키로 소스를 빌드하는 방법을 우선 사용하세요. 개인 서명키는 소스/ZIP/bundle에 포함하지 않습니다.

## 다음 실기기 확인

IT 배우기 → 스마트폰 교육 주차 목록의 흰 글자, 학습/연습/실습 화면 버튼, 음량 버튼 감지, 글자 크기 유지 선택, TTS, WebView 5레벨 주문 및 앱 재실행 진도 저장을 확인하세요. S25 Android 17 / One UI 9 실기기 검증은 사용자 환경에서 필요합니다.
