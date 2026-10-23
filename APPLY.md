# SilverRingo 협업 자동화 초기 설정

## 현재 상태

설정과 운영 문서 작성 및 로컬 검증은 완료했습니다. 자동화 자체 테스트 11개, 협업 검사, 두 workflow YAML 파싱, 내장 알림 Python 문법 검사를 통과했습니다.
GitHub 연결이 일반 tree 쓰기에도 HTTP 403 `Resource not accessible by integration`을 반환해 원격 파일·브랜치·설정은 변경하지 못했습니다.
Actions 원격 실행과 Discord 전달은 아직 검증하지 못했습니다. 저장소 계정 권한과 연결 앱 권한은 다를 수 있습니다.

기준 커밋: d66215abd6317dfc9d406ec9e014e353e03e89eb

## 로컬 Git으로 적용

자신의 GitHub 계정으로 push할 수 있는 로컬 저장소에서 실행합니다. 작업 중 변경이 있으면 먼저 보관하세요.
ZIP의 `SilverRingo_Automation.patch` 경로는 실제 압축 해제 위치에 맞추세요.

```bash
git switch main
git pull --ff-only
git apply --check /path/to/SilverRingo_Automation.patch
git apply /path/to/SilverRingo_Automation.patch
python -m unittest discover -s automation/tests -v
python automation/check.py
git add .gitignore README.md AGENTS.md .github automation docs
git commit -m "Set up Discord-first collaboration automation"
git push origin main
```

`git apply --check`가 실패하면 중단하고 현재 파일과 대조하세요. 강제로 기존 내용을 덮어쓰지 마세요.
최초 자동화 설치를 main에 반영하는 절차이며, 이후 앱 변경은 작업·통합 브랜치를 사용합니다.
Git 인증이 workflow 파일 쓰기를 허용해야 합니다. 비밀번호·PAT·webhook URL을 대화에 보내지 마세요.

ZIP의 `files/`에도 변경 파일 전체를 포함했습니다. 기존 README와 gitignore를 덮어쓰기보다 패치 적용을 권합니다.

## 원격 검증

1. 저장소 Actions → Team Sync에서 방금 push한 실행 확인.
2. `Collaboration and configured application checks`가 성공했는지 확인.
3. Summary에서 앱 검사 `NOT CONFIGURED` 표시 확인. 아직 앱 코드는 없으므로 앱 통합 성공을 의미하지 않습니다.
4. GitHub Settings → Secrets and variables → Actions에 `DISCORD_WEBHOOK_URL` Secret을 등록하면 이후 push부터 Discord 알림 사용 가능.
5. main 검사 성공·각 브랜치 검사 실패만 알림. Secret이 없으면 알림 job은 SKIP 안내 후 종료.

## 운영과 다음 설정

`files/docs/COLLABORATION.md`에 자세한 설명이 있습니다. 적용 뒤에는 저장소의 `docs/COLLABORATION.md`를 읽으면 됩니다.

- 업무 분배·회의는 Discord. Issues·Projects·PR 승인 의무 없음.
- main은 스프린트 통합 완성본. `integration/<sprint>`에서 먼저 동기화하고 부분 시연은 `release/<demo>` 사용.
- 작업 브랜치 예: `feat/<owner>/<topic>`.
- 실제 모듈·스택을 정한 뒤 `automation/config.json`에 빌드·단위·계약·통합 테스트 명령을 등록하고 workflow에 의존성 설치를 추가.
- 각 AI가 AGENTS.md를 읽도록 설정하고 논리적인 작업별 변경 보고서를 함께 push.
- 일정은 `automation/schedule.json` 수정 뒤 `python automation/check.py --write-schedule` 실행, 생성된 간트 문서도 함께 커밋.
- Rulesets와 CODEOWNERS는 이번 구성에 적용하지 않았습니다. 직접 push는 그대로 허용합니다.

push 후 Actions는 이미 올라간 커밋을 차단하거나 롤백하지 않습니다. 빨간 검사 결과를 보고 수정하는 운영입니다.
