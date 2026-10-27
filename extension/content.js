/**
 * SilverLingo - 실제 공식 사이트 실시간 오버레이 스크립트 v2.1
 * (버튼 가림 100% 박멸 + 플로팅 바 자동 충돌 회피 + 엄격한 URL/화면별 단계 자동 전환)
 */

(function () {
  console.log("[SilverLingo] 실시간 동행 오버레이 로드됨: ", window.location.href);

  // 음성 재생 함수 (TTS)
  function speak(text) {
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
      const ut = new SpeechSynthesisUtterance(text);
      ut.lang = 'ko-KR';
      ut.rate = 0.92;
      window.speechSynthesis.speak(ut);
    }
  }

  // 1. 상태 및 엘리먼트 참조
  let activeSpotlight = null;
  let activeTooltip = null;
  let currentTargetEl = null;
  let currentStepId = null;
  let tooltipPositionSide = 'auto'; // 'top', 'bottom', 'auto'
  let isNavigatingStep = false;
  let isFloatingBarCollapsed = false;

  // 2. 화면 우하단 상시 플로팅 바 (전체 진행 상황 표시 + 접기/펴기 + 자동 회피)
  function createGlobalFloatingBar() {
    let bar = document.getElementById('silverlingo-floating-bar');
    if (bar) return;

    bar = document.createElement('div');
    bar.id = 'silverlingo-floating-bar';
    bar.className = 'silverlingo-global-floating-bar';
    bar.innerHTML = `
      <div class="silverlingo-bot-avatar" id="silverlingo-avatar-toggle" title="누르시면 작게 접거나 펼칩니다">🤖</div>
      <div class="silverlingo-bot-info">
        <strong>실버링고 실시간 동행 중</strong>
        <span id="silverlingo-step-status">다음 단계를 파악하고 있어요...</span>
      </div>
      <div class="silverlingo-bar-actions">
        <button type="button" id="silverlingo-tts-btn" class="silverlingo-btn-tts" title="음성 안내 다시 듣기">
          🔊 다시 듣기
        </button>
        <button type="button" id="silverlingo-bar-collapse-btn" class="silverlingo-btn-icon" title="도우미 바 접기">
          —
        </button>
      </div>
    `;
    document.body.appendChild(bar);

    // 다시 듣기
    document.getElementById('silverlingo-tts-btn').addEventListener('click', (e) => {
      e.stopPropagation();
      if (window.silverlingoCurrentSpeech) {
        speak(window.silverlingoCurrentSpeech);
      } else {
        speak("실버링고입니다. 노란색으로 반짝이는 버튼을 찾아 눌러주세요.");
      }
    });

    // 접기/펴기 동작
    const toggleCollapse = (e) => {
      e.stopPropagation();
      isFloatingBarCollapsed = !isFloatingBarCollapsed;
      bar.classList.toggle('collapsed', isFloatingBarCollapsed);
    };
    document.getElementById('silverlingo-bar-collapse-btn').addEventListener('click', toggleCollapse);
    document.getElementById('silverlingo-avatar-toggle').addEventListener('click', toggleCollapse);
  }

  // 3. 플로팅 바 위치 충돌 회피 (타깃 버튼이 하단에 있으면 플로팅 바를 상단으로 자동 이동)
  function adjustFloatingBarPosition(targetRect) {
    const bar = document.getElementById('silverlingo-floating-bar');
    if (!bar) return;

    const winHeight = window.innerHeight;
    // 타깃 요소가 화면 하단 150px 이내에 있으면 플로팅 바를 상단으로 이동시켜 버튼 가림 완전 방지
    if (targetRect && (targetRect.bottom > winHeight - 140)) {
      bar.classList.add('dock-top');
    } else {
      bar.classList.remove('dock-top');
    }
  }

  // 4. 타깃 요소 위에 오버레이 링과 스마트 말풍선 얹기
  function attachOverlay(stepId, targetEl, title, text, speechText) {
    if (!targetEl || isNavigatingStep) return;

    // 이미 같은 단계와 같은 요소에 붙어있는 경우 위치만 갱신
    if (currentStepId === stepId && currentTargetEl === targetEl) {
      updateOverlayPosition();
      return;
    }

    currentStepId = stepId;
    currentTargetEl = targetEl;
    window.silverlingoCurrentSpeech = speechText;

    // 기존 오버레이 제거
    removeOverlayElements();

    // A. 노란색 스포트라이트 링 (pointer-events: none으로 버튼 클릭 100% 통과)
    activeSpotlight = document.createElement('div');
    activeSpotlight.className = 'silverlingo-spotlight-ring';
    document.body.appendChild(activeSpotlight);

    // B. '여기를 누르세요' 스마트 툴팁 말풍선 (반투명 & 매크로 오인 방지)
    activeTooltip = document.createElement('div');
    activeTooltip.className = 'silverlingo-tooltip-bubble';
    activeTooltip.innerHTML = `
      <div class="silverlingo-tooltip-header">
        <div class="silverlingo-tooltip-badge">
          <span>🤖 실버링고 도우미</span>
        </div>
        <button type="button" class="silverlingo-dodge-btn" title="버튼이 잘 보이도록 도우미 위치를 위/아래로 바꿉니다">
          ↕ 위치 비키기
        </button>
      </div>
      <p class="silverlingo-tooltip-text">
        <span class="silverlingo-finger-pointer">👉</span>
        <strong>${title}</strong><br>
        <span class="silverlingo-tooltip-desc">${text}</span>
      </p>
      <div class="silverlingo-click-hint">
        👇 아래 표시된 버튼을 손가락으로 직접 눌러주세요
      </div>
      <div class="silverlingo-tooltip-arrow"></div>
    `;
    document.body.appendChild(activeTooltip);

    // 위치 비키기 버튼
    const dodgeBtn = activeTooltip.querySelector('.silverlingo-dodge-btn');
    if (dodgeBtn) {
      dodgeBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        e.preventDefault();
        tooltipPositionSide = (tooltipPositionSide === 'top') ? 'bottom' : 'top';
        updateOverlayPosition();
      });
    }

    // 말풍선 터치 시 타깃 요소를 화면 중앙으로 스크롤만 지원 (매크로 차단 방지)
    activeTooltip.addEventListener('click', (e) => {
      if (e.target.closest('.silverlingo-dodge-btn')) return;
      if (currentTargetEl) {
        currentTargetEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
    });

    // 타깃 버튼 자체에 직접 클릭 감지 리스너 부착 (자동 다음 단계 전환)
    const onTargetClick = () => {
      handleTargetClicked(stepId, targetEl);
    };
    targetEl.addEventListener('click', onTargetClick, { once: true });

    // 위치 렌더링
    tooltipPositionSide = 'auto';
    updateOverlayPosition();

    // 화면 가운데로 부드럽게 스크롤
    try {
      targetEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
    } catch (err) {}

    // 음성 안내 재생
    if (speechText) {
      setTimeout(() => speak(speechText), 400);
    }
  }

  function removeOverlayElements() {
    if (activeSpotlight) {
      activeSpotlight.remove();
      activeSpotlight = null;
    }
    if (activeTooltip) {
      activeTooltip.remove();
      activeTooltip = null;
    }
  }

  // 타깃 클릭 시 자동 다음 단계 전환 피드백
  function handleTargetClicked(stepId, targetEl) {
    if (isNavigatingStep) return;
    isNavigatingStep = true;
    console.log(`[SilverLingo] 단계 [${stepId}] 버튼 클릭 완료! 다음 단계 감지 대기 중...`);

    // 긍정 피드백 음성
    speak("잘 누르셨습니다! 다음 화면으로 이동하고 있습니다.");

    // 상태바 업데이트
    const statusText = document.getElementById('silverlingo-step-status');
    if (statusText) statusText.textContent = "⏳ 다음 화면을 분석하고 있어요...";

    if (activeTooltip) {
      const desc = activeTooltip.querySelector('.silverlingo-tooltip-desc');
      if (desc) desc.textContent = "확인되었습니다! 다음 화면으로 넘어갑니다...";
      activeTooltip.classList.add('silverlingo-tooltip-success');
    }

    // 실제 클릭 수행 (말풍선에서 위임된 경우)
    if (document.activeElement !== targetEl) {
      try {
        targetEl.click();
      } catch (err) {}
    }

    // 잠시 후 다음 단계 탐색
    setTimeout(() => {
      isNavigatingStep = false;
      removeOverlayElements();
      detectCurrentStepAndGuide();
    }, 1200);
  }

  // 5. 절대 가리지 않는 스마트 위치 계산 로직 (Smart Placement Engine)
  function updateOverlayPosition() {
    if (!currentTargetEl || !activeSpotlight || !activeTooltip) return;

    const rect = currentTargetEl.getBoundingClientRect();
    if (rect.width === 0 && rect.height === 0) return;

    const scrollX = window.scrollX || window.pageXOffset;
    const scrollY = window.scrollY || window.pageYOffset;

    // 플로팅 바 위치 충돌 회피 조정
    adjustFloatingBarPosition(rect);

    // 1) 스포트라이트 박스 (타깃 바깥으로 패딩 6px 두르고 마우스 관통)
    const pad = 6;
    activeSpotlight.style.top = `${rect.top + scrollY - pad}px`;
    activeSpotlight.style.left = `${rect.left + scrollX - pad}px`;
    activeSpotlight.style.width = `${rect.width + pad * 2}px`;
    activeSpotlight.style.height = `${rect.height + pad * 2}px`;

    // 2) 툴팁 말풍선 위치 계산 (타깃 요소와 절대 겹치지 않게 여백 20px)
    const tooltipWidth = activeTooltip.offsetWidth || 310;
    const tooltipHeight = activeTooltip.offsetHeight || 120;
    const arrowEl = activeTooltip.querySelector('.silverlingo-tooltip-arrow');

    let placeOnTop = true;
    if (tooltipPositionSide === 'top') {
      placeOnTop = true;
    } else if (tooltipPositionSide === 'bottom') {
      placeOnTop = false;
    } else {
      // 위쪽 공간이 툴팁 높이 + 35px 이상 있으면 위쪽, 아니면 아래쪽
      placeOnTop = rect.top >= tooltipHeight + 35;
    }

    let tooltipTop = 0;
    if (placeOnTop) {
      // 타깃 요소 바로 위 20px 지점
      tooltipTop = rect.top + scrollY - tooltipHeight - 20;
      activeTooltip.classList.remove('arrow-top');
      activeTooltip.classList.add('arrow-bottom');
    } else {
      // 타깃 요소 바로 아래 20px 지점
      tooltipTop = rect.bottom + scrollY + 20;
      activeTooltip.classList.remove('arrow-bottom');
      activeTooltip.classList.add('arrow-top');
    }

    // 좌우 정렬: 타깃 가로 중심에 맞추되, 화면 경계 안쪽으로 안전 클램핑
    const targetCenterX = rect.left + scrollX + rect.width / 2;
    let tooltipLeft = targetCenterX - tooltipWidth / 2;
    tooltipLeft = Math.max(16, Math.min(window.innerWidth - tooltipWidth - 16, tooltipLeft));

    activeTooltip.style.top = `${tooltipTop}px`;
    activeTooltip.style.left = `${tooltipLeft}px`;

    // 화살표 꼬리 위치 정렬
    if (arrowEl) {
      const arrowLeft = Math.max(20, Math.min(tooltipWidth - 20, targetCenterX - tooltipLeft));
      arrowEl.style.left = `${arrowLeft}px`;
    }
  }

  window.addEventListener('scroll', updateOverlayPosition, { passive: true });
  window.addEventListener('resize', updateOverlayPosition, { passive: true });

  // 6. 지능적 요소 탐색 헬퍼 (링크/버튼만 타깃팅, 본문 span 오탐 방지)
  function findClickableByText(tagSelectors, keyword) {
    const elements = document.querySelectorAll(tagSelectors);
    for (const el of elements) {
      if (el.offsetParent === null) continue; // 화면에 보이는 것만
      const text = el.textContent ? el.textContent.trim() : '';
      if (text.includes(keyword)) {
        const clickable = el.closest('a, button, [role="button"], input[type="button"], input[type="submit"]');
        return clickable || el;
      }
    }
    return null;
  }

  // 7. 코레일 기차표 예매 자동 단계 엔진 (korail.com 및 letskorail.com 지원)
  function checkKorailPipeline() {
    const url = window.location.href;
    const statusText = document.getElementById('silverlingo-step-status');

    // [Step 2] 열차 목록 및 시간표 화면 (/ticket/ 등)
    if (url.includes('ticket') || url.includes('schedule') || url.includes('ebizprd')) {
      const bookBtn = document.querySelector('a.btn_r, button.btn_r, a.btn_reserve, input[value*="예매"]') ||
                      findClickableByText('a, button', '예매하기') ||
                      findClickableByText('a, button', '예매');

      if (bookBtn) {
        if (statusText) statusText.textContent = "2단계: 기차표 예매 버튼 누르기";
        attachOverlay(
          'korail_step2_book',
          bookBtn,
          "원하시는 시간의 [예매] 버튼을 누르세요!",
          "KTX 좌석을 예약하고 결제로 진행하는 버튼입니다.",
          "원하시는 시간의 기차표 예매 버튼을 눌러주세요."
        );
        return true;
      }
    }

    // [Step 1] 코레일 메인 및 간편예매 화면 (korail.com/intro, letskorail.com 등)
    const searchBtn = document.querySelector('button.search_btn') ||
                      document.querySelector('a.quick_item.n2') ||
                      document.querySelector('a[href*="ticket"], a[href*="schedule"]') ||
                      findClickableByText('button, a', '열차조회') ||
                      findClickableByText('button, a', '조회하기') ||
                      findClickableByText('button, a, img', '승차권 간편예매') ||
                      document.querySelector('img[alt*="조회"], img[alt*="예매"]')?.closest('a, button');

    if (searchBtn) {
      if (statusText) statusText.textContent = "1단계: 열차 조회하기";
      attachOverlay(
        'korail_step1_search',
        searchBtn,
        "여기를 누르세요!",
        "원하시는 기차표 일정을 조회하는 버튼입니다.",
        "어르신, 노란색으로 반짝이는 열차조회 버튼을 눌러주세요."
      );
      return true;
    }

    return false;
  }

  // 8. 통합 지능형 단계 감지기
  function detectCurrentStepAndGuide() {
    createGlobalFloatingBar();

    const host = window.location.hostname;
    if (host.includes('letskorail.com') || host.includes('korail.com')) {
      checkKorailPipeline();
    }
  }

  // 9. 자동 변화 감시자 (DOM Mutation & SPA Navigation Tracker)
  let scanDebounceTimer = null;
  function triggerStepDetectionDebounced() {
    if (isNavigatingStep) return;
    clearTimeout(scanDebounceTimer);
    scanDebounceTimer = setTimeout(() => {
      detectCurrentStepAndGuide();
    }, 300);
  }

  // 초기 시작
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
      detectCurrentStepAndGuide();
      setTimeout(detectCurrentStepAndGuide, 800);
    });
  } else {
    detectCurrentStepAndGuide();
    setTimeout(detectCurrentStepAndGuide, 800);
  }

  // DOM 변화 감시 (모달 팝업 출현 시 즉시 캐치)
  const observer = new MutationObserver((mutations) => {
    for (const m of mutations) {
      if (m.target && m.target.classList &&
          (m.target.classList.contains('silverlingo-spotlight-ring') ||
           m.target.classList.contains('silverlingo-tooltip-bubble') ||
           m.target.classList.contains('silverlingo-global-floating-bar'))) {
        return;
      }
    }
    triggerStepDetectionDebounced();
  });

  observer.observe(document.body || document.documentElement, {
    childList: true,
    subtree: true
  });

  // URL 변경 감지
  let lastUrl = location.href;
  setInterval(() => {
    const currentUrl = location.href;
    if (currentUrl !== lastUrl) {
      lastUrl = currentUrl;
      console.log("[SilverLingo] 페이지 URL 변경 감지 -> 단계 자동 재설정:", currentUrl);
      removeOverlayElements();
      setTimeout(detectCurrentStepAndGuide, 500);
    }
  }, 400);

})();
