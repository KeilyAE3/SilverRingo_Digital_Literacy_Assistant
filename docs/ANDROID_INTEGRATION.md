> 현재 팀 기준: [1.1.1 main 반영 기록](RELEASE_1_1_1.md). 아래 날짜별 내용은 과거 작업 당시 기록도 포함한다.

# 현재 작업: 2차 통합 (2026-10-29)

최신 main 2e8f75e 기준 로컬 hjh-integration-v2에서 작업했다. Push·커밋·main 병합은 하지 않았다. 아래의 1차 통합 Push 승인은 과거 기록이며 이번 작업의 승인이 아니다. 최신 구조·챗봇 실행·기기 주소는 [2차 통합 기준](ANDROID_INTEGRATION_V2.md)을 따른다.

---

# 통합 Android 개발 기준

## 기준과 원본

2026-10-28 로컬 `hjh`에 원격 추적 브랜치의 파일을 추출했다. 브랜치 Merge나 Commit은 하지 않았다.
프로젝트를 새로 생성하지 않고 Juno의 `android/`를 그대로 가져왔다.

| 원본 | 기준 커밋 | 사용 파일 |
| --- | --- | --- |
| origin/Juno | `0025219bc56c20a9a59d98e5cc2cfc81d348df18` | `android/` 전체 프로젝트 |
| origin/jib | `f011f17f8c422f104ed8ba72ed8480713958b7e7` | `silver-ringo-c/web/`, `tests/`, `scripts/`, `docs/`, Android의 `LearningEntry.kt`, `KioskPracticeScreen.kt` |
| origin/hjh | `28772a853240c2a4794233547c9a44ca5f587492` | `silverRingo/app/src/main/java/com/silverringo/app/HomeScreen.kt`, `ui/theme/` |

hjh 원본 UI 프로젝트는 비교·참고용으로 남긴다. jib Android 호스트는 원본 브랜치를 참조하며
main 반영 대상에서는 제외한다. 빌드 진입점은 `android/` 하나다.
원본 문서의 과거 검증 기록은 이번 통합 앱의 검증 증거가 아니다. 이번 결과는 `ANDROID_VALIDATION.md`를 본다.

## 보존한 환경과 기능

Kotlin 2.0.21, AGP 8.6.1, Gradle 8.7, JDK 17, Compose BOM 2024.09.03,
compile/target SDK 35, min SDK 26 및 applicationId `com.silverlingo.app`를 유지한다.
`MainDashboardScreen`, `UserProfileManager`, `CompanionBrowserActivity`, 접근성 서비스,
플로팅 서비스, 오버레이 스크립트, 테마, 권한·서비스 선언을 기준으로 사용한다.
가이드는 원본에서 지원하는 똑닥 병원 접수다. 실제 접수·로그인에는 네트워크와 외부 서비스가 필요하다.

## 통합 코드와 이유

| 경로 (`android/app/` 기준) | 변경 및 이유 |
| --- | --- |
| `src/main/java/com/silverlingo/app/MainActivity.kt` | 홈·학습·가이드 목적지를 `rememberSaveable`로 유지한다. hjh 임시 목적지 대신 실제 기능을 호출한다. 가이드의 명시적 홈 버튼과 시스템 뒤로가기를 연결하고 홈 복귀 시 플로팅 서비스를 종료한다. |
| `src/main/java/com/silverringo/app/HomeScreen.kt`, `ui/theme/` | hjh에서 복사. 기존 두 진입 콜백을 유지하고 기능 없는 바로가기 예정 영역은 제거한다. 별도 런처 Activity는 복사하지 않는다. |
| `src/main/java/kr/silverringo/learning/LearningEntry.kt` | jib의 학습 목록·단계 잠금·완료 저장을 재사용한다. 결과 승인 조건만 `completedLesson`으로 분리해 테스트한다. |
| `src/main/java/kr/silverringo/learning/KioskPracticeScreen.kt` | jib에서 복사. APK 자산 로더, origin 제한 브리지, 한국어 TTS, 화면 종료 시 WebView·TTS 해제를 재사용한다. |
| `src/main/java/kr/silverringo/learning/LearningResult.kt` | 실행한 단계와 일치하고 `passed=true`, mode가 `learn` 또는 `test`인 결과만 완료 처리한다. 자유 실습·잘못된 JSON·누락 필드는 진도를 올리지 않는다. |
| `build.gradle.kts` | WebKit 1.12.1과 기준 lifecycle 2.8.6 Compose 연동을 추가한다. `preBuild`의 Sync로 웹 소스를 생성 assets/kiosk에 복사해 중복 편집을 방지한다. JVM·기기 테스트 의존성도 추가한다. |
| `src/main/AndroidManifest.xml` | Android 11 이상 TTS 엔진 탐색용 `TTS_SERVICE` queries를 추가한다. 기존 권한·서비스·가시성은 유지한다. |
| `src/test/.../LearningResultTest.kt` | 진도 승인·거절 조건의 Kotlin 회귀 테스트. |
| `src/androidTest/.../IntegratedAppTest.kt` | 홈 왕복, 실제 WebView 결과 브리지, Activity 재생성 후 완료 유지 검사. 기기가 있어야 실행된다. |
| 저장소 `.gitignore`, `README.md`, `docs/`, 각 Android 프로젝트 README | 로컬 SDK·빌드·기기별 설정 제외 및 단일 개발 진입점·검수 절차 기록. 원본 프로젝트 문서에 통합 앱 위치를 안내한다. |

