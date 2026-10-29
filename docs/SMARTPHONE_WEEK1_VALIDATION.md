# 스마트폰 1주차 v2 검증 결과

검증일: 2026-10-29. 기준 main: `2e8f75e517b09770f1629ce04bc579f691863cc0`.

| 검사 | 결과 |
| --- | --- |
| 통합 앱 Kotlin/Compose 빌드 | v2 debug APK 생성 확인 |
| JVM 단위 테스트 | 13개 통과: PhoneEngineTest 8, PracticeEvidenceTest 4, LearningResultTest 1. 오류·실패·건너뜀 0 |
| Android 기기 테스트 | 테스트 APK 생성 확인. 기기 실행은 미수행 |
| 기존 키오스크 JS 엔진 | 이번 이어하기에서 재실행: 11개 통과 |
| APK 서명 | Google apksig 8.6.1로 v2 서명 검증 성공. 이전 전달 v1과 인증서 SHA-256 동일 |
| 키오스크 자산 | APK 내 11개 파일이 기존 web 소스와 바이트 단위 일치 |
| 주요 글자/배경 대비 | 5개 색상 쌍 모두 4.5:1 이상. contrast.log 참고 |
| 실제 Android 실행 | 미수행. S25/Android 17/One UI 9에서 실행했다고 주장하지 않음 |

이전 작업의 빌드 산출물과 JUnit XML은 남아 있으며, 이어하기에서 재확인했다. 환경 재시작으로 임시 SDK와 v2 Gradle 콘솔 로그는 남아 있지 않아 이번 이어하기에서 Gradle을 다시 실행하지 않았다. `android-build-v1.log`는 이전 버전 기록이므로 v2 로그로 해석하지 않는다. 증거 파일은 `docs/validation/smartphone-week1/`에 포함했다.

- 설치 파일: `silver-ringo-smartphone-week1-v2-debug.apk`
- applicationId: `com.silverlingo.app`, versionCode: 1
- min SDK 26 / target SDK 35. 교육의 Android 14 기준과 빌드 target SDK는 별개다.
- APK 크기: 17708805 bytes
- SHA-256: `32eb0178d1c3f96cfcc4a175082602d0fea95f7581399d17d3c864d2a04a69e1`
- 인증서 SHA-256: `6d6f7bfdb4992526f2877c6874b67254180593e5a17328cf6e08a6b5c9e13c18`

## 기기에서 확인할 사항

APK 설치 → 홈 → IT 기능 배우기 → 스마트폰 교육 → 1주차.
교육 3장 → 따라 연습 → 최종 실습 → 완료 저장 순서를 확인한다.
실제 버튼·밝기 관찰, 한국어 TTS, 기본 앱 Intent, 화면 회전, 큰 글꼴, TalkBack, 키보드와 스크롤은 기기 확인이 필요하다. 실제 전화·문자는 상대와 연습 가능한 때 사용자가 직접 수행한다. 앱은 통화/문자 성공을 자동 검증하지 않는다.

이전 제공 v1 APK와 서명이 같아 같은 앱으로 업데이트할 수 있는 구성이다. 다른 팀 빌드와 서명이 다르면 설치가 거절될 수 있으며 앱 삭제는 저장 진도를 지운다. 기존 키오스크 웹 UI는 이번에 직접 재실행하지 않았고 웹 파일은 변경하지 않았다.
