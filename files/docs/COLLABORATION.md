# Discord 중심 협업과 GitHub 자동화

## 운영

- 업무 분배·설계 논의·주간 회의: Discord. Issues·Projects·Insights 입력 의무 없음.
- 브랜치: `feat/<owner>/<topic>`, `fix/<owner>/<topic>`, `integration/<sprint>`, `release/<demo>`.
- 담당자는 GitHub 로그인 등 합의한 식별자를 사용. 현재 친구들의 계정·모듈 역할은 등록하지 않았음.
- `main`: 모든 관련 기능이 동기화된 스프린트 완성본. 부분 시연은 `release/<demo>`. `integration/<sprint>`에서 먼저 합치고 검사하면 main에 미완성 통합본을 올리는 일을 줄일 수 있음.
- PR·팀장 승인 없이 직접 push/merge 가능. GitHub는 merge commit도 push로 감지하므로 같은 자동 검사가 실행됨.
- main에 올릴 때 해당 integration 커밋의 Actions 결과와 Discord 합의를 확인. main 반영 뒤에도 재검사됨.
- 통합된 main을 진행 중인 작업 브랜치에 정기적으로 반영. CI는 각 브랜치를 검사하며 네 사람의 브랜치를 자동으로 합치지 않음.

## 지금 자동으로 실행되는 검사

| 대상 | 동작 | 한계 |
|---|---|---|
| 모든 브랜치 push | Team Sync 실행 | 로컬 commit만으로 실행되지 않음 |
| 설정 파일 | 실제 모듈 경로·검사 명령 형식 검사 | 현재 앱 모듈 없음 |
| 변경 보고서 | 필수 정보, 파일 경로, 계약 참조 검사 | 내용의 사실성은 자동 증명하지 않음 |
| 등록된 모듈 변경 | 같은 push에서 바꾼 보고서가 변경 파일들을 포함하는지 확인 | 최초 브랜치 push·수동 실행은 변경분 정책 생략, 전체 문서 형식은 검사 |
| 계약 | 제공자·소비자 모듈 등록 여부, 입출력·오류·호환성 정보 검사 | 실제 통신·타입 호환성 테스트는 별도 등록 필요 |
| Markdown | 저장소 내부 파일 링크 검사 | 외부 URL·anchor 존재 여부는 제외 |
| 일정 | 날짜·선후 관계·순환 의존 및 생성 문서 최신성 검사 | 자동 일정 재배치 없음, 달력 일수 기준 |
| 앱 검사 | 설정된 모듈 명령·통합 명령 실행, 오류 시 실패 | 설정 전에는 NOT CONFIGURED로 명시 |
| 결과 전달 | Actions Summary와 14일 보관 Markdown artifact | 결과를 문서로 자동 커밋하지 않음 |
| Discord | 검사 실패 또는 main 검사 성공 알림 | Secret 등록 전 SKIP, 일반 작업 브랜치 성공은 생략 |

push 후 검사이므로 실패한 커밋을 거부하거나 롤백하지 않음. 검사 실패는 빨간 실행 결과와 알림으로 드러남.
NOT CONFIGURED가 있어도 협업 검사 자체가 성공할 수 있으며, 이를 앱 완성·모듈 통합 성공으로 해석하면 안 됨.

## 팀원 사용 순서

1. Discord에서 작업·연결 지점을 합의하고 최신 main에서 작업 브랜치 생성.
2. AI가 [AGENTS.md](../AGENTS.md)를 읽게 설정.
3. 필요한 경우 [계약 템플릿](templates/contract.json)을 `docs/contracts/<id>.json`으로 복사해 제공자·소비자와 합의한 현재 명세 작성.
4. [보고서 템플릿](templates/report.json)을 `docs/reports/<owner>/<task>.json`으로 복사. 논리적인 작업 단위로 갱신. 모든 디버깅 커밋에 새 보고서를 만들 필요는 없음.
5. `files`에는 같은 push에서 변경하는 모듈·계약 파일 경로를 나열. 삭제된 파일도 나열 가능. `contracts`에는 현재 존재하는 계약 JSON 경로를 나열.
6. 로컬 검사 후 push. 보고서도 같은 push에 포함. 테스트 결과·검증 못한 부분·다음 작업을 정확히 기록.
7. Actions의 해당 실행에서 검사 결과와 NOT CONFIGURED 항목 확인. 연결 문제가 있으면 Discord에서 상대 담당자와 조율.

```bash
python -m unittest discover -s automation/tests -v
python automation/check.py
```

검사는 커밋 SHA를 자동 기록하므로 자기 자신의 커밋 SHA를 보고서에 미리 넣을 필요 없음.
보고서는 구현 선택·근거·영향을 간결하게 설명하며, AI의 내부 사고 과정을 수집하지 않음.

## 앱이 생긴 뒤 실제 검사 등록

