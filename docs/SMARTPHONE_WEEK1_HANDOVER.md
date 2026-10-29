# 스마트폰 교육 1주차 v2 — 교육·실제 기기 실습·가독성 개선

작성일: 2026-10-29. 기준 main: `2e8f75e517b09770f1629ce04bc579f691863cc0`.
로컬 `jib` 작업이며 원격 commit/push는 하지 않았다. 이 문서는 이전 1주차 v1 설명을 대체한다.
전체 소스는 기준 main의 기존 팀 기능을 포함하며 빌드 진입점은 루트 `android/`다.

## 이번 요구 반영

- 테스트 기기 정보: 사용자 확인 Galaxy S25 / Android 17 / One UI 9.0. 이 환경에서 직접 기기 검증한 것은 아니다.
- 교육 기준: Galaxy A16급 일반 보급형 Galaxy의 Android 14 / One UI 6 계열 기본 조작. 특정 최신 기종 판매 순위나 완전히 동일한 화면을 주장하지 않는다.
- 최신 One UI에서 빠른 설정을 오른쪽 위에서 내리는 방식과 이전 One UI의 두 번 내리기 방식을 함께 설명한다.
- 모든 학습을 `배우기(그림·글·TTS) → 따라 연습 → 별도의 최종 실습 → 완료`로 분리했다.
- 번호가 필요한 학습은 사용자가 지정한 `010-6344-2138`로 통일했다. 실제 앱 연결에도 이 번호를 사용한다. 공개 저장소에 반영할 때 이 개인 연락처 포함 여부를 팀과 확인한다.
- 소리는 옆면 음량 버튼 위치/위·아래 역할부터 교육한다. 밝기, 글자 크기의 설정 경로와 선택 이유를 설명한다.
- 글자 크기는 현재 값 유지도 정상 선택이다. 글자 크기 변경을 강제하지 않는다.
- 실제 전화·문자 발송, 연락처 저장은 사용자가 해당 기본 앱에서 직접 실행한다.

## 화면과 학습 흐름

홈 → IT 기능 배우기 → 스마트폰 교육 → 1주차 → 학습 선택.
6개 주차 분류와 2~6주차 준비 중 표시, 기존 키오스크 진입은 유지했다.

각 학습은 교육 카드 3장을 가진다. 그림은 자체 Compose Canvas 개념도이며 삼성 실제 스크린샷이 아니다. TTS로 설명을 반복해서 들을 수 있다. 영상은 이번 버전에 포함하지 않았다.

1. **밝기·소리·글자 크기**: 버튼 위치 → 밝기 설정 → 글자 미리보기/유지 선택 교육. 따라 연습과 최종 실습에서 실제 기기 또는 가상 연습을 선택한다.
2. **전화 걸고 받기**: 숫자판 → 발신/종료 → 수신 방법 교육. 따라 연습은 가상 숫자판과 통화 화면이다. 최종 실습은 실제 전화 앱 또는 가상 실습을 선택한다.
3. **연락처 저장하고 찾기**: 추가/저장 위치 → 이름/번호 입력 → 검색 교육. 최종 실습에서 실제 연락처 작성·목록 앱 또는 가상 실습을 사용한다.
4. **문자 보내고 받기**: 대화 읽기 → 수신자/내용 → 답장 확인 교육. 최종 실습에서 기본 SMS 앱 또는 가상 실습을 선택한다. 카카오톡 교육과는 구별한다.

실제 수신 실습에는 상대의 협조가 필요하다. 상대가 통화/답장할 수 없으면 실제 실습을 완료했다고 기록하지 않고 돌아와 가상 최종 실습을 선택할 수 있다. 상대에게 자동으로 연락하지 않는다.

## 실제 기기 조작과 완료 판정

| 기능 | 실행 방법 | 완료 근거 |
| --- | --- | --- |
| 음량 | 화면이 활성화된 동안 Activity의 volumeControlStream을 STREAM_MUSIC으로 설정. 사용자가 실제 음량 버튼 조작 | 미디어 음량 값이 증가·감소했는지 확인. 키 입력 자체를 가로채거나 조작 경로를 증명하지 않음 |
| 밝기 | ACTION_DISPLAY_SETTINGS로 디스플레이 설정을 열거나 빠른 설정창에서 조작 | 연습 시작 때 저장한 SCREEN_BRIGHTNESS와 돌아왔을 때 값을 비교. 자동 밝기도 영향을 줄 수 있음을 표시 |
| 글자 크기 | 디스플레이 설정에서 글자 크기와 스타일을 찾아 조작하거나 현재 크기 유지 | 사용자가 선택 확인. 실제 글꼴 값 변경은 완료 조건이 아님 |
| 전화 | ACTION_DIAL, 지정 tel URI | 앱 실행 뒤 사용자가 발신·수신 수행을 각각 확인. 실제 통화 기록을 읽지 않음 |
| 연락처 | ACTION_INSERT로 이름·번호 작성 화면, ACTION_VIEW로 목록 열기 | 사용자 저장·검색 확인. 기기의 연락처 DB를 읽거나 직접 쓰지 않음 |
| 문자 | ACTION_SENDTO + smsto URI + 안녕하세요 초안 | 사용자가 직접 전송·답장 읽기 확인. SMS 본문/수신함을 읽지 않음 |

