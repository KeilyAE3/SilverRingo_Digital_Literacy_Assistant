# DHub 챗봇 확장 코드 배포 결과

## 실제 반영

서버 silverringo-api의 기존 가상환경과 silverringo-chat.service를 그대로 사용했다. 로컬 미커밋 변경을 파일 단위로 배포했으며 Git Commit/Push/Merge/pull은 하지 않았다.

VM 저장소: /home/gkswjdgh132/SilverRingo_Digital_Literacy_Assistant

배포 파일은 SilverRingo_Chat-bot 아래 다음 8개다.

| 위치 | 파일 |
|---|---|
| src/silver_ringo_d | chat_service.py, llm.py, system_prompt.py, action_registry.py, knowledge_store.py |
| knowledge | action_catalog.json, app_info.json, assistant_guides.json |

8개 모두 배포 전 VM 버전이 로컬 HEAD e5fe417과 일치함을 확인했다. VM 전용 수정과 충돌하지 않아 필요한 파일만 적용했다. 검증용 테스트는 .local staging에 따로 두었으며 운영 Tester 파일이나 기존 VM 테스트를 덮어쓰지 않았다.

백업: /home/gkswjdgh132/SilverRingo_Digital_Literacy_Assistant/.local/dhub-deploy/expansion-20261031-062417/backup

## 요청 제한·보안 보존

VM 전용 SlidingWindowRateLimiter는 api.py에 있다. 해당 파일을 수정하지 않았으며 배포 전후 SHA256이 같다.

8224038c572966ee754071ce548af6fe2893e047e27a64298b7c02aeec8f46bd

기존 IP별 제한 검사와 추가 전역 한도·health 예외 검사를 Fake 모드로 확인했다. 실제 운영 한도를 소진하는 테스트는 하지 않았다. 따라서 코드 보존과 통합 동작은 검증했지만 외부에서의 운영 한도 수치·클라이언트 IP 전달 정책까지 새로 평가한 것은 아니다.

/etc/silverringo/silverringo-chat.env를 읽거나 수정하지 않았다. API 키·모델 비밀값을 조회/출력하지 않았으며 systemd 설정·DHub 주소·의존성을 변경하지 않았다.

## 테스트와 재시작

- 별도 staging에 VM의 기존 공개 소스·지식과 api.py를 복사한 후 8개 변경만 적용했다.
- VM 기존 Python 3.12.3 가상환경에서 챗봇 24/24, 요청 제한 2/2 통과.
- 백업과 기존 해시를 재확인한 뒤 파일을 교체하고, 설치된 Python 소스를 사용하는 동일 검사 26개를 다시 통과했다.
- 이후 silverringo-chat.service만 재시작했다. active/running, MainPID=9846, NRestarts=0.
- 외부 검증 완료 후 배포 8개 해시와 api.py 불변을 재확인했다. 검증 실패가 없어 롤백은 수행하지 않았다. 원본 백업은 유지한다.

## 외부 HTTPS 실호출 결과

GET /health: HTTP 200, status=ok, knowledge_loaded=true. 기본 TLS 인증서 검증을 사용했다.

실제 채팅 요청은 아래 4회만 실행했으며 자동 재요청은 하지 않았다.

| 사례 | 상태 | 출처·액션 |
|---|---|---|
| general: 휴대폰에서 사진 보내는 방법을 알려주세요. | 200 | sources 0개, 액션 없음 |
| kiosk: 키오스크 결제 방법을 배우고 싶어요. | 200 | sources 2개, 액션 open_kiosk_course |
| followup: 그다음에는 어떻게 해? | 200 | sources 2개, 액션 없음 |
| hospital: 실버링고에서 병원 접수 도와줘 | 200 | sources 2개, 액션 open_guide_home |

일반 질문은 카카오톡 사진 전송을 단계별로 설명했고 sources/actions는 모두 비었다. 키오스크 답변은 실제 결제가 아닌 시험 학습임을 구분하며 learning.kiosk를 반환했다. 후속 질문에는 직전 키오스크 질문·답변 이력 2개를 전달했고 키오스크 결제 문맥과 출처가 유지됐다. 후속 답변의 액션이 빈 것은 인용·점수 필터에 따른 정상 동작이다. 병원 질문은 guides.home을 반환했다. 모두 [모의 응답] 표시 없는 자연어 답변이다.

응답 원문과 요청 ID는 Git 제외 .local/dhub-deploy/live-results.json에 기록했다.

## 스마트폰 확인과 한계

확장판 APK로 일반 질문·키오스크/병원 답변 버튼을 눌러 실제 화면 진입과 채팅 복귀를 확인해야 한다. 큰 글자·키보드·Wi-Fi/모바일 데이터·429/503 재시도·외부 앱 권한 흐름은 실기기 검증이 남았다. 서버 응답의 앱 메뉴 명칭·기기별 설명 정확성은 별도 검수해야 한다. 예를 들어 일반 사진 질문에서는 사용 앱을 확인하지 않고 카카오톡을 가정한 답변이 나왔다.

현재 배포는 서버 정상 응답·출처·액션 계약 검증이며 스마트폰 실행을 완료했다는 뜻은 아니다.