[설정](../automation/config.json)의 `modules`에 `id`, `paths`, `commands`를 등록.
아래는 형식 예시이며, 실제 경로·스택을 결정한 뒤에만 적용할 것.

```json
{
  "modules": [
    {"id": "backend", "paths": ["backend"], "commands": [["python", "-m", "unittest", "discover", "-s", "backend/tests"]]}
  ],
  "integration_commands": [["python", "-m", "unittest", "discover", "-s", "tests/integration"]]
}
```

명령은 repository root에서 실행하는 인자 배열. `cd ... && ...` 같은 shell 문자열은 쓰지 않음.
의존성 설치, Node·Java·Flutter 등 런타임, 테스트용 DB가 필요하면 `.github/workflows/team-sync.yml`에 setup/install/services를 추가.
현재는 Python 3.12 표준 라이브러리만 필요하며 앱 의존성을 자동 추측하거나 설치하지 않음.
검사는 등록된 모든 모듈과 통합 명령을 실행. 명령당 최대 5분, 전체 job 최대 15분.
통합 명령에는 실제 API 요청/응답, 오류 처리, 제공자·소비자 간 호환성, 핵심 사용자 흐름 검증을 등록.
앱 소스가 생겼는데 등록하지 않으면 모듈 변경 보고 강제 검사도 적용되지 않으므로 첫 앱 커밋에서 등록해야 함.

## 간트 일정 시범 사용

[일정 데이터](../automation/schedule.json)의 각 작업에 `id`, `title`, `owner`, `start`, `days`, `after`, `status` 작성.
상태는 `todo`, `active`, `done`. `after`는 선행 작업 id 배열. 선행 종료일 이후로 시작일을 직접 맞춤.
처음에는 자동화 구축 작업 하나만 기록했으며 앱 일정이나 팀원별 역할은 추측하지 않았음.

```bash
python automation/check.py --write-schedule
```

데이터와 갱신된 [간트 문서](schedule/TEAM_GANTT.md)를 함께 push. 네 명이 하나의 파일을 동시에 수정하기보다 회의 후 일정 담당자가 합의된 일정만 반영하는 방식 권장.

## Discord 알림 활성화

1. 원하는 Discord 채널의 설정 → 연동/Integrations → Webhooks에서 webhook 생성. 서버·채널 권한 필요.
2. GitHub 저장소 → Settings → Secrets and variables → Actions → New repository secret.
3. 이름 `DISCORD_WEBHOOK_URL`, 값은 Discord에서 복사한 webhook URL. 채팅·코드·문서에 값을 붙이지 말 것.
4. 그 뒤 push하여 Team Sync 완료 후 Discord Sync Notification 실행 결과 확인.

워크플로는 Secret이 없으면 SKIP하고, main 검사 성공 및 각 브랜치 검사 실패만 알림.
알림 내용은 브랜치·커밋·실행 링크이며 @멘션은 비활성화. 원격 브랜치의 스크립트를 알림 job에서 실행하지 않음.
취소된 실행은 알리지 않음. Secret을 나중에 등록했으면 새 push로 확인 가능.

## Rulesets와 CODEOWNERS

Rulesets는 특정 브랜치에 적용하는 서버 측 규칙 묶음. PR 의무·승인 수·필수 검사뿐 아니라 삭제 제한·force push 차단만 선택할 수도 있음.
이 팀에는 main의 삭제·force push 차단 정도가 선택 가능한 최소 설정이며 일반 push와 merge는 유지.
현재는 적용하지 않았음. 필요하면 Settings → Rules → Rulesets → New branch ruleset에서 main 대상으로 Restrict deletions와 Block force pushes만 선택.
Require a pull request와 Require status checks는 이 직접 push 운영의 초기 설정에서는 선택하지 않음.

CODEOWNERS는 `/backend/ @account`처럼 파일 경로에 검토 담당자를 연결하는 파일. PR에서 담당자를 자동 리뷰 요청할 때 유용.
그 자체로 수정 권한을 독점시키거나 직접 push를 금지하지 않으며, 직접 push 알림·모듈 동기화를 자동으로 제공하지 않음.
이 팀은 PR 의무가 없고 실제 담당 계정·모듈도 아직 없으므로 CODEOWNERS는 만들지 않았음.

## 원격 설정과 제한

워크플로 파일은 main에 있어야 Actions의 Run workflow 버튼과 workflow_run 기반 Discord 알림을 정상 이용할 수 있음.
Actions가 비활성화되어 있다면 Settings → Actions → General에서 허용 필요. 이 워크플로는 저장소 읽기 권한만 사용.
GitHub 연결이 workflow 쓰기 권한을 제공하지 않으면 파일 작성 권한과 별도로 추가 연결 설정이 필요할 수 있음. 원격 반영·실행 여부는 실제 결과로 확인해야 함.
