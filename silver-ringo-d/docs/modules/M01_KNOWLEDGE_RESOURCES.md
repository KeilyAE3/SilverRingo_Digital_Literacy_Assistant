# M01 — 4개 JSON 지식 리소스·원본 추출·검증

## 목표
샘플 JSON 4개를 실제 A/B/C 소스와 대조하여 신뢰 가능한 초기 리소스로 만들고, schema validator/원본 변화 감지를 구현한다. 아이디어 자료는 절대 현행 서비스로 등록하지 않는다.

## 선행
M00 완료. `docs/01_DATA_CONTRACT.md`, `docs/03_SOURCE_CATALOG.md`, `knowledge/*.json` 참고.

## 파일 / 인터페이스
- `knowledge/app_info.json`: A 홈/menu/placeholder.
- `knowledge/learning_courses.json`: C `content.json`의 코스/6단계/9가이드 정보를 재구성. 상품/가격은 교육 지식에 반드시 필요할 때만 포함.
- `knowledge/assistant_guides.json`: B 똑닥 현행 서비스, native 7단계, web 4단계. 출처 구분 유지. 후속 아이디어 별도 리스트.
- `knowledge/action_catalog.json`: 현재 합의되지 않은 UI 이동은 모두 `enabled=false`.
- `src/silver_ringo_d/knowledge_store.py`: `load()`, `documents()`, `reload()`(원자적).
- `scripts/import_jib_content.py`: 가능하면 C 원본을 읽어 기계적으로 레벨·안내를 변환하는 동기화 스크립트. GitHub API나 네트워크에 의존하지 말고 로컬 파일 입력 인자를 받는다.
- `tests/test_knowledge_store.py`: JSON 스키마와 중복·상태 필터·원본 참조 검사.

## 요구사항
1. `git fetch` 후 A/B/C 원본 경로가 현재도 유효한지 확인. `source.branch/path/kind` 기입; 가능하면 정확한 commit SHA 확보.
2. `level_id`와 `step_id` 보존. **웹 overlay step 1~4 = Native step 1~7 아님**.
3. 소스가 존재하더라도 실제 통합 미완료이면 `status=prototype`/`placeholder`로 설명; 동작을 과장하지 않음.
4. `ideas` 후보는 `status=idea`, 실행 연결 없음. 정부24는 과거 폐기 문서가 있어 `retired`, 카카오T도 현재 B 지원 범위 밖이므로 일단 `retired`로 취급. 향후 재검토는 사용자 승인.
5. `aliases`는 D파트 보완값이고 원본 인용과 구분. 근거 없는 예시는 `curated` 표시.
6. 누락/잘못된 JSON은 조용히 무시하지 않고 오류 보고. 원자적 reload 시 이전 정상 데이터 보존.
7. `learning_courses.json`의 내용이 변경되는 경우 `scripts/import_jib_content.py`로 재생성/차이를 검토; 다른 팀원 소스는 절대 직접 변경하지 않음.

## 결과 시연
- `python ...validate_knowledge.py` 등의 검사 명령 제공(이름은 실제 파일로 확정).
- 적재 문서 수와 `implemented/prototype/placeholder/idea/retired` 별 건수 출력.
- C 레벨5, A 학습 메뉴, B 접수 7단계의 구체적 추출 샘플 보여주기.

## 테스트 수락
- 4개 JSON이 유효하고 ID 중복 없음.
- course kiosk 1개/레벨6개/guide9개.
- 똑닥 지원 정보 존재, 정부24·카카오T는 활성 행동으로 분류되지 않음.
- 문서 출처 100% 기록; 검색 색인에서 idea/retired 기본 제외 가능.
