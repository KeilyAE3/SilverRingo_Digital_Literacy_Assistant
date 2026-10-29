package kr.silverringo.learning.phone

import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.json.JSONObject
import java.util.Locale
import kotlin.math.roundToInt

internal val phoneSaver = Saver<PhoneState, String>(save = { s ->
    JSONObject().put("lesson", s.lesson.id).put("stage", s.stage).put("guided", s.guided)
        .put("brightness", s.brightness).put("volume", s.volume).put("number", s.number)
        .put("name", s.name).put("query", s.query).put("message", s.message)
        .put("feedback", s.feedback).put("mistakes", s.mistakes).put("hints", s.hints).put("actions", s.actions).put("evidence", s.evidence).toString()
}, restore = { text -> runCatching {
    val j = JSONObject(text)
    PhoneState(PhoneLesson.fromId(j.getString("lesson")), j.getInt("stage"), j.getBoolean("guided"),
        j.getInt("brightness"), j.getInt("volume"), j.getString("number"), j.getString("name"),
        j.getString("query"), j.getString("message"), j.getString("feedback"),
        j.getInt("mistakes"), j.getInt("hints"), j.getInt("actions"), j.optString("evidence", "simulation"))
}.getOrNull() })

@Composable
internal fun GuidedSimulationScreen(lesson: PhoneLesson, onExit: () -> Unit, onCompleted: (PhoneState) -> Unit, guided: Boolean = true) {
    var state by rememberSaveable(lesson.id, stateSaver = phoneSaver) { mutableStateOf(PhoneState(lesson, guided = guided)) }
    var started by rememberSaveable(lesson.id) { mutableStateOf(true) }
    var reported by rememberSaveable(lesson.id) { mutableStateOf(false) }
    val latestCompleted by rememberUpdatedState(onCompleted)
    val speak = rememberPhoneSpeaker()
    val scroll = rememberScrollState()
    LaunchedEffect(state.stage, started) { scroll.scrollTo(0) }
    LaunchedEffect(state.complete) {
        if(state.complete && !reported) { latestCompleted(state); reported = true }
    }
    val back: () -> Unit = {
        if(started && !state.complete && state.stage > 0) state = reducePhone(state, PhoneAction.Back)
        else if(started && !state.complete) onExit()
        else onExit()
    }
    BackHandler(onBack = back)
    val act: (PhoneAction) -> Unit = { state = reducePhone(state, it) }
    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("스마트폰 교육 › 1주차", fontSize = 18.sp)
        Text(lesson.title, fontSize = 28.sp, modifier = Modifier.semantics { heading() })
        if(!started) {
            Text(lesson.concept, fontSize = 22.sp)
            Text("이번 목표: ${lesson.task}", fontSize = 22.sp)
            Text("연습용 가상 스마트폰이에요. 실제 기기의 설정·연락처는 바뀌지 않고 전화·문자도 발송되지 않아요.", fontSize = 18.sp)
            PhoneButton(if(state.guided) "✓ 안내와 함께 배우기" else "안내와 함께 배우기") { state = state.copy(guided = true) }
            PhoneButton(if(!state.guided) "✓ 혼자 연습하기" else "혼자 연습하기") { state = state.copy(guided = false) }
            PhoneButton("연습 시작") { started = true }
        } else if(state.complete) {
            Text("연습을 완료했어요!", fontSize = 28.sp)
            Text("${lesson.title}의 모든 조작을 직접 해봤어요.", fontSize = 22.sp)
            Text("힌트 ${state.hints}회 · 다시 확인한 조작 ${state.mistakes}회", fontSize = 19.sp)
            PhoneButton("다시 연습하기") { state = PhoneState(lesson, guided = state.guided); reported = false; started = false }
            PhoneButton("1주차 학습 목록") { onExit() }
        } else {
            Text("${state.stage + 1} / ${lesson.guides.size} 단계 · ${if(state.guided) "학습 모드" else "혼자 연습"}", fontSize = 18.sp)
            Text(lesson.task, fontSize = 20.sp)
            if(state.guided) Text(lesson.guides[state.stage], fontSize = 23.sp, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F3F8))) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("가상 스마트폰 · 연습용", fontSize = 17.sp)
                    PhoneSimulation(state, act)
                }
            }
            if(state.feedback.isNotEmpty()) Text(state.feedback, fontSize = 22.sp, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            PhoneButton("힌트 보기") { act(PhoneAction.Hint) }
            PhoneButton("음성 안내 듣기") { act(PhoneAction.Hint); speak(lesson.guides[state.stage]) }
            PhoneButton("처음부터 다시 연습") { state = PhoneState(lesson, guided = state.guided); reported = false }
        }
        OutlinedButton(onClick = back, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("이전으로", fontSize = 20.sp) }
        if(started && !state.complete) OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("학습 목록으로", fontSize = 20.sp) }
    }
}

