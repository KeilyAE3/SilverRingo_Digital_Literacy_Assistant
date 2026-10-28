# Team Sync 운영 분석 및 통합 앱 검사

분석 기준: 업로드 전 `origin/main` `4a3b52ee2a6ab85df99d7f4922f2f752b8bab3da`.
`Tester/docs/TEAM_SYNC.md`, config, check.py, notify.py, example 전체, 두 workflow를 직접 읽었다.
문서와 충돌하면 **현재 실행 코드 및 Actions YAML**을 기준으로 한다.

## 실제 등록 및 선택 규칙

- `Tester/tests/test_<기능>.py`에 unittest.TestCase와 `test_` 메서드·명세 docstring을 작성한다.
- `Tester/config.json`의 checks에 name/module/test와 **local/workflow 각각의 설정 객체**를 등록한다.
  module은 저장소 루트 기준 파일 경로, test는 Tester/ 기준 경로다.
- check.py는 module 파일의 존재만 확인한다. module을 자동 실행·번역·검증하지 않는다.
  테스트 파일을 `Tester.…` import 경로로 로딩하므로 테스트가 실제 코드를 호출해야 한다.
- 등록·활성화된 테스트 파일만 실행한다. Git 변경 파일, 확장자, diff, 브랜치 담당자로 자동 선별하지 않는다.
- enabled=false이면 파일 존재 확인과 import도 생략한다. include 생략은 해당 파일 전체,
  include=[]는 실행 없음, include 목록은 지정 메서드를 지정 순서로 실행한다.
  선택 제외는 Skipped로 집계되지 않는다.
- 저장소/Tester 바깥 경로, 테스트 파일 중복, 누락 파일·메서드, import 실패, 모호한 메서드 선택은 설정 오류다.

## local / workflow 차이

두 모드 모두 **현재 체크아웃/작업 파일**을 검사한다. main과 가상 병합하지 않는다.
기본값 local, `--mode workflow`는 config의 workflow 선택을 사용한다. 로컬에서 workflow 모드를 실행해도
GitHub에서 실행되는 것은 아니다. Actions는 checkout한 github.sha로 실행한다.

기존 Example-01은 local에서 4개(정상 2, 의도된 assertion 실패 2), workflow에서 정상 2개만 실행한다.
Example-02는 두 모드 모두 정상 3개다. **이 기존 설정은 변경하지 않았다.**
의도된 실패는 unittest.expectedFailure가 아니라 틀린 기대값을 사용한 실제 assertion 실패다.

이번 등록: `Tester/tests/test_learning_engine.py` 2개를 두 모드 모두 활성화했다.

1. Node subprocess로 기존 `silver-ringo-c/tests/engine.test.cjs`의 실제 JS 엔진 회귀 검사 11개 실행.
2. 웹 content.js의 실제 데이터와 Android 목록용 content.json의 구조/값 일치 검사.

Python으로 Kotlin을 흉내 내지 않는다. Node 누락, 시간 초과, 하위 검사 실패는 검사 실패로 노출한다.
Actions에는 Node 22 설치 단계만 추가했다. check.py, notify.py, 기존 예제, Discord workflow는 보존했다.
Team Sync의 2개 등록 테스트 중 첫 번째가 11개 JS 검사를 묶으므로 두 카운트를 합산해 과장하지 않는다.

## 실행·실패·결과 파일

`python Tester/automation/check.py`와 `python Tester/automation/check.py --mode workflow`.
로컬 Python 3.14, Actions Python 3.12 / Node 22 (로컬 Node 24.14.1).

매 실행은 `Tester/.team-sync/{results.json,summary.md,tests.log}`를 새로 생성한다.
마지막 실행이 이전 결과를 덮어쓰므로 모드별 증거를 `docs/validation/team-sync-local/`,
`docs/validation/team-sync-workflow/`에 별도 보존했다. 기존 tracked 결과는 원래 파일을 보존하고
생성 파일 변경을 커밋하지 않는다. .gitignore만 추가해도 이미 tracked된 파일은 추적이 계속된다.

상태 passed는 테스트 1개 이상, unittest 성공, skip=0, expectedFailure=0인 경우뿐이다.
assertion/error/unexpectedSuccess/skip/expectedFailure는 실패(exit 1), 0개는 not_configured(exit 1).
설정·import 오류도 failed 결과를 생성한다. failure_events는 테스트 수가 아니라 실패 사건 수이며
subTest 여러 실패는 따로 집계될 수 있다. 일반 assertion 후에도 다른 검사를 계속 실행한다.

