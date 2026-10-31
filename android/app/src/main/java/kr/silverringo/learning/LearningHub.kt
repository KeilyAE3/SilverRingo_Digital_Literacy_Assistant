package kr.silverringo.learning

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.silverringo.learning.phone.*
import org.json.JSONObject

/** Stable host interface. Routes are owned by C, root navigation remains unchanged. */
@Composable
fun LearningEntry(onExit: () -> Unit, onResult: (String) -> Unit = {}) {
    LearningTheme { LearningHubContent(onExit, onResult) }
}

@Composable
private fun LearningHubContent(onExit: () -> Unit, onResult: (String) -> Unit) {
    var topic by rememberSaveable { mutableStateOf("") }
    var week by rememberSaveable { mutableIntStateOf(0) }
    var lessonId by rememberSaveable { mutableStateOf("") }
    val prefs = LocalContext.current.getSharedPreferences("sr_learning", Context.MODE_PRIVATE)
    var revision by remember { mutableIntStateOf(0) }
    val back: () -> Unit = {
        when { lessonId.isNotEmpty() -> lessonId = ""; week != 0 -> week = 0; topic.isNotEmpty() -> topic = ""; else -> onExit() }
    }
    BackHandler(onBack = back)
    if (topic == "kiosk") { KioskLearningEntry(onExit = { topic = "" }, onResult = onResult); return }
    if (lessonId.isNotEmpty()) {
        val lesson = PhoneLesson.fromId(lessonId)
        PhonePracticeScreen(lesson, onExit = back, onCompleted = { state ->
            if (state.complete && state.lesson == lesson) {
                val result = JSONObject().put("schemaVersion", 1).put("topicId", "smartphone")
                    .put("weekId", 1).put("lessonId", lesson.id).put("passed", true)
                    .put("mode", if (state.guided) "learn" else "test")
                    .put("completionEvidence", state.evidence).put("courseVersion", 3)
                    .put("hints", state.hints).put("mistakes", state.mistakes).put("actions", state.actions)
                prefs.edit().putBoolean("smartphone.week1.${lesson.id}.done", true)
                    .putString("smartphone.week1.${lesson.id}.result", result.toString()).apply()
                revision++
                onResult(result.toString())
            }
        })
        return
    }
    key(revision) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("Silver Ringo · 배움터", fontSize = 28.sp, color = LocalContentColor.current) }
            item { Text(when { topic.isEmpty() -> "무엇을 배워 볼까요?"; week == 0 -> "스마트폰 교육"; else -> "${week}주차 · ${phoneWeeks[week - 1].title}" }, fontSize = 25.sp, color = LocalContentColor.current) }
            item { Text("원하는 학습을 골라 천천히 연습해요. 시간제한은 없어요.", fontSize = 20.sp, color = LocalContentColor.current) }
            if (topic.isEmpty()) {
                item { HubButton("스마트폰 교육", "6개 주차 · 1주차 실습 4개", { topic = "phone" }) }
                item { HubButton("키오스크 배우기", "기존 햄버거 주문 연습", { topic = "kiosk" }) }
            } else if (week == 0) {
                phoneWeeks.forEach { w -> item { HubButton("${w.number}주차 · ${w.title}", if(w.number == 1) "4개 실습 시작 가능" else "교육 내용 보기 · 실습 준비 중", { week = w.number }) } }
                item { Text("피플에듀 스마트폰·SNS 교육의 6차시 구성을 참고했어요. 주차는 학습 분류이며 날짜 제한은 없어요.", fontSize = 16.sp, color = LocalContentColor.current) }
            } else {
                phoneWeeks[week - 1].lessons.forEachIndexed { i, title -> item {
                    val lesson = if(week == 1) PhoneLesson.entries[i] else null
                    val done = lesson != null && prefs.getBoolean("smartphone.week1.${lesson.id}.done", false)
                    HubButton(title, if(done) "완료 · 다시 연습하기" else if(lesson != null) "설명 → 직접 조작 → 결과 확인" else "실습 준비 중", { if(lesson != null) lessonId = lesson.id }, lesson != null)
                } }
            }
            item { OutlinedButton(onClick = back, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(if(topic.isEmpty()) "학습 나가기" else "이전 목록", fontSize = 20.sp, color = LocalContentColor.current) } }
        }
    }
}

@Composable
private fun HubButton(title: String, description: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, colors = learningButtonColors(), shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp)) {
        Column(Modifier.fillMaxWidth().padding(8.dp)) { Text(title, fontSize = 23.sp, color = LocalContentColor.current); Spacer(Modifier.height(8.dp)); Text(description, fontSize = 18.sp, color = LocalContentColor.current) }
    }
}