@Composable
private fun PhoneSimulation(s: PhoneState, act: (PhoneAction) -> Unit) {
    val next = { act(PhoneAction.Continue) }
    when(s.lesson) {
        PhoneLesson.SETTINGS -> when(s.stage) {
            0 -> { Text("홈 화면", fontSize = 25.sp); PhoneButton("설정", next) }
            1 -> {
                Text("화면 밝기 ${s.brightness}%", fontSize = 24.sp)
                Box(Modifier.fillMaxWidth().height(70.dp).background(Color.Gray.copy(alpha = 1f - s.brightness / 125f)))
                Slider(value = s.brightness.toFloat(), onValueChange = { act(PhoneAction.Brightness((it / 10).roundToInt() * 10)) }, valueRange = 0f..100f, steps = 9, modifier = Modifier.semantics { contentDescription = "화면 밝기" })
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { Button(onClick = { act(PhoneAction.Brightness(s.brightness - 10)) }, modifier = Modifier.heightIn(min = 56.dp)) { Text("− 10%") }; Button(onClick = { act(PhoneAction.Brightness(s.brightness + 10)) }, modifier = Modifier.heightIn(min = 56.dp)) { Text("+ 10%") } }
                PhoneButton("밝기 확인", next)
            }
            2 -> {
                Text("소리 크기 ${s.volume}%", fontSize = 24.sp)
                Slider(value = s.volume.toFloat(), onValueChange = { act(PhoneAction.Volume((it / 10).roundToInt() * 10)) }, valueRange = 0f..100f, steps = 9, modifier = Modifier.semantics { contentDescription = "소리 크기" })
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { Button(onClick = { act(PhoneAction.Volume(s.volume - 10)) }, modifier = Modifier.heightIn(min = 56.dp)) { Text("− 10%") }; Button(onClick = { act(PhoneAction.Volume(s.volume + 10)) }, modifier = Modifier.heightIn(min = 56.dp)) { Text("+ 10%") } }
                PhoneButton("소리 확인", next)
            }
        }
        PhoneLesson.CALLS -> when(s.stage) {
            0 -> PhoneButton("전화", next)
            1 -> {
                Text(s.number.ifEmpty { "전화번호를 입력하세요" }, fontSize = 24.sp)
                listOf("123", "456", "789", "*0#").forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { digit -> Button(onClick = { act(PhoneAction.Number(s.number + digit)) }, modifier = Modifier.weight(1f).heightIn(min = 64.dp)) { Text(digit.toString(), fontSize = 26.sp) } }
                    }
                }
                PhoneButton("한 글자 지우기") { act(PhoneAction.Number(s.number.dropLast(1))) }
                PhoneButton("전화 걸기", next)
            }
            2 -> { Text("연습 친구와 모의 통화 중", fontSize = 24.sp); PhoneButton("통화 종료", next) }
            3 -> { Text("연습 친구에게 전화가 왔어요", fontSize = 24.sp); PhoneButton("받기", next) }
            4 -> { Text("전화를 받았어요 · 모의 통화 중", fontSize = 24.sp); PhoneButton("통화 종료", next) }
        }
        PhoneLesson.CONTACTS -> when(s.stage) {
            0 -> PhoneButton("연락처", next)
            1 -> { Text("연락처 목록이 비어 있어요", fontSize = 22.sp); PhoneButton("새 연락처 추가", next) }
            2 -> {
                PhoneField("이름", s.name, { act(PhoneAction.Name(it)) })
                PhoneField("전화번호", s.number, { act(PhoneAction.Number(it)) }, true)
                PhoneButton("저장", next)
            }
            3 -> { Text("${s.name} 연락처를 저장했어요", fontSize = 22.sp); PhoneField("이름 검색", s.query, { act(PhoneAction.Query(it)) }); PhoneButton("검색", next) }
            4 -> { Text("검색 결과 1개", fontSize = 22.sp); PhoneButton("${s.name}\n${s.number}", next) }
        }
        PhoneLesson.MESSAGES -> when(s.stage) {
            0 -> PhoneButton("문자", next)
            1 -> { Text("받은 문자 1개", fontSize = 22.sp); PhoneButton("연습 친구 · 읽지 않은 문자", next) }
            2 -> { Text("연습 친구: 잘 지내세요?", fontSize = 24.sp); PhoneButton("새 문자 쓰기", next) }
            3 -> { PhoneField("받는 전화번호", s.number, { act(PhoneAction.Number(it)) }, true); PhoneField("문자 내용", s.message, { act(PhoneAction.Message(it)) }); PhoneButton("보내기", next) }
            4 -> { Text("나: ${s.message}\n전송 완료 (모의 문자)", fontSize = 22.sp); PhoneButton("연습 친구의 새 답장이 도착했어요", next) }
            5 -> { Text("연습 친구: 반가워요! 다음에 만나요.", fontSize = 24.sp); PhoneButton("읽음 확인", next) }
        }
    }
}

@Composable
private fun PhoneField(label: String, value: String, onChange: (String) -> Unit, phone: Boolean = false) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label, fontSize = 18.sp) },
        modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 22.sp),
        keyboardOptions = KeyboardOptions(keyboardType = if(phone) KeyboardType.Phone else KeyboardType.Text))
}
@Composable
internal fun PhoneButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)) { Text(label, fontSize = 22.sp) }
}

@Composable
internal fun rememberPhoneSpeaker(): (String) -> Unit {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ready by remember { mutableStateOf(false) }
    DisposableEffect(lifecycle) {
        var disposed = false
        val engine = TextToSpeech(context) { status ->
            if(!disposed && status == TextToSpeech.SUCCESS) ready = (tts?.setLanguage(Locale.KOREAN) ?: -1) >= 0
        }
        tts = engine
        val observer = LifecycleEventObserver { _, event -> if(event == Lifecycle.Event.ON_PAUSE) engine.stop() }
        lifecycle.addObserver(observer)
        onDispose { disposed = true; lifecycle.removeObserver(observer); engine.stop(); engine.shutdown(); tts = null; ready = false }
    }
    return { text -> if(ready) { tts?.setSpeechRate(0.85f); tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "smartphone") }
        else Toast.makeText(context, "한국어 음성을 사용할 수 없어요. 화면 안내를 읽어 주세요.", Toast.LENGTH_LONG).show() }
}