로컬 실제 결과:

| 모드 | 총 등록 검사 | 통과 | 실패 | 종료 코드 | 원인 |
| --- | ---: | ---: | ---: | ---: | --- |
| local | 9 | 7 | 2 | 1 | 기존 test_progress_fail_demo(25≠50), test_remaining_fail_demo(3≠2) |
| workflow | 7 | 7 | 0 | 0 | 앱 엔진/콘텐츠 2 + 기존 예제 5 |

위 local 실패는 통합 앱 오류가 아니다. 원인 불명 가이드 버그와도 관련이 없다.
실패를 없애려고 예제나 검사 설정을 수정하지 않았다.

## GitHub Actions 실행 조건과 확인

Team Sync는 **main Push 또는 workflow_dispatch**에서 실행하고 job도 refs/heads/main일 때만 실행한다.
hjh/Juno/jib Push에는 이 workflow가 실행되지 않는다. paths 필터는 없다.
Ubuntu, Python 3.12, Node 22로 workflow 모드 검사, 최대 10분.
항상 upload-artifact를 시도하며 `team-sync-{run_id}-{run_attempt}`에 결과 세 파일을 저장한다.
보존 기간은 14일, include-hidden-files=true다. 실패해도 Push를 취소하거나 코드를 되돌리지 않는다.
커밋 메시지에 CI 생략 지시를 넣지 않는다.

저장소 Actions → Team Sync → 해당 SHA의 실행 → test job / Summary에서 결과를 보고,
Artifacts에서 해당 run_id/attempt 파일을 내려받아 상세 traceback을 확인한다.
원격 실행 결과와 링크는 검증 완료 후 이 문서 아래에 기록한다.

## Discord

Discord Sync Notification은 Team Sync 완료 workflow_run(브랜치 main)을 받아 실행한다.
동일 저장소의 실행만 처리하고 cancelled는 제외한다. main에서 알림 코드를 checkout하며
정확한 run_id와 attempt의 artifact를 받는다. 다운로드 오류여도 알림 단계는 실행한다.
notify.py는 JSON 크기(2MB), 형태 및 결과 SHA 일치를 검사한다.
실패 시 상세 결과 확인 불가 메시지와 Actions 링크를 보낸다.
메시지는 전체 요약과 미통과 항목 최대 2개의 명세/마지막 오류 줄, 나머지 개수, Actions 링크를 포함한다.
allowed_mentions를 비워 멘션을 하지 않는다.

DISCORD_WEBHOOK_URL은 저장소 Actions Secret이어야 한다. 값은 출력·커밋하지 않는다.
현재 notify.py는 Secret이 없으면 exit 1로 **알림 job 실패**다. HTTPS discord.com/api/webhooks/만 허용한다.
wait=true로 응답받은 **Discord 메시지 id가 있어야 성공(exit 0)**이다.
알림 workflow의 Send result 단계 로그에서 `Discord 전송 성공: message_id=…`를 확인해야 전송 성공으로 판정한다.
Actions 성공만으로 사람이 Discord에서 읽었거나 앱이 정상이라는 뜻은 아니다.

## 기존 문서와 다른 점

| TEAM_SYNC.md의 과거 설명 | 현재 코드/YAML |
| --- | --- |
| config.json 없음, tests 자동 수집, 등록 시 check/workflow 수정 불필요 | config.json 등록 필수. checks에 등록된 파일만 로딩 |
| python automation/check.py, tests/ | 실제 경로는 Tester/automation/check.py, Tester/tests/ |
| 각 브랜치 Push에서 검사 | main Push만 검사; main 가상 병합 없음 |
| 실제 테스트·시범 테스트 미동봉 | Example-01/02 및 의도된 실패 예제 동봉 |
| Discord 실패 테스트 최대 3개 | notify.py는 미통과 2개의 상세 + 나머지 개수 |
| Webhook 없으면 알림 생략 | 현재 notify.py는 exit 1로 알림 실패 |
| setup_commands/config 없음 | setup_commands는 없지만 선택용 config.json은 있음 |

문서 맨 아래 실행 규칙의 config 등록/Tester 경로 설명이 코드에 더 가깝다.
원본 문서를 임의 삭제하지 않고 차이를 이 문서에 기록했다. 담당자가 안내를 통일할 필요가 있다.

## 검증 범위와 개선사항

