package com.silverlingo.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.silverlingo.app.ui.theme.AmberAccent
import com.silverlingo.app.ui.theme.AmberDark
import com.silverlingo.app.ui.theme.NavyPrimary
import java.util.Locale
import kotlin.math.abs

/**
 * 실버링고 똑닥(DDocDoc) 병원 안심 당일 접수 전용 동행 접근성 서비스
 *
 * - 태블릿 대형 화면 및 가로/세로 모드 완벽 대응
 * - 분리형 듀얼 윈도우:
 *   1) 노란색 펄스 링 (FLAG_NOT_TOUCHABLE): 타깃 버튼에 정확히 밀착되며 터치가 100% 관통하여 즉시 동작
 *   2) 스마트 말풍선 (FLAG_NOT_TOUCH_MODAL): 버튼 위/아래 외곽 분리 배치로 버튼 가림 0% 및 태블릿 너비(320dp+) 확보
 * - 최신 똑닥(sayup) 노드 텍스트 전면 포괄 (권한 허용, 시작하기, 병원・약국, 진료접수, 본인선택 등)
 * - 재귀적 딥 노드 탐색(DFS) 탑재로 React Native UI 완벽 탐지
 * - 어르신 친화적 0.88배속 음성 안내(TTS) 자동 제공
 */
class SilverAccessibilityService : AccessibilityService(), TextToSpeech.OnInitListener {

    private companion object {
        const val DDOCDOC_PACKAGE = "com.bbros.sayup"
        val AUTH_KEYWORDS = listOf(
            "회원가입", "약관", "본인인증", "본인 인증", "본인확인", "본인 확인", "인증번호", "통신사",
            "문자(SMS)", "보안문자", "주민등록번호", "생년월일", "휴대폰번호", "이메일 로그인", "카카오로 시작"
        )
        val HOME_SEARCH_KEYWORDS = listOf("질병, 진료과", "병원 검색", "병원검색")
        val SEARCH_INPUT_KEYWORDS = listOf("질병", "진료과", "병원", "검색", "증상")
        const val REVIEW_MS = 8000L
        val TRANSIT_PACKAGES = setOf(DDOCDOC_PACKAGE, "com.android.systemui", "com.kakao.talk", "com.nhn.android.search")
    }

    private val handler = Handler(Looper.getMainLooper())
    private val reviewTimeout = Runnable { refreshGuidance() }
    private val scrollSettled = Runnable { refreshGuidance() }
    private var trackedNode: AccessibilityNodeInfo? = null
    private var hasEnteredDdocdoc = false
    // 홈(1단계 검색창)을 보기 전 로그인·인증 화면의 글자/입력창이 2·6단계로 잡히지 않도록 막는 게이트
    private var hasReachedHome = false
    private var reviewStartedAt = 0L

