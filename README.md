## 2차 통합 기준 버전 — 1.1.1 (2026-10-30)

Android 개발 진입점은 `android/` 하나다. 최신 Juno 가이드 수정, 기존 키오스크 학습, 홈 UI, FastAPI 챗봇 연결을 포함한다. 일반 사용자용 서버 주소 설정은 없으며 개발자는 빌드 설정으로 주소를 지정한다. 운영 주소가 비어 있으면 '챗봇 서비스 준비 중'으로 표시한다.

- [2차 통합 구조](docs/ANDROID_INTEGRATION_V2.md)
- [이번 main 반영·담당자 확인사항](docs/RELEASE_1_1_1.md)
- [챗봇 서버 배포 기준](docs/CHATBOT_DEPLOYMENT.md): DHub 승인 대기 중이며 VM 배포·실제 Gemini 연결은 별도 작업이다.

# Silver Ringo 통합 Android 앱

개발 기준 앱은 **`android/`** 입니다. Android Studio에서는 이 폴더를 여세요.
Juno의 가이드 앱에 hjh의 홈 UI와 jib의 키오스크 마이크로러닝을 연결합니다.
챗봇 화면은 기존 Python FastAPI의 POST /v1/chat에 연결합니다. 운영 서버 미설정 안내와 연결 실패 재시도를 제공합니다.

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

## 스마트폰 교육 1주차 추가

홈 → IT 기능 배우기에서 스마트폰 교육과 키오스크를 선택합니다.
스마트폰 교육은 6개 주차의 목록을 제공하며, 1주차의 밝기·소리·글자 크기 / 전화 / 연락처 / 문자 실습을 Kotlin + Compose로 구현했습니다.
2~6주차 실습은 준비 중입니다. 변경 파일과 팀원별 영향은 [스마트폰 인수인계](docs/SMARTPHONE_WEEK1_HANDOVER.md),
이번 빌드 및 테스트 결과는 [검증 기록](docs/SMARTPHONE_WEEK1_VALIDATION.md)을 확인하세요.

스마트폰 교육 v2는 그림·TTS 교육 → 따라 연습 → 최종 실습으로 구성됩니다. 실제 기기 음량/밝기와 기본 앱을 사용하는 선택 경로, C파트 고대비 테마를 포함합니다. 실제값 확인·사용자 확인·가상 수행을 구분해 결과를 기록합니다.
