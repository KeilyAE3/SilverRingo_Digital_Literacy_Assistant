# SilverLingo Android 네이티브 모듈 (Kotlin + Jetpack Compose)

어르신의 온라인 관공서 및 금융 업무를 화면 위에서 1:1로 직접 동행하는 **SilverLingo 서드파티 플로팅 오버레이 앱**입니다.

---

## 🛠️ 기술 스택
- **Language**: Kotlin 2.0.21
- **UI Framework**: Jetpack Compose (Material3)
- **Minimum SDK**: 26 (Android 8.0 Oreo 이상)
- **Target / Compile SDK**: 35 (Android 15)
- **Build System**: Gradle 8.7 (Kotlin DSL) + Android Gradle Plugin 8.6.1
- **Key APIs**:
  - `SYSTEM_ALERT_WINDOW`: 다른 앱 위에 상시 상주하는 플로팅 동행 버블 위젯
  - `AccessibilityService`: 똑닥(DDocDoc) 앱 화면 노드 실시간 탐지 및 화면 위 노란색 링/말풍선 주입
  - `androidx.security.crypto`: 개인정보 온디바이스 로컬 암호화 (AES256 Zero-Knowledge)
  - `TextToSpeech`: 각 단계별 친절한 음성 설명 제공

---

## 📂 폴더 구조

```
android/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/silverlingo/app/
│       │   ├── MainActivity.kt               # Jetpack Compose 엔트리포인트
│       │   ├── data/
│       │   │   └── UserProfileManager.kt     # EncryptedSharedPreferences 기반 온디바이스 로컬 저장소
│       │   ├── service/
│       │   │   ├── FloatingCompanionService.kt  # 화면 상시 상주 플로팅 도우미 (ComposeView 렌더링)
│       │   │   ├── SilverAccessibilityService.kt # 화면 요소 감지 및 스포트라이트 포커스 링 오버레이
│       │   │   └── ServiceLifecycleOwner.kt     # Service 내 Compose 수명주기 홀더
│       │   └── ui/
│       │       ├── screens/
│       │       │   ├── MainDashboardScreen.kt   # 병원 안심 당일 접수(똑닥) 단일 진입 화면
│       │       │   └── UserProfileDialog.kt     # 내 정보 입력 및 수정 다이얼로그
│       │       └── theme/
│       │           ├── Color.kt                 # 고대비 색상 팔레트
│       │           ├── Theme.kt                 # Material3 테마
│       │           └── Type.kt                  # 20sp 이상 어르신 맞춤 큰 글씨 타이포그래피
│       └── res/
│           ├── values/strings.xml, themes.xml
│           └── xml/accessibility_service_config.xml
├── gradle/libs.versions.toml                 # 의존성 버전 카탈로그
├── local.properties                          # 로컬 Android SDK 경로 설정
└── build.gradle.kts, settings.gradle.kts
```

---

## 🚀 Android Studio에서 실행 방법

1. **Android Studio 실행**
2. **[Open]** 클릭 ➔ `C:\SilverLingooooo\android` 폴더 선택 후 열기
3. Gradle Sync가 자동으로 진행됩니다.
4. 연결된 Android 기기(실제 갤럭시 폰 또는 에뮬레이터)를 선택하고 **Run 'app'** (`Shift + F10`) 클릭.

### 📱 최초 실행 시 권한 허용 안내
1. **다른 앱 위에 그리기 권한**: 앱 대시보드 상단의 권한 버튼을 눌러 허용 (플로팅 위젯 표시용).
2. **접근성 권한**: 설정 ➔ 접근성 ➔ 설치된 앱 ➔ **[실버링고 (SilverLingo)]** 켜기 (화면 버튼 탐색용).