실제 통화/문자 앱을 열었다는 이유만으로 성공 처리하지 않는다. 기기가 지원하지 않는 Intent는 실패 안내 후 가상 실습을 선택할 수 있다. 연락처 목록 열기가 지원되지 않으면 해당 실제 연습은 완료하지 않고 가상 경로로 돌아갈 수 있다.
앱은 밝기·음량을 임의로 쓰거나 복원하지 않는다. 사용자가 마친 뒤 편한 값으로 맞추도록 안내한다. 화면을 떠나면 폴링을 멈추고 돌아왔을 때 값을 다시 읽는다. 음량 스트림 설정은 화면 종료 때 원래대로 돌린다.
새 CALL_PHONE, SEND_SMS, READ_SMS, READ_CONTACTS, WRITE_CONTACTS, WRITE_SETTINGS 권한은 추가하지 않았다. 기존 B파트의 접근성/오버레이도 사용하지 않는다.

## 가독성 원인과 수정

호스트의 Typography.bodyLarge 등에 텍스트 색이 검정으로 명시되어 있었다. 이 값이 버튼의 LocalContentColor(흰색)보다 우선해서 남색 버튼 안의 글씨까지 어두워졌다.
`LearningTheme.kt`에서 C파트 전용 Typography와 lightColorScheme을 적용하고 텍스트 색을 고정하지 않도록 했다. 주요 버튼은 남색 `#211C50` + 흰색, 일반 화면은 밝은 배경 + 어두운 본문이다. 공통 테마 파일을 고쳐 A/B 화면까지 바꾸는 방식은 사용하지 않았다.
텍스트/버튼 주요 쌍의 수치 대비를 확인했으며 실제 S25 화면·큰 글꼴·TalkBack은 별도 검수가 필요하다.

## 파일별 변경과 팀원 영향

경로는 저장소 루트 기준. v1까지 포함한 main 대비 변경이다.

| 파일 | 상태 / 설명 |
| --- | --- |
| `android/app/src/main/java/kr/silverringo/learning/LearningEntry.kt` | 기존 함수를 KioskLearningEntry로 이름 변경. 키오스크 내부 로직 유지 |
| `.../learning/LearningHub.kt` | 추가. 기존 LearningEntry(onExit,onResult) 인터페이스로 주제/주차/학습 선택. courseVersion/evidence 결과 저장 |
| `.../learning/LearningTheme.kt` | 추가. C 영역만 고대비·텍스트색 상속 수정 |
| `.../learning/phone/PhoneCurriculum.kt` | 추가. 6주차/1주차 데이터와 지정 번호 |
| `.../learning/phone/PhoneEngine.kt` | 추가. 가상 실습 상태·판정, 완료 근거 필드 |
| `.../learning/phone/PhonePracticeScreen.kt` | 추가. 내부 GuidedSimulationScreen으로 가상 실습을 제공. TTS와 상태 저장 |
| `.../learning/phone/PhoneCourseScreen.kt` | 추가. 배우기→연습→최종실습 분리, 최종 결과만 호스트 전달 |
| `.../learning/phone/PhoneTeaching.kt` | 추가. 교육 카드·자체 개념도 |
| `.../learning/phone/DevicePractice.kt` | 추가. 실제 음량/밝기 관찰, 기본 앱 연결과 사용자 완료 확인 |
| `.../learning/phone/PracticeEvidence.kt` | 추가. 지정 번호 상수와 판정 함수 |
| `android/app/src/test/java/kr/silverringo/learning/PhoneEngineTest.kt` | 추가. 가상 실습 8개 회귀 검사 |
| `android/app/src/test/java/kr/silverringo/learning/PracticeEvidenceTest.kt` | 추가. 실제값/사용자확인 판정 및 교육 데이터 4개 검사 |
| `android/app/src/androidTest/java/com/silverlingo/app/IntegratedAppTest.kt` | 공통 테스트 수정. 키오스크 진입 전 새 주제 선택 화면에서 버튼 클릭 1줄 추가 |
| `android/app/src/androidTest/java/com/silverlingo/app/SmartphoneLearningTest.kt` | 추가. 교육→가상 연습→최종 실습, 회전, 진도 분리, 준비 중 목록 기기 검사 |
| `README.md`, `docs/SMARTPHONE_WEEK1_*.md`, `docs/validation/smartphone-week1/` | 공통 README 경로 안내와 새 인수인계/검증 기록 |

