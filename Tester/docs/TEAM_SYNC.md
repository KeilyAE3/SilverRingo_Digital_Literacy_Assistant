# Team Sync: 최소 사용 안내

## 목적과 범위

현재 작업 파일의 Python 기능 테스트를 모두 실행합니다. 로컬과 GitHub에서 같은 명령을 씁니다.
문서 링크, 간트 차트, 계약 JSON, 변경 보고서 검사는 없습니다. config.json도 없습니다.
등록한 테스트가 다루는 동작만 검증합니다. 테스트 성공이 프로그램 전체의 무결성을 보장하지는 않습니다.
main과의 가상 병합 검사는 이번 판에서 제외했습니다. 각 브랜치 push는 그 브랜치,
main push는 통합된 main을 검사합니다. 실패해도 이미 이루어진 push를 취소하지 않습니다.

## 로컬 실행

저장소 루트에서 Python 3.12로 실행:

```sh
python automation/check.py
```

결과는 콘솔과 `.team-sync/`에 생성됩니다.

| 결과 파일 | 용도 |
|---|---|
| tests.log | 모든 테스트 이름, 설명, 성공/실패, 실패 원인 traceback |
| summary.md | 전체 상태와 개수 |
| results.json | Discord 전달용 결과 |

모두 통과하면 종료 코드 0. 실패·생략·expectedFailure가 있거나 테스트가 0개이면 종료 코드 1입니다.
0개는 `not_configured`입니다. 시범 테스트로 성공처럼 꾸미지 않기 위해 실제 기능 테스트는 동봉하지 않았습니다.
`failure_events`는 실패 사건 수입니다. 한 테스트의 여러 subTest 실패가 각각 집계될 수 있습니다.
모든 테스트를 수집해서 실행하며, 일반적인 assertion 실패 후에도 다음 테스트를 계속합니다.
무한 대기나 프로세스 강제 종료 시에는 완전한 보고서가 없을 수 있습니다. GitHub 실행 제한은 10분입니다.

## 기능을 추가하는 에이전트의 작업

1. 실제 기능 코드를 작성하거나 수정합니다.
2. `tests/test_<기능명>.py`에 `unittest.TestCase`를 작성합니다.
3. `test_`로 시작하는 메서드마다 요구사항과 기대 결과를 docstring으로 적습니다.
4. 실제 함수를 import해서 정상 입력, 경계값·잘못된 입력, 필요한 모듈 간 연결을 검증합니다.
5. 위 전체 실행 명령으로 기존 기능까지 확인하고 코드와 테스트를 함께 커밋합니다.

새 테스트 등록을 위해 check.py나 워크플로우를 수정하지 않습니다.
초기에는 모든 테스트 파일을 tests/ 바로 아래에 두세요. 하위 폴더를 만들면 각 폴더에 __init__.py가 필요합니다.
외부 API·비밀키·인터넷 없이 반복 가능한 테스트를 우선하며 외부 호출은 unittest.mock으로 대체할 수 있습니다.
필요한 실제 모듈 연결은 mock 대신 실제 객체를 연결한 테스트로 검증합니다.

다음은 형식 설명이며 실제 모듈 이름과 기대값으로 바꿔야 합니다:

```python
import unittest
from your_module import calculate_progress

class ProgressTests(unittest.TestCase):
    def test_quarter_completed(self):
        """진행률: 4개 중 1개를 완료하면 25를 반환한다."""
        self.assertEqual(calculate_progress(completed=1, total=4), 25)
```

## Push와 Discord

team-sync.yml → check.py → 결과 artifact → discord-notify.yml → notify.py → Discord.
테스트마다 별도 workflow를 만들지 않습니다. 두 workflow는 공통으로 유지합니다.
Discord에는 전체 상태·테스트 수·실패 테스트 이름 최대 3개·GitHub 실행 링크를 보냅니다.
성공한 개별 테스트의 이름과 설명, 긴 오류 원문은 artifact의 tests.log에서 확인합니다.
작성자 GitHub 이름은 표시하지만 사용자 매핑·멘션·DM은 이번 판에 포함하지 않았습니다.
취소된 실행은 알림을 생략하며, 결과 artifact가 없으면 실행 로그 확인 안내를 보냅니다.

설정: GitHub 저장소 Settings → Secrets and variables → Actions에
`DISCORD_WEBHOOK_URL`을 등록합니다. 값은 Discord 채널의 Webhook URL입니다. 서버 초대 URL이 아닙니다.
알림 workflow와 automation/notify.py는 기본 브랜치 main에도 있어야 합니다.
Webhook이 없으면 테스트는 실행하고 알림만 생략합니다.

의존성이 생기면 앱의 기존 의존성 파일을 사용하고 team-sync.yml의 테스트 실행 앞에
설치 단계(예: `python -m pip install -r requirements.txt`)를 추가하세요.
지금은 표준 라이브러리만 사용하므로 setup_commands나 별도 의존성 파일이 없습니다.

## 이전 판에서 교체하기

이 패키지는 앱 전체가 아니라 자동화 교체 파일입니다. GitHub 원격 또는 PC 파일은 자동 수정되지 않습니다.
먼저 기존 변경사항을 커밋하거나 백업하세요. 압축 안의 폴더들을 저장소 최상위에 배치합니다.

- 두 workflow와 automation/check.py를 새 파일로 교체하고 automation/notify.py를 추가/교체합니다.
- 기존 실제 앱 코드와 실제 기능 테스트는 보존합니다. 실제 테스트가 있다면 tests/로 옮기고 import를 맞춥니다.
- 이전 자동화 전용 config.json, collaboration.py, run.py, unittest_runner.py, schedule.json,
  discord-map.json, CONFIG_REFERENCE.md, automation/tests/는 이 판에서 사용하지 않습니다.
  변경 여부를 확인한 후 제거할 수 있습니다.
- 이전에 제공한 examples/learning_demo와 자동화 전용 docs/contracts, reports, schedule, templates,
  testing 등의 문서는 실행에 필요 없습니다. 팀이 작성한 내용이 있으면 보존하고 재사용 여부를 결정하세요.
- 기존 AGENTS.md와 .github/copilot-instructions.md에 남아 있는 구버전 등록·계약·보고서 의무를
  제거하고, 아래 문장으로 안내를 통일하세요. 다른 프로젝트 지침은 유지합니다.

> 기능을 추가·수정할 때 docs/TEAM_SYNC.md를 읽고 tests/test_<기능명>.py에 실제 동작 테스트를 추가한 뒤 python automation/check.py로 전체 검사를 실행한다.

- .gitignore에 `.team-sync/`, `__pycache__/`, `*.pyc`를 추가합니다. 기존 .gitignore를 통째로 덮어쓰지 않습니다.
- files/나 examples/ 아래에 새 automation 폴더를 넣지 않습니다.

최초 실행에서 테스트 미등록이 나오는 것은 의도된 동작입니다. 실제 기능 테스트를 추가하면 전체 실행에 자동 포함됩니다.

## 실행 규칙

- 실행 명령: python Tester/automation/check.py
- 실제 기능 테스트 위치: Tester/tests/test_<기능명>.py
- 예시와 예시 테스트 위치: Tester/example/
- 검사 등록: Tester/config.json의 checks에 name, module, test 추가
- 결과 위치: Tester/.team-sync/
- 기능 개발 시 작업: 기능 코드 수정 → 테스트 작성 → config 등록 → 전체 검사
- 문서 링크 검사는 추가하지 않음