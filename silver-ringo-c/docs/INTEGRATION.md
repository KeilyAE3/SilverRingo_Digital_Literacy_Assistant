# C파트 인터페이스와 상태 계약

## 책임 구분

`content.js`는 레벨의 시작/끝 화면과 검사 키를 정의한다. `engine.js`의 reducer는 주문 조작을 처리하고, evaluator는 지정 목표에 맞는지 검사한다. 화면별 정답을 클릭했는지와 주문이 성공적으로 끝났는지를 혼동하지 않는다. 전체 실전에서는 최종 주문 내역·장소·결제 방식으로 판정한다. 잘못 고른 뒤 수정해서 목표를 달성하면 통과할 수 있다. 요청한 힌트 수와 조작 횟수를 기록하지만 '오류 횟수'로 과장하지 않는다.

학습 레벨은 1:시작/장소, 2:메뉴 찾기, 3:옵션/담기, 4:장바구니/주문 확인, 5:결제, 6:전체다. 2~5의 앞선 상태는 fixture이며 평가 대상에 포함하지 않는다. 잠금 해제는 해당 레벨의 `passed=true` 완료만 사용한다. 자유 실습 결과는 잠금에 사용하지 않는다.

## Native → Web

로컬 자산 URL: `https://appassets.androidplatform.net/assets/kiosk/index.html?embed=1&level=1`

`level`은 1~6 정수다. 네이티브 목록에서 잠금을 검사한 뒤 연다. embedded 화면은 완료 후 네이티브 목록으로 돌아오며 다음 레벨을 웹에서 임의 실행하지 않는다. `WebViewAssetLoader`가 해당 APK 자산만 제공한다. INTERNET 권한은 사용하지 않는다.

## Web → Native

`window.SilverRingoNative.postMessage(JSON.stringify(message))` 사용. 주입 객체의 origin과 메인 프레임을 제한하며, 임의 자바스크립트 인터페이스를 노출하지 않는다.

| type         | 필드           | 의미                                 |
| ------------ | -------------- | ------------------------------------ |
| `speak`      | `text`         | 한국어 안내 발화 요청 (최대 1,000자) |
| `stopSpeech` | 없음           | 현재 음성 중단                       |
| `result`     | 아래 결과 필드 | 완료한 레벨의 평가 결과              |
| `exit`       | 없음           | A에서 연결한 학습 목록으로 돌아가기  |

결과 예시 (시간과 조작 수는 예시):

```json
{
  "type": "result",
  "schemaVersion": 1,
  "topicId": "kiosk",
  "levelId": 6,
  "mode": "test",
  "passed": true,
  "failures": [],
  "hints": 0,
  "actions": 10,
  "elapsedSeconds": 85,
  "completedAt": "2026-10-24T11:00:00.000Z"
}
```

네이티브는 자신이 실행한 levelId와 일치하는 결과만 처리한다. 결과는 로컬 학습 피드백이며 인증·결제·증빙 API가 아니다. `elapsedSeconds`는 내부 기록용으로만 사용하고 시간 점수/제한으로 사용하지 않는다.

## 저장

웹: `silver-ringo-c-v3` localStorage, version=1. `completed`는 누적 완료 레벨, `results`는 최근 120회, `session`은 진행 중 상태. 저장 제한/차단 시 화면에 알리고 현재 실습은 계속 가능하다. 서버 저장이나 다른 브라우저와의 동기화는 없다.

Android: `sr_learning` SharedPreferences의 `done_N`, `last_result_N`. WebView 내부 저장과 별개로 네이티브 완료 목록을 관리한다. 상태 소유권은 웹 실습 reducer / Android 레벨 완료로 한정한다. Android 실습 화면의 프로세스 종료·회전 후 정밀한 세션 복원은 후속 작업이다.

## B파트와 향후 공유할 계약 제안 (현재 미구현)

`StepDefinition(id, instruction, targetKey, nextStepId)`와 `GuideAction(repeat, previous, retry)`를 양쪽 팀이 합의할 수 있다. B는 실제 화면의 접근성/작업 관찰을, C는 자체 reducer의 이벤트와 상태를 검사한다. DOM 선택자나 C의 상품 가격 규칙을 공통 엔진에 넣지 않는다.

## 네이티브 전환

현재 `OrderState`, `OrderAction`, `GoalSpec`, `LearningResult`의 필드를 Kotlin data class/sealed interface로 옮기고 순수 reducer/evaluator부터 동일 테스트 시나리오로 검증한다. 이후 WebView 화면을 Compose로 대체한다. 브리지는 제거할 수 있지만 topicId/levelId/결과 계약은 유지한다. 이 전환은 별도 작업과 확인이 필요하다.
