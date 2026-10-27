package com.silverlingo.app.service

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.silverlingo.app.ui.theme.AmberAccent
import com.silverlingo.app.ui.theme.NavyDark
import com.silverlingo.app.ui.theme.NavyPrimary
import kotlin.math.hypot

class FloatingCompanionService : Service() {

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundServiceNotification()
        createFloatingWidget()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "silverlingo_overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "실버링고 실시간 동행 도우미",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("실버링고 실시간 동행 중")
                .setContentText("온라인 업무 위치를 화면 위에서 짚어드립니다.")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("실버링고 실시간 동행 중")
                .setContentText("온라인 업무 위치를 화면 위에서 짚어드립니다.")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()
        }

        startForeground(1001, notification)
    }

    // 위젯 상태: 펼침/미니 모드, 붙어 있는 가장자리(좌/우), 하단 [✕] 종료 영역 표시/호버 여부
    private var isExpanded by mutableStateOf(true)
    private var dockRight = true
    private var dismissActive by mutableStateOf(false)
    private var dismissHover by mutableStateOf(false)
    private var dismissView: ComposeView? = null
    private var snapAnimator: ValueAnimator? = null
    private lateinit var params: WindowManager.LayoutParams

    private val density get() = resources.displayMetrics.density
    private val dismissSizePx get() = (88 * density).toInt()
    private val dismissMarginPx get() = (48 * density).toInt()

    private fun createComposeView(content: @Composable () -> Unit) = ComposeView(this).apply {
        // Compose 수명주기 홀더 연결
        val lifecycleOwner = ServiceLifecycleOwner()
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_RESUME)

        setViewTreeLifecycleOwner(lifecycleOwner)
        setViewTreeViewModelStoreOwner(lifecycleOwner)
        setViewTreeSavedStateRegistryOwner(lifecycleOwner)

        setContent(content)
    }

    private fun createFloatingWidget() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // 미니 모드에서 🤖 탭을 화면 밖으로 반쯤 걸치려면 FLAG_LAYOUT_NO_LIMITS 필요
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = resources.displayMetrics.widthPixels
            y = (resources.displayMetrics.heightPixels * 0.72f).toInt()
        }

        // 하단 중앙 [✕] 종료 영역: 터치를 받지 않는 독립 창, 드래그 중에만 표시
        val dismissParams = WindowManager.LayoutParams(
            dismissSizePx,
            dismissSizePx,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dismissMarginPx
        }
        dismissView = createComposeView {
            DismissTarget(active = dismissActive, hovering = dismissHover)
        }.apply { visibility = View.GONE }

        composeView = createComposeView {
            FloatingWidgetContent(
                isExpanded = isExpanded,
                onToggle = { isExpanded = !isExpanded },
                onSizeChanged = { size -> snapTo(dockX(size.width)) },
                onDragStart = { onWidgetDragStart() },
                onDrag = { dx, dy ->
                    params.x += dx.toInt()
                    params.y += dy.toInt()
                    updateWidgetLayout()
                    updateDismissHover()
                },
                onDragEnd = { onWidgetDragEnd() }
            )
        }

        try {
            windowManager.addView(dismissView, dismissParams)
            windowManager.addView(composeView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun onWidgetDragStart() {
        snapAnimator?.cancel()
        dismissView?.visibility = View.VISIBLE
        dismissActive = true
    }

    private fun onWidgetDragEnd() {
        val overDismiss = dismissHover
        dismissActive = false
        dismissHover = false
        // 페이드아웃이 끝난 뒤 숨김
        dismissView?.postDelayed({ if (!dismissActive) dismissView?.visibility = View.GONE }, 250)

        if (overDismiss) {
            composeView?.visibility = View.GONE
            stopSelf()
            return
        }

        val view = composeView ?: return
        val screenW = resources.displayMetrics.widthPixels
        dockRight = params.x + view.width / 2 > screenW / 2
        // 화면 옆으로 밀어 일부가 화면 밖으로 나가면 미니 모드로 접기
        val pushedToEdge = params.x < 0 || params.x + view.width > screenW
        if (pushedToEdge && isExpanded) {
            isExpanded = false  // 크기가 바뀌면 onSizeChanged가 가장자리로 다시 붙여 줌
        } else {
            snapTo(dockX(view.width))
        }
    }

    private fun updateDismissHover() {
        val view = composeView ?: return
        val metrics = resources.displayMetrics
        val bubbleX = params.x + view.width / 2f
        val bubbleY = params.y + view.height / 2f
        val targetX = metrics.widthPixels / 2f
        val targetY = metrics.heightPixels - dismissMarginPx - dismissSizePx / 2f
        val hover = hypot(bubbleX - targetX, bubbleY - targetY) < dismissSizePx
        if (hover && !dismissHover) {
            dismissView?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
        dismissHover = hover
    }

    // 펼침: 가장자리 안쪽 여백 8dp / 미니: 🤖 탭이 화면 밖으로 반쯤 걸침
    private fun dockX(width: Int): Int {
        val screenW = resources.displayMetrics.widthPixels
        val margin = (8 * density).toInt()
        return when {
            !isExpanded && dockRight -> screenW - width / 2
            !isExpanded -> -width / 2
            dockRight -> screenW - width - margin
            else -> margin
        }
    }

    // 가장자리로 자석처럼 부드럽게 이동 (세로 위치는 화면 안으로만 보정)
    private fun snapTo(targetX: Int) {
        snapAnimator?.cancel()
        val view = composeView ?: return
        params.y = params.y.coerceIn(0, (resources.displayMetrics.heightPixels - view.height).coerceAtLeast(0))
        snapAnimator = ValueAnimator.ofInt(params.x, targetX).apply {
            duration = 220
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                params.x = it.animatedValue as Int
                updateWidgetLayout()
            }
            start()
        }
    }

    private fun updateWidgetLayout() {
        val view = composeView ?: return
        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        snapAnimator?.cancel()
        listOf(composeView, dismissView).forEach { view ->
            try {
                if (view != null) windowManager.removeView(view)
            } catch (_: Exception) {}
        }
        composeView = null
        dismissView = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

@Composable
fun FloatingWidgetContent(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSizeChanged: (IntSize) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit
) {
    Row(
        modifier = Modifier
            .onSizeChanged(onSizeChanged)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd,
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                )
            }
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        NavyPrimary.copy(alpha = 0.88f),
                        NavyDark.copy(alpha = 0.90f)
                    )
                )
            )
            .border(2.dp, AmberAccent.copy(alpha = 0.9f), RoundedCornerShape(32.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 🤖 로봇 아이콘 (터치 시 접기/펴기 토글)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🤖", fontSize = 24.sp)
        }

        AnimatedVisibility(visible = isExpanded) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 10.dp)
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "실버링고 동행 중",
                        color = AmberAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "입력 위치를 짚어드려요",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * 드래그 중 화면 하단 중앙에 나타나는 [✕ 종료] 영역 (버블을 올리면 확대 + 진한 빨강)
 */
@Composable
fun DismissTarget(active: Boolean, hovering: Boolean) {
    val alpha by animateFloatAsState(if (active) 1f else 0f, tween(200), label = "dismissAlpha")
    val scale by animateFloatAsState(if (hovering) 1.2f else 1f, tween(150), label = "dismissScale")
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)  // 확대(1.2배) 시 창 경계에 잘리지 않도록 여유
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(Color(0xFFDC2626).copy(alpha = if (hovering) 0.95f else 0.55f))
            .border(3.dp, Color.White.copy(alpha = 0.9f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "✕", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Text(text = "종료", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}
