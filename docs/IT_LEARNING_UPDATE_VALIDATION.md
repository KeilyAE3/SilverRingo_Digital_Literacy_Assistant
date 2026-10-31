# IT 배우기 업데이트 검증 (2026-10-31)

## 통과

- Kotlin/Compose 앱 assembleDebug: 설치용 APK 생성.
- assembleDebugAndroidTest: 계측 테스트 APK 빌드 성공 (기기 실행은 미수행).
- testDebugUnitTest: 총 18개, 실패/오류 0. 스마트폰 엔진 8, 실제 조작 증거 4, 결과 파서 1, 기존 챗봇 4, 가독성 회귀 1.
- 가독성 회귀: 부모 테마에 검정 글자 스타일을 설정한 상태에서 실제 LearningEntry → 스마트폰 교육 화면을 Robolectric API 35 네이티브 그래픽으로 렌더링. 첫 주차 제목의 흰 글자 픽셀과 남색 배경 픽셀 검사 통과. 렌더링 화면도 확인했습니다. 실기기 스크린샷은 아닙니다.
- JavaScript 엔진: 기존 회귀 10 + 새 5레벨 회귀 8 = 18개 통과.
- Team Sync 학습 래퍼 2개 통과: 위 JS 검사와 content.js/content.json 일치.
- Chromium: 420px, 1440px에서 각각 5레벨 완료, 결과 브리지 이벤트, 최종 레벨 강조 없음, 메뉴 이미지 로드, 메뉴 화면 가로 넘침 없음, 페이지 오류 없음.
- APK 서명 검증 통과. 인증서 SHA256: e3a258155b867de5298a453c33abe5dcb99754696d26889be0b60013ebea32b1.

## 한계

- 실제 S25 / Android 17 / One UI 9 기기에서 설치·음량 버튼·설정 앱·전화·문자·TTS는 이번 환경에서 실행하지 않았습니다.
- Android 계측 테스트는 APK 빌드와 실제 기기 실행을 구분합니다. 기기 연결 실행은 수행하지 않았습니다.
- 브라우저 검증은 Android WebView의 실제 기기 동작을 대체하지 않습니다.
- 공식 키오스크와 픽셀 단위 동일성은 검증하지 않았습니다. 전체 판매 메뉴·쿠폰·실결제는 구현 범위가 아닙니다.
- 기존 배포 APK는 현재 파일로 서명 비교가 불가능했습니다. 설치본과 동일 서명이라고 보장하지 않습니다.

## 재현

```bash
cd android
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest
cd ../silver-ringo-c
npm install
npm test
npx playwright install chromium
npm run test:browser
cd ..
python -m unittest Tester.tests.test_learning_engine -v
```

JDK 17 및 SDK 35 필요. 제한 네트워크 환경에서는 Gradle 및 Robolectric Maven 다운로드용 프록시 설정이 필요할 수 있습니다. 프록시·SDK·개인 키 설정은 저장소에 포함하지 않았습니다.
