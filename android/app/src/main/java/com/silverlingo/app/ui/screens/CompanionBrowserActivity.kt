package com.silverlingo.app.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.silverlingo.app.service.FloatingCompanionService
import com.silverlingo.app.service.OverlayScriptProvider
import com.silverlingo.app.ui.theme.*
import java.util.Locale

class CompanionBrowserActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var lastSpeechText = "실버링고입니다. 화면에 노란색으로 반짝이는 버튼을 눌러주세요."
    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)

        val targetUrl = intent.getStringExtra("EXTRA_TARGET_URL") ?: "https://www.ddocdoc.com/"
        val serviceKey = intent.getStringExtra("EXTRA_SERVICE_KEY") ?: "ddocdoc"
        val serviceTitle = intent.getStringExtra("EXTRA_SERVICE_TITLE") ?: "병원 안심 당일 접수"

        setContent {
            SilverLingoTheme {
                CompanionBrowserScreen(
                    targetUrl = targetUrl,
                    serviceKey = serviceKey,
                    serviceTitle = serviceTitle,
                    onBackClick = { finish() },
                    onReplayTts = { replayTts() },
                    onWebViewCreated = { wv -> webView = wv }
                )
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.KOREAN
            tts?.setSpeechRate(0.9f)
        }
    }

    fun speakText(text: String) {
        lastSpeechText = text
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "SilverLingoCompanionTTS")
    }

    fun replayTts() {
        speakText(lastSpeechText)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 화면 회전 등 재생성이 아니라 실제로 나갈 때만 '동행 중' 위젯 종료
        if (isFinishing) stopService(Intent(this, FloatingCompanionService::class.java))
        tts?.stop()
        tts?.shutdown()
        webView?.destroy()
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun speak(text: String) {
            runOnUiThread {
                speakText(text)
            }
        }

        @JavascriptInterface
        fun onStepChanged(stepId: String, title: String) {
            // 필요 시 UI 상태 갱신
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CompanionBrowserScreen(
    targetUrl: String,
    serviceKey: String,
    serviceTitle: String,
    onBackClick: () -> Unit,
    onReplayTts: () -> Unit,
    onWebViewCreated: (WebView) -> Unit
) {
    val context = LocalContext.current
    var currentWebTitle by remember { mutableStateOf(serviceTitle) }
    var currentStepGuidance by remember { mutableStateOf("노란색 테두리 버튼을 찾아 누르세요") }
    var zoomLevel by remember { mutableStateOf(100) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🔒 $serviceTitle",
                                color = AmberAccent,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = currentStepGuidance,
                            color = Color.White,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "홈으로 돌아가기",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                actions = {
                    // 음성 다시 듣기 버튼
                    Button(
                        onClick = onReplayTts,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberAccent,
                            contentColor = AmberText
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "음성 다시 듣기",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "다시듣기", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        // 1. 쿠키 및 서드파티 인증 세션 활성화
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        // 2. 카카오톡/인증 연동을 위한 웹뷰 세부 설정
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            textZoom = zoomLevel
                            builtInZoomControls = true
                            displayZoomControls = false
                            setSupportMultipleWindows(true)
                            javaScriptCanOpenWindowsAutomatically = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                        }

                        val activity = ctx as? CompanionBrowserActivity
                        if (activity != null) {
                            addJavascriptInterface(activity.WebAppInterface(), "SilverLingoAndroid")
                        }

                        // 3. 카카오톡 intent:// 및 외부 스킴 완벽 처리 WebViewClient
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val reqUrl = request?.url?.toString() ?: return false
                                val act = ctx as? Activity ?: return false
                                return handleCustomSchemeOrIntent(act, view, reqUrl)
                            }

                            @Suppress("DEPRECATION")
                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                if (url == null) return false
                                val act = ctx as? Activity ?: return false
                                return handleCustomSchemeOrIntent(act, view, url)
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)

                                // 1. CSS 주입
                                val css = OverlayScriptProvider.getOverlayCss().replace("\n", " ").replace("'", "\\'")
                                val injectCssJs = """
                                    (function() {
                                        var style = document.getElementById('silverlingo-injected-style');
                                        if (!style) {
                                            style = document.createElement('style');
                                            style.id = 'silverlingo-injected-style';
                                            style.type = 'text/css';
                                            style.innerHTML = '$css';
                                            document.head.appendChild(style);
                                        }
                                    })();
                                """.trimIndent()
                                view?.evaluateJavascript(injectCssJs, null)

                                // 2. JS 오버레이 엔진 주입
                                val overlayJs = OverlayScriptProvider.getOverlayJs(serviceKey)
                                view?.evaluateJavascript(overlayJs, null)
                            }
                        }

                        // 4. 팝업 창(window.open) 및 타이틀 처리 WebChromeClient
                        webChromeClient = object : WebChromeClient() {
                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                super.onReceivedTitle(view, title)
                                if (!title.isNullOrBlank()) {
                                    currentWebTitle = title
                                }
                            }

                            override fun onCreateWindow(
                                view: WebView?,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: android.os.Message?
                            ): Boolean {
                                val popupWebView = WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(v: WebView?, req: WebResourceRequest?): Boolean {
                                            val pUrl = req?.url?.toString() ?: return false
                                            val act = ctx as? Activity ?: return false
                                            return handleCustomSchemeOrIntent(act, view, pUrl)
                                        }
                                    }
                                }
                                val transport = resultMsg?.obj as? WebView.WebViewTransport
                                transport?.webView = popupWebView
                                resultMsg?.sendToTarget()
                                return true
                            }
                        }

                        loadUrl(targetUrl)
                        onWebViewCreated(this)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * 카카오톡(kakaotalk://, intent://), 금융인증서, 전화 등 외부 스킴 및 앱 인텐트 완벽 처리
 */
private fun handleCustomSchemeOrIntent(activity: Activity, view: WebView?, url: String): Boolean {
    // 1. 일반 웹 주소는 웹뷰 자체에서 로드
    if (url.startsWith("http://") || url.startsWith("https://")) {
        return false
    }

    // 2. 카카오톡, PASS, 금융인증 등 intent:// 스킴 처리
    if (url.startsWith("intent:")) {
        try {
            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
            if (intent != null) {
                // 카카오톡 등 설치된 앱이 있는 경우 실행
                val packageManager = activity.packageManager
                val resolveInfo = packageManager.resolveActivity(intent, 0)
                if (resolveInfo != null) {
                    activity.startActivity(intent)
                    return true
                }

                // 앱이 미설치된 경우: browser_fallback_url (웹용 로그인/인증 페이지) 시도
                val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                if (!fallbackUrl.isNullOrEmpty()) {
                    view?.loadUrl(fallbackUrl)
                    return true
                }

                // fallbackUrl도 없을 경우 Play Store 마켓 페이지 열기
                val packageName = intent.`package`
                if (!packageName.isNullOrEmpty()) {
                    try {
                        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                        activity.startActivity(marketIntent)
                    } catch (_: Exception) {
                        val webMarketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                        activity.startActivity(webMarketIntent)
                    }
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true
    }

    // 3. kakaotalk://, market://, tel:// 등 커스텀 스킴 처리
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        activity.startActivity(intent)
        return true
    } catch (e: Exception) {
        // 카카오톡 미설치 기기에서 kakaotalk:// 호출 시 웹 로그인 창으로 자동 유도
        if (url.startsWith("kakaotalk:")) {
            view?.loadUrl("https://accounts.kakao.com/login")
            return true
        }
        e.printStackTrace()
    }

    return true
}
