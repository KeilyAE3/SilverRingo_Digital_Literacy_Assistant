# Team Sync 운영 기준

## 실행과 등록

현재 실제 실행 코드는 Tester/automation/check.py이며 Tester/config.json의 checks를 읽는다. 문서보다 실행 코드를 기준으로 판단한다. 테스트 자동 검색 방식이나 저장소 루트 automation/check.py는 사용하지 않는다.

- 기능 테스트: Tester/tests/test_<기능명>.py. 실제 함수를 import해 unittest로 정상·경계·실패 동작을 검사하고 test_ 메서드에 명세 docstring을 적는다.
- 등록: config.json에 name, module, test와 local/workflow의 enabled를 지정한다. include가 있으면 해당 메서드만, 생략하면 등록 파일 전체를 실행한다.
- 로컬: `python Tester/automation/check.py --mode local`
- CI 범위 재현: `python Tester/automation/check.py --mode workflow`
- Python 3.12+와 Node가 필요하다. HTTP 검사에는 `python -m pip install -e './SilverRingo_Chat-bot[api,test]'`가 필요하며 실제 Gemini 키는 쓰지 않는다.
- 결과: Tester/.team-sync/{results.json,summary.md,tests.log}. Git에서 제외한다.

모든 선택 검사를 실행하며 실패·생략·expectedFailure 또는 0개이면 종료 코드 1이다. local에는 기존 Example-01의 의도적 실패 2개가 포함된다. workflow는 기존 config의 선택 범위를 유지하며 예제는 원래 비활성이다. 실제 오류와 예제 실패를 구분하고 통과를 위해 검사를 삭제하거나 끄지 않는다.

현재 workflow는 학습 Node 엔진, 지식 적재, 선택된 챗봇 서비스, 새 HTTP 계약·안전한 오류를 검사한다. Android APK·Kotlin·기기·브라우저 검사는 별도 실행이며 CI 통과가 해당 검사의 자동 수행을 뜻하지 않는다. 새 의존성은 기존 프로젝트 파일과 공통 workflow의 설치 단계에 추가한다. 새 검사마다 workflow를 만들지 않는다.

## Push와 Discord

main push 또는 main의 수동 실행 → Team Sync → 검사 결과 artifact → Discord Sync Notification → 기존 Tester/automation/notify.py 순서다. 개발 브랜치 push는 현재 workflow 트리거가 아니다. main 가상 병합 검사는 없으며 이미 이뤄진 push를 실패 시 취소하지 않는다.

team-sync.yml은 해당 github.sha를 checkout한다. 알림은 같은 저장소의 main 실행 완료에만 반응하고 취소된 실행은 생략한다. 트리거 run/attempt의 artifact만 내려받으며 notifier는 결과 SHA를 확인한다. 전체 상태·개수·실패 상세 최대 2개·실행 링크를 전송한다. 긴 상세는 artifact에서 본다.

GitHub Secret DISCORD_WEBHOOK_URL이 필요하다. 값은 서버 초대 링크가 아닌 Discord 채널 Webhook이다. 현재 notify.py는 Secret이 없으면 종료 코드 1로 실패한다(성공 처리/조용한 생략이 아님). 전송 성공은 Discord 메시지 생성 응답의 id까지 확인한다. Secret 값은 로그·Git에 적지 않는다. 두 공통 workflow와 notifier를 보존한다.

2차 통합의 실행 결과와 기기 검증 제한은 [릴리스 기록](../../docs/RELEASE_1_1_1.md)을 따른다.