학습의 `web/engine.js`, `web/app.js`, 콘텐츠·이미지는 원본을 유지한다.
기존 엔진을 Compose로 다시 작성하거나 새 공통 가이드 엔진을 만들지 않았다.

## 화면 이동 및 필수 인터페이스

`MainActivity → SilverRingoApp → HomeScreen(onLearnClick, onGuideClick)`이 루트다.

- 홈 → 학습: `LearningEntry(onExit, onResult)` 호출.
- 학습 목록 → 실습: `KioskPracticeScreen(levelId, onExit, onResult)` 호출.
- 실습의 학습 목록 버튼/시스템 뒤로가기 → Compose 목록. 목록의 학습 나가기/시스템 뒤로가기 → 홈.
- 홈 → 생활 가이드: 기존 `MainDashboardScreen()` 호출. 공식 앱·웹 동행·권한·프로필 UI를 유지한다.
- 웹 동행 상단 뒤로가기/시스템 뒤로가기 → 가이드. 가이드 홈 버튼/시스템 뒤로가기 → 홈.
- 똑닥 공식 앱은 외부 앱이므로 Android의 뒤로가기/최근 앱으로 통합 앱에 복귀한다.

신규 기능은 루트의 목적지 분기에 Compose 화면을 추가하고 HomeScreen에 클릭 콜백을 전달한다.
기능 화면은 필수 `onExit: () -> Unit`으로 호스트에 복귀를 위임한다.
복잡한 전역 내비게이션 라이브러리나 새 Activity는 필요할 때만 추가한다.
자원(TTS·WebView·서비스)은 소유한 화면/서비스가 수명주기에 맞춰 정리한다.
학습 기능의 `onResult: (String) -> Unit`은 결과 JSON을 전달하며 호스트는 `latest_result`에 기록한다.
가이드의 실제 접근성 탐지와 학습 시뮬레이터의 채점은 서로 다른 책임이다.

## 키오스크 사례로 학습 콘텐츠 추가하기

1. `silver-ringo-c/web/content.js`의 `levels`에 단계를 정의한다. 현재 순차 정수 ID 1~6을 사용하며
   잠금은 이전 ID 완료를 요구한다. 다른 주제를 추가할 때는 `topicId`별 진도 키를 먼저 분리한다.
2. 기존 키오스크의 1단계는 `start → menu`, 검사 키 `dining`이다. 2단계는 메뉴 선택,
   3단계는 옵션/담기, 4단계는 주문 확인, 5단계는 결제, 6단계는 전체 주문 평가다.
   `start`, `end`, `checks`, `concept`, `task`를 실제 엔진 화면과 검사 키에 맞춰 작성한다.
3. 기존 상품·단계를 바꾸는 경우 content.js를 편집한다. 이미지 파일은 `web/assets/`에 둔다.
   새로운 조작이나 화면이 필요할 때만 `engine.js`의 순수 transition/evaluate와 `app.js` 렌더링을 확장한다.
   단순 문구·가격·목표 변경에는 엔진을 재작성하지 않는다.
4. `cd silver-ringo-c; npm run sync`로 Compose 목록이 읽는 `web/content.json`을 갱신한다.
   웹은 content.js, Compose는 content.json을 사용하므로 둘을 함께 반영한다.
5. `npm test`와 `npm run test:browser`로 목표 달성·잘못된 조작·진도·좁은 화면을 확인한다.
   기존 테스트는 6단계와 메뉴를 명시적으로 검증하므로 콘텐츠 변경 시 기대값도 바꾼다.
