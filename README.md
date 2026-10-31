## 팀 공용 통합 앱 — 1.1.1

Android 개발 진입점은 `android/` 하나다. 최신 Juno 가이드 수정, 기존 키오스크 학습, 홈 UI, FastAPI 챗봇 연결을 포함한다. 일반 사용자용 서버 주소 설정은 없으며 개발자는 빌드 설정으로 주소를 지정한다. 현재 Debug/Release는 DHub HTTPS 서버를 사용한다. 일반 디지털 질문·RAG 안내·후속 문맥과 답변별 학습/가이드 버튼을 지원한다. 운영 주소를 비우면 준비 중 안내로 전환된다.

- [2차 통합 구조](docs/ANDROID_INTEGRATION_V2.md)
- [이번 main 반영·담당자 확인사항](docs/RELEASE_1_1_1.md)
- [최신 main 반영·테스트·서버 동기화](docs/MAIN_CHAT_UPDATE.md)
- [챗봇 서버 배포 기준](docs/CHATBOT_DEPLOYMENT.md), [DHub 실제 배포 결과](docs/DHUB_DEPLOYMENT_RESULT.md)

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
