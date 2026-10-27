너는 어르신을 위한 1:1 스마트폰/태블릿 동행 도우미 서비스인 'SilverLingo (실버링고)' 프로젝트의 전문 안드로이드 개발 AI 어시스턴트야.

작업을 시작하기 전에 프로젝트 루트의 `CLAUDE.md`와 `AGENTS.md`를 반드시 먼저 숙지해줘. 
(참고: 과거 기획 단계의 RPA 완전 자동화나 가짜 시뮬레이터 모달은 이미 공식 폐기된 항목이니, 절대 누락/버그로 오해하거나 되살리지 말 것!)

---

### [작업 목표: 화면 우하단 '실버링고 동행 중' 플로팅 위젯의 UI/UX 대폭 개선]

현재 화면 우하단에 상주하는 플로팅 도우미 위젯(`android/app/src/main/java/com/silverlingo/app/service/FloatingCompanionService.kt`)의 문구가 앱 화면의 핵심 버튼이나 텍스트를 가리는 불편함이 있어, 아래 **2가지 핵심 제스처 인터랙션**을 구현해줘.

#### 1. 화면 옆쪽(가장자리) 축소 및 도킹 (Edge Docking / Minimize)
- **문제점**: "실버링고 동행 중 / 입력 위치를 짚어드려요 / 다시듣기" 텍스트 바가 항상 펼쳐져 있어 화면 우하단을 가림.
- **요구사항**:
  - 로봇 아이콘(🤖)을 누르거나 화면 옆으로 밀면, 텍스트가 쏙 접히고 **화면 좌/우 가장자리 끝에 작은 🤖 탭만 반쯤 걸쳐 있는 미니 모드**로 축소되게 해줘.
  - 미니 모드 상태에서는 화면의 어떤 버튼도 가리지 않아야 하며, 사용자가 🤖 탭을 다시 터치하면 부드럽게 원래의 "실버링고 동행 중" 및 [🔊 다시듣기] 바가 펼쳐져야 함.
  - 드래그를 마치고 손을 뗐을 때, 화면 중앙에 붕 떠 있지 않고 **가장 가까운 좌측 또는 우측 화면 모서리로 자석처럼 착 달라붙는(Snap to edge)** 부드러운 위치 보정 적용.

#### 2. 화면 중앙 하단 드래그 앤 드롭으로 '✕ 완전 종료' (Drag-to-Dismiss)
- **요구사항 (페이스북 메신저 챗헤드 / 안드로이드 플로팅 버블 스타일)**:
  - 사용자가 플로팅 버블을 손가락으로 드래그하기 시작하면(`onDragStart`), **화면 중앙 하단(`Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL`)에 반투명한 빨간색 원형 [✕ 종료] 타깃 영역**이 서서히 나타남.
  - 버블을 화면 중앙 하단의 [✕] 영역 위로 가져가면:
    - [✕] 타깃 영역이 살짝 확대(Scale-up)되거나 붉은색으로 강조되며, 기기 진동(Haptic Feedback)으로 사용자에게 "여기에 놓으면 닫힌다"는 피드백을 줌.
  - [✕] 타깃 영역 안에서 손가락을 떼면(`onDragEnd`):
    - 플로팅 도우미가 즉시 화면에서 사라지고 `FloatingCompanionService`가 완전히 종료(`stopSelf()`)됨.
  - [✕] 타깃 영역 밖에서 손가락을 떼면:
    - [✕] 타깃 영역은 다시 페이드아웃되고, 버블은 가장 가까운 좌/우 화면 가장자리로 스냅되어 유지됨.

#### 3. 구현 시 주의사항 (코드 레벨 안내)
- **파일 위치**: `android/app/src/main/java/com/silverlingo/app/service/FloatingCompanionService.kt`
- 현재 `FloatingCompanionService.kt`를 보면 Compose `FloatingWidgetContent`에 `onDrag` 매개변수는 있으나 실제 `Modifier.pointerInput`에서 `detectDragGestures`가 바인딩되어 있지 않아 실제 드래그가 매끄럽게 동작하지 않는 상태임.
- Compose의 `pointerInput(Unit) { detectDragGestures(onDragStart, onDragEnd, onDragCancel, onDrag) }`를 올바르게 적용하고, `WindowManager.updateViewLayout`을 통해 윈도우 좌표(`params.x`, `params.y`)를 실시간 업데이트할 것.
- 하단 [✕] 닫기 영역은 별도의 독립 Window(`dismissView`, `FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCHABLE`)로 관리하여, 드래그 중에만 표시(`VISIBLE`)하고 손을 떼면 숨김(`GONE`) 처리할 것.
- 기존의 TTS 음성 안내 기능 및 `ServiceLifecycleOwner` 연동이 깨지지 않도록 안전하게 구현하고, 작업 후 `gradlew.bat assembleDebug`로 빌드 에러 0개를 반드시 확인할 것.

위 요구사항을 바탕으로 `FloatingCompanionService.kt`를 깔끔하게 수정하고, 정상 빌드 결과를 보고해줘.
