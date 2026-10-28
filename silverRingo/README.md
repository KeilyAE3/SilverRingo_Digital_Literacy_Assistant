# Silver Ringo Android UI

> 이 폴더는 hjh 원본 UI 참고용입니다. 현재 통합 앱은 저장소의 `android/`에서 빌드합니다.
> 통합 앱 개발에는 아래 SDK 37/JDK 25 설정 대신 [통합 개발 문서](../docs/ANDROID_INTEGRATION.md)를 따르세요.

Kotlin + Jetpack Compose로 만든 독립 실행 가능한 메인 UI입니다.
Android Studio에서 이 폴더를 열고 로컬 Android SDK 경로를 설정한 뒤 실행합니다.
Gradle Wrapper가 포함되어 있으며 SDK 37과 Gradle 데몬용 JDK 25를 사용합니다.
`local.properties`와 빌드 결과물은 저장소에 포함하지 않습니다.

## 화면 연결

`HomeScreen(onLearnClick, onGuideClick)`의 두 콜백에 학습·가이드 화면을 연결합니다.
`MainActivity`의 `SilverRingoApp`은 현재 임시 화면과 홈 복귀 동작만 제공합니다.
AI 대화와 음성 입력은 입력 상태 및 준비 중 안내만 구현되어 있습니다.

## 검증

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

연결된 기기에서 UI 테스트: `.\gradlew.bat :app:connectedDebugAndroidTest`
기존 Espresso 3.5.1은 API 37 에뮬레이터에서 `InputManager.getInstance` 호환성 오류가
발생했습니다. 해당 환경에서는 메뉴·진입·복귀·입력·안내를 ADB로 별도 확인했습니다.