    private lateinit var windowManager: WindowManager
    private var ringView: ComposeView? = null
    private var tooltipView: ComposeView? = null
    private var lastTargetRect: Rect? = null
    private var shownLabel: String? = null
    private var ringParams: WindowManager.LayoutParams? = null
    private var tooltipParams: WindowManager.LayoutParams? = null
    private var isTopSide = true
    private var lastSpokenLabel: String? = null
    private var lastSpokenTime: Long = 0
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.KOREAN
            tts?.setSpeechRate(0.88f)
            isTtsReady = true
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            followTargetOnScroll()
            return
        }
        refreshGuidance(
            isWindowChange = event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            isOwnEvent = event.packageName?.toString() == packageName
        )
    }

    // 똑닥 이용 중 잠시 거쳐 갈 수 있는 화면 (실버링고, 권한 창, 시스템 UI, 키보드, 카카오/네이버 로그인)
    private fun isTransitPackage(pkg: String): Boolean {
        val imePkg = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)?.substringBefore('/')
        return pkg.isEmpty() || pkg == packageName || pkg == imePkg || pkg in TRANSIT_PACKAGES ||
            pkg.contains("permissioncontroller") || pkg.contains("inputmethod")
    }

    private fun refreshGuidance(isWindowChange: Boolean = false, isOwnEvent: Boolean = false) {
        // 똑닥 화면(및 똑닥이 띄운 시스템 권한 허용 창) 위에서만 동작. 실버링고 등 다른 앱이 맨 앞이면 정리
        val rootNode = rootInActiveWindow ?: return
        val activePkg = rootNode.packageName?.toString() ?: ""
        if (activePkg == DDOCDOC_PACKAGE) {
            hasEnteredDdocdoc = true
        } else if (isWindowChange && hasEnteredDdocdoc && !isTransitPackage(activePkg)) {
            // 똑닥을 쓰다가 홈 화면이나 다른 앱으로 완전히 나가면 '동행 중' 위젯도 종료
            hasEnteredDdocdoc = false
            stopService(Intent(this, FloatingCompanionService::class.java))
        }
        if (activePkg != DDOCDOC_PACKAGE && !activePkg.contains("permissioncontroller")) {
            lastTargetRect = null
            removeSpotlightOverlay()
            return
        }
        // 우리 오버레이 창이 낸 이벤트는 똑닥 화면 변화가 아니므로 무시 (표시→재탐색 반복 방지)
        if (isOwnEvent) return

        // 로그인·회원가입·본인인증·주민번호 입력 화면은 안내 대상이 아님 (PASS 웹뷰 내부·입력창 hint까지 DFS로 확인)
        if (isAuthOrSignupScreen(rootNode)) {
            hasReachedHome = false
            lastTargetRect = null
            removeSpotlightOverlay()
            return
        }

        val target = findDdocdocStep(rootNode)

        if (target != null) {
            val (targetNode, message) = target
            trackedNode = targetNode
            val rect = Rect()
            targetNode.getBoundsInScreen(rect)
            if ((!rect.isNear(lastTargetRect) || ringView == null || message != shownLabel) && rect.width() > 10 && rect.height() > 10) {
                lastTargetRect = rect

                showSpotlightOverlay(
                    targetRect = rect,
                    nodeText = message
                )

                // 새로운 안내가 나오면 자동 음성 가이드 (3초 쿨타임)
                val now = SystemClock.uptimeMillis()
                if (message != lastSpokenLabel || (now - lastSpokenTime) > 3000) {
                    lastSpokenLabel = message
                    lastSpokenTime = now
                    speak(message)
                }
            }
        } else {
            // 타깃이 없으면 즉시 정리 (RN/웹뷰는 액티비티 전환 없이 내용만 바뀌어 이전 말풍선이 남던 문제)
            lastTargetRect = null
            removeSpotlightOverlay()
        }
    }

    private fun speak(message: String) {
        if (!isTtsReady) return
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "SilverAccessibilityTTS")
    }

    /**
     * 똑닥: "이 버튼은 무엇" 대신 병원을 찾아가는 순서를 안내
     * 1 검색창 → 2 검색어 입력 → 3 병원 고르기 → 4 후기 확인 → 5 바로 접수 → 6 본인 선택 → 7 접수 신청
     * 뒤 단계 화면일수록 먼저 검사하고, "접수/확인/선택"처럼 여러 화면에 흔한 단어는 쓰지 않음 (홈의 "실시간 접수"가 검색창을 가로채던 문제)
     */
    private fun findDdocdocStep(root: AccessibilityNodeInfo): Pair<AccessibilityNodeInfo, String>? {
        val homeSearch = findNodeByKeywords(root, HOME_SEARCH_KEYWORDS)
        if (homeSearch != null) hasReachedHome = true
        if (!hasReachedHome) {
            return findNodeByKeywords(root, listOf("앱 사용 중에만 허용", "허용", "건너뛰기"))
                ?.let { it to "시작하기 전에 화면의 [허용] 또는 [건너뛰기]를 눌러주세요." }
        }

        // 홈에는 자주 찾은 병원 카드의 "후기"·"예약" 글자도 보여 4·5단계가 검색창을 가로챔 → 검색창이 보이면 1·2단계 우선
        if (homeSearch != null) {
            resetReviewTimer()
            return searchInputStep(root) ?: (homeSearch to "1단계: 먼저 맨 위 검색창을 눌러주세요. 아픈 곳, 진료과, 병원 이름으로 찾을 수 있어요.")
        }

        findNodeByKeywords(root, listOf("접수 신청", "접수신청", "접수 요청하기"))
            ?.let { return it to "7단계: 마지막 단계예요. [접수 신청]을 눌러주세요." }
        // "본인 인증하기", "본인 확인을 하기 위해" 같은 문구의 "본인"은 제외
        findNodeByKeywords(root, listOf("본인", "진료받을 분", "진료 받을 분", "진료 대상자"), exclude = listOf("인증", "확인"))
            ?.let { return it to "6단계: 진료받으실 분으로 [본인]을 선택해주세요." }

        // 병원 상세 화면: 후기 확인은 누를 버튼이 없어 넘어갈 계기가 없으므로, 일정 시간 뒤 [바로 접수] 안내로 자동 전환
        // 접수 마감 시간에는 [바로 접수] 대신 [시간예약]만 활성화됨
        val applyBtn = findNodeByKeywords(root, listOf("바로접수", "바로 접수", "진료접수", "진료 접수", "접수하기", "시간예약", "시간 예약", "예약하기"))
        val reviewBtn = findNodeByKeywords(root, listOf("후기", "리뷰"))
        if (applyBtn != null || reviewBtn != null) {
            val now = SystemClock.uptimeMillis()
            if (reviewStartedAt == 0L) {
                reviewStartedAt = now
                handler.postDelayed(reviewTimeout, REVIEW_MS)
            }
            val showReview = reviewBtn != null && (applyBtn == null || now - reviewStartedAt < REVIEW_MS)
            return if (showReview) {
                reviewBtn!! to "4단계: [후기]를 눌러 이 병원 평이 어떤지 먼저 확인해 보세요."
            } else {
                applyBtn!! to "5단계: 이 병원으로 접수하시려면 노란색으로 표시된 [접수/예약] 버튼을 눌러주세요."
            }
        }
        resetReviewTimer()

        searchInputStep(root)?.let { return it }
        findNodeByKeywords(root, listOf("진료중", "진료 중", "접수가능", "접수 가능"))
            ?.let { return it to "3단계: 원하시는 병원 하나를 눌러 들어가 보세요." }
        return null
    }

    // 다른 병원을 고르면 후기 확인부터 다시 시작
    private fun resetReviewTimer() {
        reviewStartedAt = 0L
        handler.removeCallbacks(reviewTimeout)
    }

    // 병원 검색창일 때만 2단계 (PASS 이름·휴대폰번호·인증번호 입력창 등은 제외)
    private fun searchInputStep(root: AccessibilityNodeInfo): Pair<AccessibilityNodeInfo, String>? {
        val guidingSearch = shownLabel?.let { it.startsWith("1단계") || it.startsWith("2단계") } == true
        return root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?.takeIf { it.isEditable && (guidingSearch || SEARCH_INPUT_KEYWORDS.any { kw -> nodeText(it).contains(kw) }) }
            ?.let { it to "2단계: 아픈 곳(예: 감기)이나 진료과(예: 내과), 병원 이름을 적고 검색해주세요." }
    }

    private fun nodeText(node: AccessibilityNodeInfo): String {
        val hint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) node.hintText?.toString() ?: "" else ""
        return "${node.text ?: ""} ${node.contentDescription ?: ""} $hint"
    }

    // 보이는 노드의 text·contentDescription·hintText를 DFS로 훑어 인증/가입 키워드 확인 (findAccessibilityNodeInfosByText는 웹뷰·hint를 놓침)
    private fun isAuthOrSignupScreen(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        if (node.isVisibleToUser && AUTH_KEYWORDS.any { nodeText(node).contains(it) }) return true
        for (i in 0 until node.childCount) {
            if (isAuthOrSignupScreen(node.getChild(i))) return true
        }
        return false
    }


    private fun findNodeByKeywords(
        root: AccessibilityNodeInfo,
        keywords: List<String>,
        exclude: List<String> = emptyList()
    ): AccessibilityNodeInfo? {
        // 1. Android 기본 substring 검색
        for (kw in keywords) {
            val nodes = root.findAccessibilityNodeInfosByText(kw)
            if (!nodes.isNullOrEmpty()) {
                for (node in nodes) {
                    if (exclude.any { nodeText(node).contains(it) }) continue
                    val interactive = findClickableAncestor(node)
                    val rect = Rect()
                    interactive.getBoundsInScreen(rect)
                    if (node.isVisibleToUser && rect.width() > 10 && rect.height() > 10 && rect.top >= 0) {
                        return interactive
                    }
                }
            }
        }

        // 2. React Native 및 커스텀 컴포넌트 깊이 우선 재귀 탐색 (DFS)
        return findDeepInteractiveNode(root, keywords, exclude)
    }

    private fun findDeepInteractiveNode(
        node: AccessibilityNodeInfo?,
        keywords: List<String>,
        exclude: List<String> = emptyList()
    ): AccessibilityNodeInfo? {
        if (node == null) return null

        // 똑닥 검색창의 "질병, 진료과, 병원 검색"처럼 입력창 안내 문구는 hintText에만 들어 있음
        val combined = nodeText(node)

        if (combined.isNotBlank() && exclude.none { combined.contains(it) }) {
            for (kw in keywords) {
                if (combined.contains(kw, ignoreCase = true)) {
                    val interactive = findClickableAncestor(node)
                    val rect = Rect()
                    interactive.getBoundsInScreen(rect)
                    if (node.isVisibleToUser && rect.width() > 10 && rect.height() > 10 && rect.top >= 0) {
                        return interactive
                    }
                }
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findDeepInteractiveNode(child, keywords, exclude)
            if (found != null) return found
        }

        return null
    }

    private fun findClickableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo {
        var curr: AccessibilityNodeInfo? = node
        var depth = 0
        var bestCandidate: AccessibilityNodeInfo = node
        while (curr != null && depth < 5) {
            if (curr.isVisibleToUser) {
                bestCandidate = curr
                if (curr.isClickable) {
                    return curr
                }
            }
            curr = curr.parent
            depth++
        }
        return bestCandidate
    }

    private fun showSpotlightOverlay(
        targetRect: Rect,
        nodeText: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            return
        }

        // 같은 버튼이 조금 움직인 경우에는 창을 다시 만들지 않고 위치만 옮김 (펄스 애니메이션 재시작 깜빡임 방지)
        val ring = ringView
        val tooltip = tooltipView
        val rp = ringParams
        val tp = tooltipParams
        if (ring != null && tooltip != null && rp != null && tp != null && nodeText == shownLabel) {
            placeRing(rp, targetRect)
            placeTooltip(tp, targetRect)
            try {
                windowManager.updateViewLayout(ring, rp)
                windowManager.updateViewLayout(tooltip, tp)
            } catch (_: Exception) {}
            return
        }

        removeSpotlightOverlay()
        shownLabel = nodeText

        // 1. 노란색 펄스 링 (타깃 버튼에 정확히 밀착 & FLAG_NOT_TOUCHABLE로 클릭 100% 관통)
        val newRingParams = WindowManager.LayoutParams(
            0,
            0,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            useScreenCoordinates()
        }
        placeRing(newRingParams, targetRect)
        ringParams = newRingParams

        ringView = ComposeView(this).apply {
            val lifecycleOwner = ServiceLifecycleOwner()
            lifecycleOwner.performRestore(null)
            lifecycleOwner.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_CREATE)
            lifecycleOwner.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_START)
            lifecycleOwner.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_RESUME)

            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                PulsingHighlightRing()
            }
        }

        try {
            windowManager.addView(ringView, newRingParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. 스마트 말풍선 (태블릿 가로/세로 너비 완벽 보장 & 버튼 가림 0% 분리 배치)
        val density = resources.displayMetrics.density
        isTopSide = targetRect.top >= ((170 + 30) * density).toInt()

        val newTooltipParams = WindowManager.LayoutParams(
            0,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            useScreenCoordinates()
        }
        placeTooltip(newTooltipParams, targetRect)
        tooltipParams = newTooltipParams

        tooltipView = ComposeView(this).apply {
            val lifecycleOwner = ServiceLifecycleOwner()
            lifecycleOwner.performRestore(null)
            lifecycleOwner.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_CREATE)
            lifecycleOwner.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_START)
            lifecycleOwner.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_RESUME)

            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                SmartTooltipBubble(
                    nodeText = nodeText,
                    onDodgeToggle = {
                        isTopSide = !isTopSide
                        lastTargetRect?.let { placeTooltip(newTooltipParams, it) }
                        try {
                            windowManager.updateViewLayout(this@apply, newTooltipParams)
                        } catch (_: Exception) {}
                    }
                )
            }
        }

        try {
            windowManager.addView(tooltipView, newTooltipParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 오버레이 창은 기본적으로 상태바·노치 아래부터 배치되어, 화면 절대좌표(getBoundsInScreen)와 기기마다 다른 만큼 어긋남
    private fun WindowManager.LayoutParams.useScreenCoordinates() {
        flags = flags or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    private fun placeRing(params: WindowManager.LayoutParams, targetRect: Rect) {
        val density = resources.displayMetrics.density
        val ringPadding = (6 * density).toInt()
        params.width = (targetRect.width() + ringPadding * 2).coerceAtLeast((36 * density).toInt())
        params.height = (targetRect.height() + ringPadding * 2).coerceAtLeast((36 * density).toInt())
        params.x = (targetRect.left - ringPadding).coerceAtLeast(0)
        params.y = (targetRect.top - ringPadding).coerceAtLeast(0)
    }

    private fun placeTooltip(params: WindowManager.LayoutParams, targetRect: Rect) {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val density = displayMetrics.density
        val tooltipWidth = (320 * density).toInt().coerceAtMost(screenWidth - (32 * density).toInt())
        val tooltipEstimatedHeight = (170 * density).toInt()

        params.width = tooltipWidth
        params.x = (targetRect.centerX() - tooltipWidth / 2).coerceIn(
            (16 * density).toInt(),
            (screenWidth - tooltipWidth - (16 * density).toInt()).coerceAtLeast(0)
        )
        params.y = if (isTopSide) {
            (targetRect.top - tooltipEstimatedHeight - (12 * density).toInt()).coerceAtLeast((16 * density).toInt())
        } else {
            (targetRect.bottom + (12 * density).toInt()).coerceAtMost(screenHeight - tooltipEstimatedHeight - (16 * density).toInt())
        }
    }

    // 리액트 네이티브 화면은 같은 버튼 좌표가 몇 px씩 흔들리므로 8px 미만 이동은 무시
    private fun Rect.isNear(other: Rect?): Boolean {
        if (other == null) return false
        val tolerance = 8
        return abs(left - other.left) < tolerance && abs(top - other.top) < tolerance &&
            abs(right - other.right) < tolerance && abs(bottom - other.bottom) < tolerance
    }

    // 스크롤 중에는 무거운 전체 탐색 대신 현재 타깃 노드 좌표만 새로 읽어 링을 따라 움직이고, 멈추면 전체 탐색으로 확정
    private fun followTargetOnScroll() {
        handler.removeCallbacks(scrollSettled)
        handler.postDelayed(scrollSettled, 300)
        val node = trackedNode ?: return
        val label = shownLabel ?: return
        val rect = Rect()
        if (!node.refresh() || !node.isVisibleToUser) {
            // 타깃이 화면 밖으로 나가면 엉뚱한 곳에 남지 않도록 숨김 (멈추면 scrollSettled가 다시 찾음)
            lastTargetRect = null
            removeSpotlightOverlay()
            return
        }
        node.getBoundsInScreen(rect)
        if (rect.width() > 10 && rect.height() > 10 && !rect.isNear(lastTargetRect)) {
            lastTargetRect = rect
            showSpotlightOverlay(targetRect = rect, nodeText = label)
        }
    }

    private fun removeSpotlightOverlay() {
        if (ringView != null) {
            try {
                windowManager.removeView(ringView)
            } catch (_: Exception) {}
            ringView = null
        }
        if (tooltipView != null) {
            try {
                windowManager.removeView(tooltipView)
            } catch (_: Exception) {}
            tooltipView = null
        }
        shownLabel = null
    }

    override fun onInterrupt() {
        removeSpotlightOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        removeSpotlightOverlay()
        tts?.stop()
        tts?.shutdown()
    }
}

/**
 * 타깃 버튼에 정확히 밀착되는 굵은 노란색 펄스 링 (클릭 100% 관통)
 */
@Composable
fun PulsingHighlightRing() {
    val infiniteTransition = rememberInfiniteTransition(label = "ringPulse")
    val pulseBorder by infiniteTransition.animateColor(
        initialValue = AmberAccent,
        targetValue = AmberDark,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderColor"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .border(4.dp, pulseBorder, RoundedCornerShape(12.dp))
    )
}

/**
 * 태블릿 너비(320dp+)를 확보하고 버튼 위/아래 외곽에 배치되는 스마트 말풍선
 */
@Composable
fun SmartTooltipBubble(
    nodeText: String,
    onDodgeToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NavyPrimary.copy(alpha = 0.90f))
            .border(2.dp, AmberAccent, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "👉", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "여기를 누르세요!",
                        color = AmberAccent,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // ↕ 위치 비키기 버튼
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { onDodgeToggle() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "↕ 비키기",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = nodeText,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 4
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "👇 노란색으로 반짝이는 버튼을 직접 눌러주세요",
                color = AmberAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