기존 Python 예제 5개는 앱을 검증하지 않는다. 새 등록 2개는 실제 JS 주문 엔진/콘텐츠 연결만 검증한다.
Team Sync에는 Android SDK·JDK·Gradle·브라우저 설치 및 실행 단계가 없다.
Kotlin 컴파일·Manifest 병합·APK 패키징·WebView 실제 메시지·Compose 화면·접근성 권한·오버레이·TTS,
똑닥 앱/외부 웹 호환성은 이 Actions 성공으로 검증되지 않는다.

로컬 별도 APK 빌드/Kotlin/브라우저 결과는 ANDROID_VALIDATION.md에서 확인한다.
사용자는 APK 설치·앱 실행 성공을 확인했으나 일부 가이드 버그는 원인 미확정이다.
증상/재현 순서/기기·Android·WebView·똑닥 버전/logcat을 모아 원본 Juno와 통합 앱을 비교해야 한다.
이번 반영에서 근거 없는 가이드 엔진 수정은 하지 않았다.

향후 담당자 합의 후 Android JDK17/SDK35 빌드·단위 테스트 CI 및 기기/에뮬레이터 검사를 추가하고,
별도로 브라우저 회귀 검사 설치·실행 단계를 마련할 수 있다. 현재 파이프라인 보존을 위해 이번에 확대하지 않았다.
tracked `.team-sync`/pyc 정리와 TEAM_SYNC.md의 과거 안내 정리는 별도 변경으로 권장한다.

## main 업로드 및 실제 원격 결과

업로드 전: main protected=false, 적용된 branch rules=[]를 읽기 전용 API로 확인했다.
사용자가 APK 설치·실행을 확인하고 이번 main 반영·Commit·Push를 명시적으로 승인했다.
main 외 원격 브랜치에는 Push하지 않으며 보호 정책 우회/강제 Push를 하지 않는다.
### 실제 Push 및 실행 결과

- 통합 코드 커밋(로컬 hjh): `433b7a79521746fa3fa163e9975d16b37fcf1c3f`.
- main 병합/첫 Push SHA: `4acba96266fb75e6df4561658558eb96bddf8ed5`.
- [Team Sync 실제 실행](https://github.com/KeilyAE3/SilverRingo_Digital_Literacy_Assistant/actions/runs/40649420675):
  push 이벤트, completed/success. Python 3.12.15·Node 22.23.3에서 workflow 검사 7/7 통과,
  failure_events=0, skipped=0, expected_failures=0.
- [결과 artifact](https://github.com/KeilyAE3/SilverRingo_Digital_Literacy_Assistant/actions/runs/40649420675/artifacts/12799151573):
  `team-sync-40649420675-1`, 세 파일 생성·업로드·다운로드 확인. results.json의 sha가 위 main SHA와 일치한다.
- [Discord 알림 실행](https://github.com/KeilyAE3/SilverRingo_Digital_Literacy_Assistant/actions/runs/40649442529):
  workflow_run 이벤트, completed/success. 정확한 artifact 다운로드 성공 후
  `2026-10-28T07:20:38Z` (한국 16:20:38)에 `Discord 전송 성공: message_id=1564901693009825884` 확인.
- 증거: [실제 원격 결과 JSON](validation/github-main-4acba96/results.json),
  [원격 summary](validation/github-main-4acba96/summary.md),
  [원격 tests.log](validation/github-main-4acba96/tests.log),
  [Team Sync job 로그](validation/github-team-sync.log),
  [Discord 전송 로그 발췌](validation/github-discord-evidence.log).

실패 분류: 통합 코드 오류는 이번 자동 검사 범위에서 발견되지 않았다.
local 실패 2개는 의도된 예제, 기기 테스트 실행 불가는 기기 미연결 환경 문제다.
기기 가이드 버그는 원인 미확정으로 남으며 위 CI가 검증하지 못하는 범위다.
Actions에는 Node 20 기반 기존 actions의 Node 24 강제 실행/deprecation 및 punycode 경고가 있었다.
실행은 성공했으나 공통 workflow 담당자가 action 버전 갱신을 별도 검토할 필요가 있다.

이 원격 결과 기록은 문서 후속 커밋으로 main에 추가하며 CI 생략 지시 없이 다시 Push한다.
후속 문서 커밋은 Android/JS/검사 로직을 변경하지 않는다. 최신 실행은 Actions에서 해당 SHA로 확인한다.
원격 hjh=`28772a8…`, Juno=`0025219…`, jib=`f011f17…`는 원래 상태임을 Push 후 확인했다.