6. `android/gradlew.bat :app:assembleDebug`에서 웹 파일 전체가 자동 assets/kiosk로 복사된다.
   생성된 `app/build/generated/learningAssets`를 직접 수정하지 않는다.
7. APK에서 새 목록·실습 진입·결과·재실행 후 저장·종료를 직접 확인한다.

### WebView 계약

진입 URL: `https://appassets.androidplatform.net/assets/kiosk/index.html?embed=1&level=N`.
웹은 `SilverRingoNative.postMessage(JSON.stringify(message))`로 `result`, `exit`, `speak`, `stopSpeech`를 전달한다.
브리지는 허용된 HTTPS origin과 메인 프레임에만 적용한다. 학습 WebView는 네트워크·파일·콘텐츠 접근을 차단한다.
가이드 웹 동행은 별도 WebView이며 기존 INTERNET 권한을 사용한다.

```json
{"type":"result","schemaVersion":1,"topicId":"kiosk","levelId":1,"mode":"learn","passed":true,"failures":[],"hints":0,"actions":2,"elapsedSeconds":10,"completedAt":"2026-10-28T06:00:00Z"}
```

Android `sr_learning` SharedPreferences: `done_N` 완료 여부, `last_result_N` 통과 결과,
`latest_result` 마지막 전달 결과. 완료는 누적하며 실패/자유 실습으로 취소하지 않는다.
웹 localStorage `silver-ringo-c-v3`는 별도의 상세 실습 이력/세션을 저장한다.
서버·다른 기기 간 진도 동기화는 구현하지 않았다.
실습 도중 화면 회전/프로세스 종료 후에는 같은 단계의 소개로 돌아가며 세부 주문 상태를 자동 복원하지 않는다.
완료한 단계는 네이티브 저장소로 보존된다. 앱 데이터 삭제/삭제 후 재설치에는 보존을 보장하지 않는다.

## 호환성 및 해결

- hjh의 SDK 37/JDK 25 및 jib의 더 최신 Compose 환경을 기준 앱에 이식하지 않고 Juno 버전을 유지했다.
- JDK 24가 기본인 노트북에서는 Gradle 8.7을 JDK 17로 실행한다. Unity에 포함된 Temurin 17.0.9 사용 가능.
- Unity SDK에는 플랫폼 35는 있지만 AGP 기본 Build Tools 34가 없어 첫 빌드가 실패했다.
  프로젝트 `.local/android-sdk`에 플랫폼을 복사하고 Build Tools 34/platform-tools를 설치해 해결한다.
  Unity 설치 폴더에 의존 패키지를 설치하거나 전역 Java 설정을 바꾸지 않는다.
- Android System WebView가 WEB_MESSAGE_LISTENER를 지원해야 음성·진도·웹 종료가 연결된다.
  미지원 기기는 업데이트 안내를 표시하며 시스템 뒤로가기로 목록에 복귀할 수 있다.
- 한국어 TTS 엔진/언어 데이터가 없으면 학습 화면은 안내 문구를 표시한다. 실제 음성 출력은 기기 검수가 필요하다.
- 똑닥의 ARM64 앱은 x86 에뮬레이터에서 실행되지 않을 수 있다. 가이드 웹 모드와 실제 ARM64 기기 검수를 구분한다.
- 가이드의 외부 사이트 로그인·실제 접수 성공은 팀 앱만으로 보장할 수 없다. 테스트 중 실제 접수/결제는 실행하지 않는다.

## 로컬 작업 규칙

개발 브랜치는 `hjh`. 사용자가 APK 설치·앱 실행을 확인하고 main 반영·Commit·Push를 승인했다.
이번 작업은 로컬 hjh 통합 코드를 커밋하고 최신 main에 병합·검증 후 main에만 Push한다.
원격 hjh/Juno/jib는 수정하지 않는다. Force Push·강제 Reset·보호 정책 우회는 하지 않는다.
일부 가이드 버그는 원인 미확정이며 설치·앱 실행 성공과 가이드 전체 정상 동작을 구분한다.

담당자별 검토 및 파일별 실제 diff: [ANDROID_FILE_CHANGES.md](ANDROID_FILE_CHANGES.md).
자동 검사 운영 규칙·문서 차이·원격 실행 결과: [TEAM_SYNC_AUDIT.md](TEAM_SYNC_AUDIT.md).

main 병합 `4acba96266fb75e6df4561658558eb96bddf8ed5`를 Push했고 실제 Team Sync 및 Discord 전송이 성공했다.
세부 결과·SHA가 일치하는 artifact 및 담당자 확인사항은 위 문서에 기록했다.
