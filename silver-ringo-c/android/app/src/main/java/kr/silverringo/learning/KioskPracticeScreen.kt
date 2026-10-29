package kr.silverringo.learning

import android.annotation.SuppressLint
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.util.Locale

private const val ORIGIN = "https://appassets.androidplatform.net"

/** 네트워크 대신 APK 내부 자산만 실행. origin 제한 메시지로 TTS/결과를 전달한다. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun KioskPracticeScreen(levelId: Int, onExit: () -> Unit, onResult: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val latestExit by rememberUpdatedState(onExit)
    val latestResult by rememberUpdatedState(onResult)
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var ttsReady by remember { mutableStateOf(false) }
    var webView: WebView? by remember { mutableStateOf(null) }
    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val language = tts?.setLanguage(Locale.KOREAN)
                ttsReady = language != null && language >= TextToSpeech.LANG_AVAILABLE
            }
        }
        tts = engine
        onDispose { engine.stop(); engine.shutdown(); tts = null }
    }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) { tts?.stop(); webView?.onPause() }
            if (event == Lifecycle.Event.ON_RESUME) webView?.onResume()
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            webView?.apply { stopLoading(); destroy() }
            webView = null
        }
    }
    AndroidView(modifier = modifier.fillMaxSize(), factory = {
        WebView(context).apply {
            webView = this
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.blockNetworkLoads = true
            val loader = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context)).build()
            webViewClient = object : WebViewClientCompat() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
                    return loader.shouldInterceptRequest(request.url)
                        ?: WebResourceResponse("text/plain", "UTF-8", 403, "Blocked", emptyMap(), ByteArrayInputStream(ByteArray(0)))
                }
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true
            }
            if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
                WebViewCompat.addWebMessageListener(this, "SilverRingoNative", setOf(ORIGIN)) { _, message, source, isMainFrame, _ ->
                    if (isMainFrame && source.scheme == "https" && source.host == Uri.parse(ORIGIN).host) {
                        runCatching {
                            val data = JSONObject(message.data ?: "{}")
                            when (data.optString("type")) {
                                "exit" -> latestExit()
                                "result" -> {
                                    // Bind completion to the level the native host actually launched.
                                    if (data.optInt("levelId") == levelId) latestResult(data.toString())
                                }
                                "stopSpeech" -> tts?.stop()
                                "speak" -> if (ttsReady) {
                                    tts?.setSpeechRate(0.85f)
                                    tts?.speak(data.optString("text").take(1000), TextToSpeech.QUEUE_FLUSH, null, "sr-guide")
                                } else Toast.makeText(context, "한국어 음성을 사용할 수 없어요. 화면 안내를 읽어 주세요.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            } else Toast.makeText(context, "Android System WebView를 업데이트해야 음성·진도 연동을 사용할 수 있어요.", Toast.LENGTH_LONG).show()
            loadUrl("$ORIGIN/assets/kiosk/index.html?embed=1&level=$levelId")
        }
    })
}
