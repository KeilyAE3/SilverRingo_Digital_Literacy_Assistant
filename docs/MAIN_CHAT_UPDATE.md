# 확장 챗봇·DHub 연동 main 반영

현재 Android 앱과 서버 코드를 같은 팀 기준으로 정리한다. 버전은 기존 1.1.1/code 3을 유지한다. 시작 로컬/원격 main은 e5fe417이며 fetch 후 main에 새 커밋이 없었다. 다른 팀원의 변경을 덮어쓰거나 개발 브랜치를 병합하지 않았다. Juno 최신 개발 변경은 이번 챗봇 작업 범위에 포함하지 않는다.

## 반영 내용

- DHub 공개 HTTPS 주소를 Debug/Release의 독립 Gradle property에 설정했다. 키는 서버에서만 관리한다.
- 일반 디지털 질문·잡담은 기존 GeminiAdapter로 생성하고, 앱 기능은 RAG 근거와 지원 상태를 구분한다.
- 짧은 후속 질문은 최근 사용자 주제를 반영하며 새 주제에는 과거 문장을 합치지 않는다.
- 답변별 출처/액션을 저장하고 실제 학습·가이드 진입만 버튼으로 제공한다. Android가 ID/type/target/출처를 다시 검증한다. 화면 왕복과 상태 복원을 유지한다.
- 정상적인 비밀번호 변경 질문을 허용하면서 실제 비밀값 탐지·기존 JSON/길이/출처 검증·오류 처리를 보존한다.
- VM에만 있던 IP별/전체 요청 제한과 429 구현을 api.py에 그대로 반영하고 Tester에 회귀 검사를 등록했다.

## VM과의 동기화

이번 작업에서 VM api.py만 읽기 전용으로 가져왔다. 이전 배포 때의 해시와 같으며 다음 값이다.

8224038c572966ee754071ce548af6fe2893e047e27a64298b7c02aeec8f46bd

다른 챗봇 확장 코드·JSON 8개는 앞선 별도 배포에서 이미 VM에 반영됐다. 이번에는 VM 쓰기·서비스 재시작을 하지 않았다. 현재 기능을 위해 추가 서버 배포는 필요하지 않으며, 후속 동기화 때 원격 main과 VM의 미커밋 파일 차이를 비교하고 백업한 뒤 통합해야 한다. 무조건 git pull하거나 초기화하지 않는다. 새 요청 제한 테스트는 팀 저장소에 추가되므로 VM에서 테스트를 계속 운영하려면 별도 검토 후 가져올 수 있다. 비공개 환경·API 키·systemd 설정은 공유하지 않는다.

## 최종 로컬 검증

| 검사 | 결과 |
|---|---|
| Python 챗봇 관련 전체 | 27/27 통과, 실제 Gemini 호출 없음 |
| Team Sync workflow 범위 | 21/21 통과 |
| Team Sync local | 36개 중 34개 통과, 기존 의도적 실패 2개만 남음 |
| Kotlin 단위 검사 | 9/9 통과 |
| API37 에뮬레이터 | 6/6 통과: 답변별 버튼·두 기능 왕복·상태 복원·안전한 오류 및 기존 학습/가이드 |
| Android Debug 빌드·lint | 성공, lint 오류 0·기존 경고 51 |

의도적 실패는 HEAD에 이미 있던 test_progress_fail_demo(25에 50 비교), test_remaining_fail_demo(3에 2 비교)다. Tester/example의 변경이 없음을 확인했다. 테스트를 삭제·비활성화하지 않았다. VM 요청 제한은 IP별·전체 차단·health 예외·시간 창 만료·IP 분리로 확인했다. 실제 운영 API 한도를 소진하지 않았다.

## 파일 구성과 제외

서버 변경 9개(Python 6개와 JSON 3개), Android의 ChatScreen/ChatClient/MainActivity/ChatActions와 DHub 주소 설정, Python·Kotlin·기기 테스트 및 문서를 반영한다. 구체적인 확장 파일 목록은 CHATBOT_EXPANSION.md를 따른다. 이번에 추가된 api.py와 test_d_rate_limit.py는 VM 요청 제한 동기화용이다.

.env·.env.*·.venv·.local·local.properties·캐시·빌드 디렉터리·APK는 제외한다. 비밀 패턴과 staged 경로를 확인한 뒤 소스/테스트/문서만 커밋한다. GitHub Actions가 자동 실행하는 범위는 Team Sync Python/Node 검사이며 Android 기기 검사는 이번 로컬에서 별도로 수행했다.

## 남은 검수

실기기에서 실제 답변 버튼·권한·큰 글자·키보드·네트워크 전환을 확인해야 한다. 생성 답변의 기기별 메뉴 명칭·사실성, 규칙 기반 후속 문맥/민감정보 탐지의 한계도 남는다. 실제 DHub 응답 검증은 DHUB_DEPLOYMENT_RESULT.md의 앞선 배포 기록이며 이번 Git 반영을 위해 Gemini를 다시 호출하지 않았다.
