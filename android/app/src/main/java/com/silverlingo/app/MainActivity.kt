package com.silverlingo.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.silverringo.app.HomeScreen
import com.silverlingo.app.chat.ChatScreen
import kr.silverringo.learning.LearningEntry
import com.silverlingo.app.service.FloatingCompanionService
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.silverlingo.app.ui.screens.MainDashboardScreen
import com.silverlingo.app.ui.theme.SilverLingoTheme
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SilverLingoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SilverRingoApp()
                }
            }
        }
    }

    /**
     * 접근성 서비스 설정 화면으로 바로 이동
     */
    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }
}

@Composable
fun SilverRingoApp() {
    var destination by rememberSaveable { mutableStateOf("home") }
    var chatQuestion by rememberSaveable { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current
    SideEffect {
        (context as? android.app.Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = destination != "home"
        }
    }
    val home = {
        context.stopService(Intent(context, FloatingCompanionService::class.java))
        destination = "home"
    }
    BackHandler(enabled = destination == "guide", onBack = home)
    when (destination) {
        "chat" -> ChatScreen(onExit = home, initialMessage = chatQuestion)
        "learning" -> Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            LearningEntry(onExit = home, onResult = { result ->
                context.getSharedPreferences("sr_learning", android.content.Context.MODE_PRIVATE)
                    .edit().putString("latest_result", result).apply()
            })
        }
        "guide" -> Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            TextButton(onClick = home, modifier = Modifier.fillMaxWidth()) { Text("홈으로 돌아가기") }
            MainDashboardScreen()
        }
        else -> HomeScreen(
            onLearnClick = { destination = "learning" },
            onGuideClick = { destination = "guide" },
            onChatClick = { question ->
                context.stopService(Intent(context, FloatingCompanionService::class.java))
                chatQuestion = question
                destination = "chat"
            }
        )
    }
}
