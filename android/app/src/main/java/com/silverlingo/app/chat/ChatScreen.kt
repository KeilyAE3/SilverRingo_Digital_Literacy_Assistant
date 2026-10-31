package com.silverlingo.app.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silverlingo.app.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ChatScreen(onExit: () -> Unit, initialMessage: String = "", serverUrl: String = BuildConfig.CHAT_BASE_URL) {
    val serviceReady = serverUrl.isNotBlank()
    var input by rememberSaveable { mutableStateOf(initialMessage) }
    var messages by rememberSaveable { mutableStateOf(listOf<String>()) }
    var sources by rememberSaveable { mutableStateOf(listOf<String>()) }
    var suggestions by rememberSaveable { mutableStateOf(listOf<String>()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }
    var initialHandled by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    val client = remember { ChatClient(allowHttp = BuildConfig.DEBUG) }
    BackHandler(onBack = onExit)

    fun send() {
        if (sending) return
        if (!serviceReady) return
        val question = input.trim()
        if (question.isBlank() || question.length > 1000) {
            error = "질문은 1~1,000자로 적어 주세요."
            return
        }
        val history = messages.mapIndexed { index, text ->
            ChatTurn(if (index % 2 == 0) "user" else "assistant", text.take(1000))
        }.takeLast(6)
        sending = true
        error = null
        scope.launch {
            try {
                val response = withContext(Dispatchers.IO) { client.send(serverUrl, question, history) }
                messages = (messages + listOf(question, response.reply)).takeLast(12)
                sources = response.sources.map { source ->
                    val category = when (source.category) { "learning" -> "학습 자료"; "assistant" -> "생활 가이드"; "app" -> "앱 안내"; else -> "도움말 자료" }
                    val status = when (source.status) { "implemented" -> "구현 자료"; "prototype" -> "시험 구현"; "placeholder" -> "준비 중"; "idea" -> "구상 단계"; else -> "참고 자료" }
                    "$category · $status"
                }.distinct()
                // D actions are currently disabled. Never treat server text as navigation authority.
                suggestions = response.actions.map { it.label }
                input = ""
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: ChatFailure) { error = "지금은 답변을 받기 어려워요. 잠시 후 다시 시도해 주세요."
            } finally { sending = false }
        }
    }

    LaunchedEffect(Unit) {
        if (!initialHandled) {
            initialHandled = true
            if (initialMessage.isNotBlank() && serviceReady) send()
        }
    }
    LaunchedEffect(messages.size, error, sending) { scroll.animateScrollTo(scroll.maxValue) }

    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("홈으로 돌아가기", fontSize = 20.sp) }
        Column(Modifier.weight(1f).verticalScroll(scroll).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("AI 도우미", style = MaterialTheme.typography.headlineMedium)
            Text("앱 사용법과 학습 내용을 물어보세요.", fontSize = 18.sp)
            if (!serviceReady) Text("챗봇 서비스 준비 중", fontSize = 22.sp)
            messages.forEachIndexed { index, text ->
                Card(colors = CardDefaults.cardColors(containerColor = if (index % 2 == 0) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(if (index % 2 == 0) "내 질문" else "도우미 답변", style = MaterialTheme.typography.titleMedium)
                        Text(text, fontSize = 20.sp)
                    }
                }
            }
            if (sources.isNotEmpty()) Text("참고: " + sources.joinToString(", "), fontSize = 16.sp)
            suggestions.forEach { Text("추천 안내: $it", fontSize = 18.sp) }
            if (sending) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("답변을 기다리고 있어요.", fontSize = 18.sp)
            }
            error?.let { text ->
                Text(text, color = MaterialTheme.colorScheme.error, fontSize = 18.sp)
                TextButton(onClick = { send() }, enabled = !sending && input.isNotBlank()) { Text("다시 시도", fontSize = 18.sp) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(input, { input = it }, label = { Text("질문 입력") }, enabled = serviceReady && !sending,
                supportingText = { Text("${input.length}/1,000자") }, modifier = Modifier.weight(1f), maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { send() }))
            Button(onClick = { send() }, enabled = serviceReady && !sending && input.isNotBlank() && input.length <= 1000,
                modifier = Modifier.heightIn(min = 56.dp)) { Text("보내기", fontSize = 18.sp) }
        }
    }
}
