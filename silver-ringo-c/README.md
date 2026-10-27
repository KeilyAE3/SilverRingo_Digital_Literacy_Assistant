# Silver Ringo C파트 · 키오스크 학습 MVP v0.3

실행 가능한 웹 시뮬레이터와 Kotlin/Jetpack Compose Android 호스트 소스입니다. 최종 네이티브 실습 화면을 완성한 버전은 아닙니다. A/B/D 기능은 포함하지 않습니다.

## 가장 빠르게 실행

압축을 푼 뒤 `silver-ringo-demo.html`을 Chrome 또는 Edge에서 여세요. 설치나 인터넷 없이 실행됩니다. 원본은 `web/`의 분리된 파일입니다.

진도 저장이 차단된 브라우저나 시크릿 모드에서는 저장되지 않을 수 있습니다. 안정적인 동일 주소의 진도 저장과 개발 검증에는 아래 로컬 서버를 권장합니다.

```bash
python -m http.server 8000 --directory web
```

브라우저에서 http://localhost:8000 을 엽니다. `file://` 데모와 서버 주소는 저장 공간이 서로 다르며 진도를 공유하지 않습니다.

## 사용 흐름

- 1~5단계: 짧은 개념 설명 → 부분 주문 실습 → 목표 판정 → 다음 단계 해제.
- 2~5단계는 이미 배운 주문 상태를 미리 준비합니다. 실제로 수행하지 않은 앞 단계를 완료했다고 기록하지 않습니다.
- 6단계: 시작부터 주문번호까지 혼자 수행. 정답 강조는 없습니다.
- 자유 실습: 처음부터 전체 주문. 언제든 이용 가능하며 레벨 잠금은 해제하지 않습니다.
- 지정 목표: 매장에서 먹기 / 불고기 버거 세트 1개 / 감자튀김 / 콜라 / 카드 결제.
- 다른 메뉴·단품·포장·간편결제도 실제로 조작할 수 있습니다. 지정 목표와 다르면 결과에서 다시 연습하도록 안내합니다.
- 선택적 자동 음성, 반복 읽기, 힌트, 이전 화면, 다시 시작, 장바구니 삭제·수량 변경 지원.
- 현재 연습은 저장되며 웹 목록의 '이어 하던 연습'으로 복귀할 수 있습니다. 결과는 최근 120회, 완료 레벨은 별도 누적 저장합니다.

실제 주문·결제·카드번호 입력은 없습니다. 메뉴·이미지·가격은 가상 예시입니다. SVG는 이 프로젝트에서 직접 제작했습니다.

## Android Studio 실행

1. 압축 전체를 유지하고 Android Studio에서 **`android` 폴더**를 엽니다. `web`을 Android 폴더와 나란히 유지하세요.
2. Gradle JDK는 17로 지정합니다. SDK Manager에서 Android SDK Platform 35와 Build Tools 35를 설치합니다.
3. 인터넷이 되는 환경에서 Gradle Sync를 완료합니다. AGP 8.9.2 / Gradle 8.11.1 / Kotlin 및 Compose plugin 2.1.20을 사용합니다.
4. API 26 이상의 에뮬레이터 또는 기기에서 `app`을 실행합니다. Android System WebView를 최신 상태로 유지하세요.
5. 한국어 TTS 음성 데이터가 설치되어 있어야 한국어 안내가 나옵니다. 사용할 수 없으면 화면 안내와 메시지를 표시합니다.

Windows:

```powershell
cd android
.\gradlew.bat :app:assembleDebug
```

macOS/Linux:

```bash
cd android
chmod +x gradlew
./gradlew :app:assembleDebug
```

빌드 성공 시 APK 경로: `android/app/build/outputs/apk/debug/app-debug.apk`.

빌드 전에 `web/`이 generated assets의 `kiosk/`로 자동 복사됩니다. 웹 사본을 수동으로 두 군데 수정할 필요가 없습니다. 콘텐츠를 수정했다면 먼저 루트에서 `node scripts/sync-content.cjs`를 실행해 Android 목록용 JSON을 갱신하세요.

Android에서 목록·진도는 Compose/SharedPreferences가, 실습은 WebView가 담당합니다. 완료 이벤트를 받은 네이티브 목록이 해당 단계를 해제합니다. 웹과 Android 진도는 각각 기기 로컬에 저장되며 클라우드 동기화하지 않습니다. Android의 시스템 뒤로가기는 학습 목록으로 돌아오고, 실습 안의 이전 화면 버튼은 주문 단계 사이를 이동합니다. Android는 진행 중 단계의 화면 자동 복원 대신 다시 시작할 수 있으며, 완료 레벨은 유지됩니다.

**중요: 현재 제공물은 Android 소스 프로젝트입니다. APK 빌드·설치·TalkBack·실기기 TTS를 검증했다고 주장하지 않습니다. `docs/VALIDATION.md`를 확인하세요.**

## 팀 연동

A파트는 `LearningEntry(onExit = { /* A 화면으로 */ }, onResult = { json -> /* 학습 결과 */ })`를 호출합니다. 전체 프로젝트의 Activity를 대체하지 말고 학습 패키지·웹 자산과 필요한 의존성을 옮기세요. `MainActivity`는 단독 시연용입니다. Gradle 버전은 팀 호스트 프로젝트에 맞춥니다.

로컬 자산 origin으로 제한된 `SilverRingoNative.postMessage` 계약은 `docs/INTEGRATION.md`에 정의했습니다. B파트와는 안내 데이터 계약을 검토하되 실제 사이트 수행 엔진을 이 프로젝트에 넣지 않았습니다.

## 개발 / 테스트

Node.js 20 이상 권장. 엔진 테스트는 외부 의존성 없이 실행됩니다.

```bash
node --test tests/engine.test.cjs
node scripts/sync-content.cjs
node scripts/build-demo.cjs
```

브라우저 회귀 테스트:

```bash
npm install
npx playwright install chromium
node tests/browser.cjs
```

독립 실행 파일은 `node scripts/build-demo.cjs`로 재생성합니다. 원본 분리 파일을 먼저 수정하세요. 브라우저 테스트의 이미지 파일은 `tests/artifacts/`에 임시 생성됩니다.

분석/구조 선택/개발 순서는 `docs/REVIEW.md`, 검증 결과와 남은 일은 `docs/VALIDATION.md`에 있습니다.
