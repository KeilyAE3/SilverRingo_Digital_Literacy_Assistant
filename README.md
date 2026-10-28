# Silver Ringo 통합 Android 앱

개발 기준 앱은 **`android/`** 입니다. Android Studio에서는 이 폴더를 여세요.
Juno의 가이드 앱에 hjh의 홈 UI와 jib의 키오스크 마이크로러닝을 연결합니다.
챗봇은 구현하지 않았으며 홈의 입력란은 기존 준비 중 안내만 표시합니다.

- 개발·통합 방법 및 원본 기록: [docs/ANDROID_INTEGRATION.md](docs/ANDROID_INTEGRATION.md)
- 실행·검수 방법과 검증 결과: [docs/ANDROID_VALIDATION.md](docs/ANDROID_VALIDATION.md)
- APK 빌드: `cd android` 후 `./gradlew.bat :app:assembleDebug`
- APK: `android/app/build/outputs/apk/debug/app-debug.apk`

`silverRingo/`는 hjh의 원본 UI 참고용이며 jib의 원본 Android 호스트는 `jib` 브랜치를 참조합니다.
통합 APK는 `android/` 하나에서 빌드합니다.
학습 웹 소스와 엔진 테스트는 `silver-ringo-c/`를 계속 사용합니다.
사용자가 APK 설치·실행을 확인하고 팀 공용 main 반영을 승인했습니다. 일부 가이드 버그는 원인 조사 중입니다.
자동 검사 범위·실행 결과는 [Team Sync 분석](docs/TEAM_SYNC_AUDIT.md),
담당자별 변경은 [파일 변경 기록](docs/ANDROID_FILE_CHANGES.md)을 확인하세요.