**A 담당:** MainActivity/HomeScreen/공통 Typography 파일 수정 없음. 동일 LearningEntry 호출이 허브로 연결되며, C 안에서만 색상 테마를 덮어쓴다.
**B 담당:** 가이드·동행 WebView·서비스·권한 변경 없음. C의 실제 앱 연결은 각 교육 목표에 속하며 B의 실제 생활 과제 안내 엔진을 변경하지 않았다.
**D 및 결과 소비자:** 기존 키오스크 JSON 유지. 스마트폰 결과는 topicId와 문자열 lessonId로 분기해야 한다. 자동값 확인/자기보고/가상 수행을 구분해서 표시한다.
**테스트 담당:** 기존 키오스크 진입 테스트 1줄 변경 및 스마트폰 기기 테스트 추가. 이번에는 APK 컴파일까지 확인하고 연결 기기 실행은 하지 않았다.

## 결과/진도 계약

```json
{"schemaVersion":1,"courseVersion":2,"topicId":"smartphone","weekId":1,"lessonId":"settings","mode":"test","passed":true,"completionEvidence":"device_values_and_font_choice","hints":0,"mistakes":0,"actions":0}
```

completionEvidence:
- `device_values_and_font_choice`: 실제 소리/밝기 값 변화 + 글자 크기 선택 확인
- `user_confirmed_external_app`: 실제 앱에서 수행했다는 사용자 확인 (자동 전송 성공 아님)
- `simulation_final`: 가상 최종 실습 통과

교육/따라 연습만 마쳤을 때는 완료 이벤트를 보내지 않는다. actions는 가상 엔진 이벤트 수이며 실제 기기 조작 횟수로 해석하면 안 된다. 실제 기기 결과의 0은 조작 없음이 아니라 미계측을 의미한다.
SharedPreferences `sr_learning`의 `smartphone.week1.<lessonId>.done/.result`를 사용한다. 기존 키오스크 `done_N/last_result_N`은 그대로다. v1의 완료 진도는 지우지 않고, 새로 수행하면 courseVersion 2 결과로 갱신한다.
rememberSaveable로 화면 재생성 시 단계를 복원하도록 작성했다. 앱 데이터 삭제 후 복원이나 다른 기기와 동기화는 제공하지 않는다.

## 실행/반영

ZIP 전체를 별도 폴더에 풀고 Android Studio에서 루트 `android/`를 연다. JDK17 / SDK35 / Build Tools34.0.0 / 기존 Gradle8.7.

```powershell
cd android
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

macOS/Linux는 `bash gradlew` 사용. 소스 ZIP의 `smartphone-week1-v2.patch`는 기준 main에 대한 누적 패치다. v1 적용본 위에 다시 적용하는 패치가 아니므로 별도 깨끗한 기준에서 비교한다.

```bash
git apply --check smartphone-week1-v2.patch
git apply smartphone-week1-v2.patch
```

APK는 기존과 같은 applicationId com.silverlingo.app, versionCode1의 디버그 빌드다. 원격 jib는 변경하지 않았다. 이전 전달 APK와 서명이 같은지 검증 기록에 기재한다.

## 기기 검수 우선순위

1. S25에서 남색 버튼의 글자가 흰색으로 보이는지, 큰 글씨에서도 스크롤/폼을 사용할 수 있는지 확인.
2. 설정 교육 3장을 읽고 TTS를 들어 본 뒤 실제 연습에서 음량 올림/내림·밝기 변경을 확인.
3. 글자 크기를 바꾸지 않고 유지 선택으로 마칠 수 있는지 확인.
4. 따라 연습만으로 완료 저장되지 않고 최종 실습 후 저장되는지 확인.
5. 실제 전화/문자/연락처 앱 연결, 취소 후 복귀, 자동 완료되지 않는지 확인. 상대와 합의된 시점에 실제 발신·문자 실습 수행.
6. 기본 앱이 없거나 지원되지 않을 때 안내와 가상 실습 대체 경로 확인.
7. 기존 키오스크·B 가이드와 홈 왕복을 검수.

참고: Samsung Galaxy A16 업데이트 문서 https://doc.samsungmobile.com/SM-A165M/031416241120/kor.html ; Samsung One UI 9 안내 https://www.samsung.com/us/support/answer/ANS10013398/ ; Android Common Intents https://developer.android.com/guide/components/intents-common ; AudioManager https://developer.android.com/reference/android/media/AudioManager .
