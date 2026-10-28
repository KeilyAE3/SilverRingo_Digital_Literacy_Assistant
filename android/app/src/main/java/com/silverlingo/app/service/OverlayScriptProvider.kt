package com.silverlingo.app.service

/**
 * 똑닥 모바일 웹(ddocdoc.com) 웹뷰에 주입할 최신 v2.2 오버레이 스크립트 및 스타일
 * (스마트 로그인/본인인증 감지 & 안심 도우미 인터럽트 파이프라인 탑재)
 */
object OverlayScriptProvider {

    fun getOverlayCss(): String {
        return """
            /* SilverLingo - 모바일 최적화 오버레이 스타일 v2.2 */
            .silverlingo-spotlight-ring {
                position: absolute !important;
                pointer-events: none !important;
                z-index: 999998 !important;
                border: 4px solid #f59e0b !important;
                border-radius: 12px !important;
                box-shadow: 0 0 0 6px rgba(245, 158, 11, 0.45), 0 8px 24px rgba(0, 0, 0, 0.35) !important;
                animation: silverlingo-pulse 1.8s infinite alternate ease-in-out !important;
                transition: all 0.25s ease !important;
            }

            .silverlingo-spotlight-ring.auth-ring {
                border-color: #3b82f6 !important;
                box-shadow: 0 0 0 6px rgba(59, 130, 246, 0.45), 0 8px 24px rgba(0, 0, 0, 0.35) !important;
            }

            @keyframes silverlingo-pulse {
                0% {
                    border-color: #f59e0b;
                    box-shadow: 0 0 0 4px rgba(245, 158, 11, 0.45), 0 4px 14px rgba(0, 0, 0, 0.2);
                }
                100% {
                    border-color: #d97706;
                    box-shadow: 0 0 0 12px rgba(217, 119, 6, 0.65), 0 12px 30px rgba(217, 119, 6, 0.35);
                }
            }

            .silverlingo-tooltip-bubble {
                position: absolute !important;
                z-index: 999999 !important;
                background: rgba(30, 27, 75, 0.82) !important;
                backdrop-filter: blur(12px) !important;
                -webkit-backdrop-filter: blur(12px) !important;
                color: #ffffff !important;
                padding: 14px 16px !important;
                border-radius: 16px !important;
                border: 2.5px solid rgba(251, 191, 36, 0.9) !important;
                box-shadow: 0 12px 36px rgba(0, 0, 0, 0.45) !important;
                font-family: -apple-system, BlinkMacSystemFont, "Pretendard", "Noto Sans KR", sans-serif !important;
                width: 300px !important;
                max-width: 92vw !important;
                cursor: default !important;
                user-select: none !important;
                animation: silverlingo-fade-in 0.3s ease !important;
                box-sizing: border-box !important;
                transition: top 0.25s cubic-bezier(0.16, 1, 0.3, 1), left 0.25s cubic-bezier(0.16, 1, 0.3, 1) !important;
            }

            .silverlingo-tooltip-bubble.auth-bubble {
                border-color: rgba(96, 165, 250, 0.9) !important;
                background: rgba(15, 23, 42, 0.84) !important;
                backdrop-filter: blur(12px) !important;
                -webkit-backdrop-filter: blur(12px) !important;
            }

            @keyframes silverlingo-fade-in {
                from { opacity: 0; transform: translateY(-6px); }
                to { opacity: 1; transform: translateY(0); }
            }

            .silverlingo-tooltip-header {
                display: flex !important;
                align-items: center !important;
                justify-content: space-between !important;
                margin-bottom: 8px !important;
            }

            .silverlingo-tooltip-badge {
                display: inline-flex !important;
                align-items: center !important;
                gap: 4px !important;
                background-color: #fbbf24 !important;
                color: #78350f !important;
                font-size: 13px !important;
                font-weight: 800 !important;
                padding: 3px 8px !important;
                border-radius: 6px !important;
            }

            .silverlingo-tooltip-badge.auth-mode {
                background-color: #3b82f6 !important;
                color: #ffffff !important;
            }

            .silverlingo-dodge-btn {
                background: rgba(255, 255, 255, 0.25) !important;
                border: 1.5px solid rgba(251, 191, 36, 0.8) !important;
                color: #ffffff !important;
                font-size: 13px !important;
                font-weight: 800 !important;
                padding: 5px 10px !important;
                border-radius: 12px !important;
                cursor: pointer !important;
                touch-action: manipulation !important;
                box-shadow: 0 2px 6px rgba(0, 0, 0, 0.25) !important;
            }

            .silverlingo-dodge-btn:active {
                background: #fbbf24 !important;
                color: #78350f !important;
            }

            .silverlingo-tooltip-text {
                font-size: 17px !important;
                font-weight: 800 !important;
                line-height: 1.4 !important;
                color: #ffffff !important;
                margin: 0 0 6px 0 !important;
            }

            .silverlingo-tooltip-sub {
                font-size: 13px !important;
                color: #e0e7ff !important;
                line-height: 1.45 !important;
                margin: 0 0 8px 0 !important;
            }

            .silverlingo-click-hint {
                display: inline-block !important;
                background: rgba(245, 158, 11, 0.25) !important;
                border: 1px dashed #fbbf24 !important;
                color: #fef08a !important;
                padding: 5px 8px !important;
                border-radius: 8px !important;
                font-size: 12px !important;
                font-weight: 700 !important;
                width: 100% !important;
                box-sizing: border-box !important;
                text-align: center !important;
            }

            .silverlingo-tooltip-bubble.auth-bubble .silverlingo-click-hint {
                background: rgba(59, 130, 246, 0.25) !important;
                border-color: #93c5fd !important;
                color: #dbeafe !important;
            }

            .silverlingo-tooltip-arrow {
                position: absolute !important;
                width: 0 !important;
                height: 0 !important;
                border-style: solid !important;
                pointer-events: none !important;
            }

            .silverlingo-tooltip-bubble.arrow-bottom .silverlingo-tooltip-arrow {
                bottom: -12px !important;
                left: 50% !important;
                transform: translateX(-50%) !important;
                border-width: 12px 10px 0 10px !important;
                border-color: rgba(251, 191, 36, 0.9) transparent transparent transparent !important;
            }

            .silverlingo-tooltip-bubble.arrow-top .silverlingo-tooltip-arrow {
                top: -12px !important;
                left: 50% !important;
                transform: translateX(-50%) !important;
                border-width: 0 10px 12px 10px !important;
                border-color: transparent transparent rgba(251, 191, 36, 0.9) transparent !important;
            }

            .silverlingo-tooltip-bubble.auth-bubble.arrow-bottom .silverlingo-tooltip-arrow {
                border-color: rgba(96, 165, 250, 0.9) transparent transparent transparent !important;
            }

            .silverlingo-tooltip-bubble.auth-bubble.arrow-top .silverlingo-tooltip-arrow {
                border-color: transparent transparent rgba(96, 165, 250, 0.9) transparent !important;
            }
        """.trimIndent()
    }

