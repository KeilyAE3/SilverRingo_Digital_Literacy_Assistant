# A/B/C/D 실제 원본과 추출 대상 (2026-10-28 확인)

Repository: https://github.com/KeilyAE3/SilverRingo_Digital_Literacy_Assistant

## A hjh → app_info
- `silverRingo/app/src/main/java/com/silverringo/app/HomeScreen.kt`: 홈 메뉴 `홈`, `IT 기능 배우기`, `생활 가이드`; 기능 카드 설명; 채팅 입력 스텁과 음성입력 미구현.
- `silverRingo/app/src/main/java/com/silverringo/app/MainActivity.kt`: `onLearnClick`, `onGuideClick`; 클릭 후 placeholder 화면.
- `silverRingo/README.md`: 현재 구현/실행 조건.
원본 샘플: `FeatureCard("IT 기능 배우기", "스마트폰·디지털 기기\n사용법을 배워요.", ...)`.
링크: https://github.com/KeilyAE3/SilverRingo_Digital_Literacy_Assistant/blob/hjh/silverRingo/app/src/main/java/com/silverringo/app/HomeScreen.kt

## B Juno → assistant_guides
- `android/app/src/main/java/com/silverlingo/app/ui/screens/MainDashboardScreen.kt`: 똑닥 서비스/앱 패키지/7단계 사용자 순서.
- `android/app/src/main/java/com/silverlingo/app/service/SilverAccessibilityService.kt`: native `findDdocdocStep()`의 7단계 안내, 타깃 판별 키워드.
- `android/app/src/main/java/com/silverlingo/app/service/OverlayScriptProvider.kt`: 웹 오버레이 `ddocdoc-step-1~4` 및 안내 문구.
- `android/app/src/main/java/com/silverlingo/app/service/FloatingCompanionService.kt`: 플로팅 위젯 및 TTS.
- `android/app/src/main/java/com/silverlingo/app/ui/screens/CompanionBrowserActivity.kt`: 인앱 웹 동행.
- `AGENTS.md`, `docs/ideas_and_design.md`: 의사결정/과거 제안. **과거 카카오T·코레일·정부24 구상 또는 폐기 정보를 현재 지원이라 표현하지 말 것.**
원본 샘플: 서비스 키 `ddocdoc`, 패키지 `com.bbros.sayup`, 웹 `https://www.ddocdoc.com/`; native 7단계 `접수 신청`.
링크: https://github.com/KeilyAE3/SilverRingo_Digital_Literacy_Assistant/blob/Juno/android/app/src/main/java/com/silverlingo/app/service/SilverAccessibilityService.kt

## C jib → learning_courses
- `silver-ringo-c/web/content.json`: schemaVersion=1, topicId=kiosk, levels 6개, guides 9개, products 5개, goal.
- `silver-ringo-c/web/content.js`: JS 콘텐츠 mirror; 수정 권위/동기화 스크립트 파악.
- `silver-ringo-c/android/app/src/main/java/kr/silverringo/learning/LearningEntry.kt`: `LearningEntry(onExit,onResult)`; 레벨 잠금/완료.
- `silver-ringo-c/docs/INTEGRATION.md`: JSON result, WebView 브리지.
- `silver-ringo-c/docs/VALIDATION.md`: 엔진 테스트 11/11 및 브라우저 검증, Android 빌드/실기기 미검증 명시.
원본 샘플: 레벨5 `결제 방법 배우기`, `concept=...실제 카드나 돈이 필요하지 않습니다.`.
링크: https://github.com/KeilyAE3/SilverRingo_Digital_Literacy_Assistant/blob/jib/silver-ringo-c/web/content.json

## D ydy_workspace → 테스트
- `Tester/config.json`의 `checks` 배열이 등록 원본. `module`=저장소 루트 기준, `test`=Tester 폴더 기준.
- `Tester/automation/check.py`는 unittest 기반으로 local/workflow 각 enable/include 설정을 적용한다.
- `.github/workflows/team-sync.yml`: `main` push 또는 수동 dispatch 중 main ref만. Python 3.12, `--mode workflow`.
- `Tester/docs/TEAM_SYNC.md`에 **오래된 상충 설명**이 있으므로 해당 문서 첫머리만 믿지 않고 실제 check.py/config.json을 우선.
링크: https://github.com/KeilyAE3/SilverRingo_Digital_Literacy_Assistant/blob/ydy_workspace/Tester/config.json

## 자료 갱신 규칙
각 모듈 작업 시작 시 `git fetch`하여 현 원본의 변경 여부를 확인하고 샘플과 차이가 있으면 **그 차이를 사용자에게 보고**한 후 지식 파일 수정. 다른 브랜치 코드 읽기 전용. 임의 아이디어로 공백을 채우지 말고 `idea` 상태와 근거를 별도 관리.
