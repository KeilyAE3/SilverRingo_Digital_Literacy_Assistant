# 키오스크 5레벨 개편

1. 매장식사·포장주문 (공식 연습 단계 1)
2. 버거 카테고리 → 리아불고기 (2)
3. 빵 → 세트 → 디저트·드링크 → 주문담기 (3~6)
4. 결제 → 주문내역·신용카드 → 카드 삽입 → 포인트 → 영수증 (7~11)
5. 혼자서 전체 주문: 버튼 강조 없음

링고버거 문구와 별도 시작 광고 화면을 제거했습니다. 실제 주문·결제·포인트 조회는 발생하지 않습니다.
웹 진도 키는 silver-ringo-kiosk-course4, Android는 kiosk4_done_N입니다. 기존 코스 진도는 삭제하지 않고 새 코스에 사용하지 않습니다.
공식 화면 구조를 참고해 재구성했으며 픽셀 단위 동일 복제나 전체 메뉴 구현은 아닙니다.

엔진 테스트 18개 및 Chromium 420px/1440px 5레벨 흐름 검증을 통과했습니다.
전체 변경·Android 검증은 docs/IT_LEARNING_UPDATE_HANDOVER.md 및 IT_LEARNING_UPDATE_VALIDATION.md를 참고하세요.
웹 실행: web/index.html. Android 실행: 저장소 루트 android/ 프로젝트.