    fun getOverlayJs(serviceKey: String): String {
        return """
            (function() {
                console.log("[SilverLingo-Android] 스마트 로그인 감지 & 실시간 오버레이 엔진 로드: " + window.location.href);

                function speak(text) {
                    if (window.SilverLingoAndroid && window.SilverLingoAndroid.speak) {
                        window.SilverLingoAndroid.speak(text);
                    }
                }

                function notifyStep(stepId, title) {
                    if (window.SilverLingoAndroid && window.SilverLingoAndroid.onStepChanged) {
                        window.SilverLingoAndroid.onStepChanged(stepId, title);
                    }
                }

                let activeSpotlight = null;
                let activeTooltip = null;
                let currentTargetEl = null;
                let currentStepId = null;
                let userForcedPosition = null; // 'top' | 'bottom' | null
                let actualCurrentSide = 'bottom';
                let hasNotifiedAuthRecovery = false;

                function attachOverlay(stepId, targetEl, title, text, speechText, badgeText, isAuthMode) {
                    if (!targetEl) return;
                    if (currentStepId === stepId && currentTargetEl === targetEl) {
                        updateOverlayPosition();
                        return;
                    }

                    currentStepId = stepId;
                    currentTargetEl = targetEl;
                    userForcedPosition = null;
                    removeOverlay();

                    notifyStep(stepId, title);
                    if (speechText) {
                        speak(speechText);
                    }

                    const badge = badgeText || (isAuthMode ? '🛡️ 본인인증(로그인) 도우미' : '👉 여기를 누르세요!');

                    // 1. 하이라이트 박스 (노란/파란 테두리 링)
                    activeSpotlight = document.createElement('div');
                    activeSpotlight.className = 'silverlingo-spotlight-ring' + (isAuthMode ? ' auth-ring' : '');
                    document.body.appendChild(activeSpotlight);

                    // 2. 스마트 말풍선 (반투명 글래스모피즘 & 매크로 오인 방지)
                    activeTooltip = document.createElement('div');
                    activeTooltip.className = 'silverlingo-tooltip-bubble' + (isAuthMode ? ' auth-bubble' : '');
                    activeTooltip.innerHTML = `
                        <div class="silverlingo-tooltip-header">
                            <span class="silverlingo-tooltip-badge ` + (isAuthMode ? 'auth-mode' : '') + `">${"$"}{badge}</span>
                            <button type="button" class="silverlingo-dodge-btn" id="silverlingo-dodge-btn">↕ 위치 비키기</button>
                        </div>
                        <div class="silverlingo-tooltip-text">${"$"}{title}</div>
                        <div class="silverlingo-tooltip-sub">${"$"}{text}</div>
                        <div class="silverlingo-click-hint">👇 아래 표시된 버튼을 손가락으로 직접 눌러주세요</div>
                        <div class="silverlingo-tooltip-arrow"></div>
                    `;
                    document.body.appendChild(activeTooltip);

                    // 위치 비키기 버튼: 모바일 터치와 클릭 모두 완벽 지원
                    const dodgeBtn = activeTooltip.querySelector('#silverlingo-dodge-btn');
                    if (dodgeBtn) {
                        function handleDodge(e) {
                            e.stopPropagation();
                            if (e.cancelable) e.preventDefault();
                            // 현재 위치의 정반대로 토글
                            if (userForcedPosition === 'top') {
                                userForcedPosition = 'bottom';
                            } else if (userForcedPosition === 'bottom') {
                                userForcedPosition = 'top';
                            } else {
                                // 현재 실제 배치된 위치의 반대로 전환
                                userForcedPosition = (actualCurrentSide === 'top') ? 'bottom' : 'top';
                            }
                            updateOverlayPosition();
                        }

                        dodgeBtn.addEventListener('click', handleDodge);
                        dodgeBtn.addEventListener('touchend', handleDodge);
                    }

                    // ⭐ [매크로 오인 방지]: 말풍선 터치 시 타깃 버튼을 대리 클릭(click())하지 않음!
                    // 사이트의 매크로 감지 보안 솔루션이 차단하는 것을 100% 방지하고, 타깃 버튼으로 부드럽게 스크롤만 유도
                    activeTooltip.addEventListener('click', (e) => {
                        if (e.target && e.target.id === 'silverlingo-dodge-btn') return;
                        if (currentTargetEl) {
                            currentTargetEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
                        }
                    });

                    updateOverlayPosition();
                }

                function removeOverlay() {
                    if (activeSpotlight && activeSpotlight.parentNode) {
                        activeSpotlight.parentNode.removeChild(activeSpotlight);
                    }
                    if (activeTooltip && activeTooltip.parentNode) {
                        activeTooltip.parentNode.removeChild(activeTooltip);
                    }
                    activeSpotlight = null;
                    activeTooltip = null;
                }

                function updateOverlayPosition() {
                    if (!currentTargetEl || !activeSpotlight || !activeTooltip) return;

                    const rect = currentTargetEl.getBoundingClientRect();
                    const scrollX = window.pageXOffset || document.documentElement.scrollLeft;
                    const scrollY = window.pageYOffset || document.documentElement.scrollTop;

                    // 테두리 링 위치
                    activeSpotlight.style.top = (rect.top + scrollY - 6) + 'px';
                    activeSpotlight.style.left = (rect.left + scrollX - 6) + 'px';
                    activeSpotlight.style.width = (rect.width + 12) + 'px';
                    activeSpotlight.style.height = (rect.height + 12) + 'px';

                    // 말풍선 위치 계산 (타깃과 겹치지 않는 외곽 16px 안전 배치)
                    const tooltipWidth = Math.min(300, document.documentElement.clientWidth - 20);
                    const tooltipHeight = activeTooltip.offsetHeight || 140;

                    let topPos = 0;
                    let targetSide = userForcedPosition;

                    if (!targetSide) {
                        // 자동 배치: 상단 공간이 충분하면 상단, 부족하면 하단
                        targetSide = (rect.top > tooltipHeight + 24) ? 'top' : 'bottom';
                    }

                    if (targetSide === 'top') {
                        topPos = (rect.top + scrollY - tooltipHeight - 16);
                        // 만약 화면 위로 잘릴 경우 최소 여백 확보
                        if (topPos < scrollY + 10) {
                            topPos = scrollY + 10;
                        }
                        actualCurrentSide = 'top';
                    } else {
                        topPos = (rect.bottom + scrollY + 16);
                        actualCurrentSide = 'bottom';
                    }

                    const useArrowClass = (actualCurrentSide === 'top') ? 'arrow-bottom' : 'arrow-top';

                    let leftPos = (rect.left + scrollX + (rect.width / 2) - (tooltipWidth / 2));
                    const maxLeft = (document.documentElement.clientWidth - tooltipWidth - 10);
                    leftPos = Math.max(10, Math.min(leftPos, maxLeft));

                    activeTooltip.style.top = topPos + 'px';
                    activeTooltip.style.left = leftPos + 'px';

                    activeTooltip.classList.remove('arrow-top', 'arrow-bottom');
                    activeTooltip.classList.add(useArrowClass);
                }


                window.addEventListener('resize', updateOverlayPosition);
                window.addEventListener('scroll', updateOverlayPosition, { passive: true });

                // 🔍 로그인 / 본인인증 화면 판정 함수
                function isAuthOrLoginPage() {
                    const url = window.location.href.toLowerCase();
                    if (url.includes('main') && !url.includes('login')) {
                        return false;
                    }

                    const hasAuthUrl = url.includes('login') || url.includes('sso') || url.includes('auth') || url.includes('cert') || url.includes('member');
                    const hasPasswordInput = !!document.querySelector('input[type="password"]');
                    const hasAuthHeading = Array.from(document.querySelectorAll('h1, h2, h3, .title, strong, legend')).some(el => {
                        const t = (el.textContent || '').trim();
                        return t.includes('로그인') || t.includes('본인확인') || t.includes('간편인증') || t.includes('비회원 신청');
                    });

                    return hasAuthUrl || hasPasswordInput || hasAuthHeading;
                }

                // 사이트별 파이프라인
                const serviceKey = '$serviceKey';

                function checkPipeline() {
                    const currentUrl = window.location.href;

                    // ⭐ [핵심 1] 로그인 / 본인인증 화면 감지 시 단계 일시 중단 및 안심 도우미 가동
                    if (isAuthOrLoginPage()) {
                        hasNotifiedAuthRecovery = false;

                        // 1순위: 간편인증 (카카오톡, 네이버) 또는 비회원 신청 버튼 우선 타깃팅
                        const easyAuthBtn = Array.from(document.querySelectorAll('button, a, div[role="button"], input[type="button"], label')).find(el => {
                            const txt = (el.textContent || el.value || '').trim();
                            return txt.includes('카카오') || txt.includes('간편인증') || txt.includes('비회원 신청') || txt.includes('비회원 로그인');
                        });

                        if (easyAuthBtn) {
                            const clickable = easyAuthBtn.closest('button') || easyAuthBtn.closest('a') || easyAuthBtn;
                            attachOverlay(
                                'auth-interruption-easy',
                                clickable,
                                '안심하세요! 본인 확인을 도와드릴게요',
                                '아이디나 비밀번호가 없어도 괜찮습니다. 가장 쉽고 빠른 [카카오톡 간편인증]이나 [비회원 신청]을 눌러주세요.',
                                '안심하세요. 병원 접수를 위해 본인인지 확인하는 화면입니다. 아이디를 기억하지 못하셔도 괜찮으니, 제가 짚어드리는 카카오톡이나 비회원 신청을 눌러보세요.',
                                '🛡️ 본인인증 도우미',
                                true
                            );
                            return;
                        }

                        // 2순위: 일반 로그인 버튼 (아이디/비밀번호 입력 화면)
                        const submitLoginBtn = document.querySelector('button[type="submit"], input[type="submit"], #btn_login, .btn_login')
                            || Array.from(document.querySelectorAll('button, a')).find(el => (el.textContent || '').trim() === '로그인');
                        if (submitLoginBtn) {
                            attachOverlay(
                                'auth-interruption-login',
                                submitLoginBtn,
                                '본인 확인(로그인) 화면입니다',
                                '아이디나 회원번호를 적으신 후 이 [로그인] 버튼을 누르시면 원래 신청 화면으로 돌아갑니다.',
                                '본인 확인 화면입니다. 정보를 입력하신 후 로그인 버튼을 눌러주세요.',
                                '🛡️ 로그인 확인',
                                true
                            );
                            return;
                        }
                    }

                    // ⭐ [핵심 2] 로그인이 끝나고 원래 업무 화면으로 복귀했을 때 안심 음성
                    if (!hasNotifiedAuthRecovery && currentUrl.includes('apply')) {
                        hasNotifiedAuthRecovery = true;
                        speak("본인 확인이 완료되었습니다! 계속해서 병원 접수를 도와드릴게요.");
                    }

                    // [똑닥 (DDocDoc) 병원 안심 당일 접수 시나리오]
                    if (serviceKey === 'ddocdoc' || currentUrl.includes('ddocdoc')) {
                        // 1. 병원 상세/접수 화면의 접수하기/예약하기 버튼 (우선순위 1위)
                        const applyBtn = Array.from(document.querySelectorAll('button, a, div[role="button"]')).find(el => {
                            const txt = (el.textContent || '').trim();
                            return txt === '접수하기' || txt === '예약하기' || txt.includes('당일 접수') || txt.includes('바로 접수');
                        });
                        if (applyBtn) {
                            const clickable = applyBtn.closest('button') || applyBtn.closest('a') || applyBtn;
                            attachOverlay(
                                'ddocdoc-step-2',
                                clickable,
                                '[🩺 진료 접수하기]',
                                '오늘 진료를 신청하시려면 이 접수하기 버튼을 눌러주세요.',
                                '당일 진료를 신청하시려면 접수하기 버튼을 눌러주세요.'
                            );
                            return;
                        }

                        // 2. 진료 대상자 선택 (본인)
                        const patientSelect = Array.from(document.querySelectorAll('button, div, label, span')).find(el => {
                            const txt = (el.textContent || '').trim();
                            return txt === '본인' || txt.includes('본인(나)');
                        });
                        if (patientSelect) {
                            const clickable = patientSelect.closest('button') || patientSelect.closest('label') || patientSelect;
                            attachOverlay(
                                'ddocdoc-step-3',
                                clickable,
                                '[👤 진료 대상자 (본인)]',
                                '진료를 받으실 본인을 선택해 주세요.',
                                '진료받으실 분으로 본인을 선택해 주세요.'
                            );
                            return;
                        }

                        // 3. 접수 최종 완료 버튼
                        const submitApplyBtn = Array.from(document.querySelectorAll('button, a')).find(el => {
                            const txt = (el.textContent || '').trim();
                            return txt.includes('접수 신청') || txt.includes('접수 완료') || txt.includes('예약 완료');
                        });
                        if (submitApplyBtn) {
                            attachOverlay(
                                'ddocdoc-step-4',
                                submitApplyBtn,
                                '[✅ 접수 신청 완료]',
                                '접수 내용을 확인하신 후 누르시면 병원 대기 번호가 발급됩니다.',
                                '접수 신청을 완료하시려면 이 버튼을 눌러주세요.'
                            );
                            return;
                        }

                        // 4. 메인 화면의 병원 찾기 / 진료과 선택 버튼
                        const ddocdocSearchBtn = Array.from(document.querySelectorAll('button, a, div[role="button"]')).find(el => {
                            const txt = (el.textContent || '').trim();
                            return txt.includes('병원 찾기') || txt.includes('접수') || txt.includes('예약') || txt.includes('진료과');
                        });
                        if (ddocdocSearchBtn) {
                            const clickable = ddocdocSearchBtn.closest('button') || ddocdocSearchBtn.closest('a') || ddocdocSearchBtn;
                            attachOverlay(
                                'ddocdoc-step-1',
                                clickable,
                                '[🏥 병원 찾기 / 진료 접수]',
                                '방문하실 병원을 찾거나 가까운 진료과를 선택하시려면 이 버튼을 눌러주세요.',
                                '진료받으실 병원을 선택하시려면 화면에 표시된 버튼을 눌러주세요.'
                            );
                            return;
                        }
                    }
                }

                // 주기적 및 DOM 변경 시 파이프라인 체크
                setInterval(checkPipeline, 900);
                checkPipeline();

                const observer = new MutationObserver(() => {
                    checkPipeline();
                });
                observer.observe(document.body, { childList: true, subtree: true });
            })();
        """.trimIndent()
    }
}
