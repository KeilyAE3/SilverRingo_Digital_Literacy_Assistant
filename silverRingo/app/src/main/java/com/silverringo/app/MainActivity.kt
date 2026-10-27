package com.silverringo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.silverringo.app.ui.theme.SilverRingoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        setContent { SilverRingoTheme { SilverRingoApp() } }
    }
}

@Composable
fun SilverRingoApp() {
    var destination by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = destination != null) { destination = null }
    if (destination == null) {
        HomeScreen(
            onLearnClick = { destination = "IT 기능 배우기" },
            onGuideClick = { destination = "생활 가이드" }
        )
    } else {
        // 실제 시스템이 준비되면 이 임시 화면만 교체합니다.
        Scaffold { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(destination.orEmpty(), style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(16.dp))
                Text("이곳에 새로운 화면이 연결될 예정이에요.")
                Spacer(Modifier.height(24.dp))
                Button(onClick = { destination = null }, modifier = Modifier.heightIn(min = 56.dp)) {
                    Text("홈으로 돌아가기")
                }
            }
        }
    }
}
