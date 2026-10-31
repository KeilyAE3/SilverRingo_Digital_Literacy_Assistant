package kr.silverringo.learning

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject

/** A파트 진입점. A의 메뉴/내비게이션 구현 없이 이 화면만 호출한다. */
@Composable
fun KioskLearningEntry(onExit: () -> Unit, onResult: (String) -> Unit = {}) {
    val context = LocalContext.current
    val lessons = remember {
        val json = JSONObject(context.assets.open("kiosk/content.json").bufferedReader().use { it.readText() })
        val levels = json.getJSONArray("levels")
        List(levels.length()) { index -> levels.getJSONObject(index) }
    }
    val prefs = remember { context.getSharedPreferences("sr_learning", Context.MODE_PRIVATE) }
    var level by rememberSaveable { mutableStateOf<Int?>(null) }
    var revision by remember { mutableIntStateOf(0) }
    BackHandler { if (level != null) level = null else onExit() }
    val current = level
    if (current != null) {
        KioskPracticeScreen(levelId = current, onExit = { level = null }, onResult = { text ->
            completedLesson(text, current)?.let { id ->
                prefs.edit().putBoolean("kiosk4_done_$id", true).putString("kiosk4_last_result_$id", text).apply()
                revision++
            }
            onResult(text)
        })
        return
    }
    // revision makes the list recompute after a WebView completion callback.
    key(revision) {
        LazyColumn(modifier = Modifier.fillMaxSize().background(Color(0xFFF3F5F9)), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Column(Modifier.fillMaxWidth().background(Color(0xFF211C50), RoundedCornerShape(24.dp)).padding(24.dp)) {
                    Text("Silver Ringo", color = Color(0xFFFFD337), fontSize = 30.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("키오스크 배우기", color = Color.White, fontSize = 28.sp)
                    Text("한 단계씩, 천천히 연습해요.", color = Color.White, fontSize = 21.sp)
                }
            }
            items(lessons) { lesson ->
                val id = lesson.getInt("id")
                val done = prefs.getBoolean("kiosk4_done_$id", false)
                val unlocked = id == 1 || prefs.getBoolean("kiosk4_done_${id - 1}", false)
                Button(onClick = { level = id }, enabled = unlocked,
                    colors = learningButtonColors(),
                    shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp)) {
                    Column(Modifier.fillMaxWidth().padding(8.dp)) {
                        Text("${if (done) "✓" else if (unlocked) "$id" else "잠김"}  ${lesson.getString("title")}", fontSize = 23.sp, color = LocalContentColor.current)
                        Text(if (unlocked) lesson.getString("description") else "앞 단계를 완료하면 열려요", fontSize = 18.sp, color = LocalContentColor.current)
                    }
                }
            }
            item { OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text("학습 나가기", fontSize = 22.sp, color = LocalContentColor.current) } }
        }
    }
}
