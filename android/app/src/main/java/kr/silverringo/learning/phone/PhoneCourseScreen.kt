package kr.silverringo.learning.phone

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Teaching -> guided exercise -> separate final assessment, with distinct completion evidence. */
@Composable
fun PhonePracticeScreen(lesson: PhoneLesson,onExit: () -> Unit,onCompleted: (PhoneState) -> Unit) {
    var phase by rememberSaveable(lesson.id) { mutableStateOf("teach") }
    var page by rememberSaveable(lesson.id) { mutableIntStateOf(0) }
    var reported by rememberSaveable(lesson.id) { mutableStateOf(false) }
    var outcome by rememberSaveable(lesson.id,stateSaver=phoneSaver) { mutableStateOf(PhoneState(lesson)) }
    var guidedHints by rememberSaveable(lesson.id) { mutableIntStateOf(0) }
    val callback by rememberUpdatedState(onCompleted)
    val speak=rememberPhoneSpeaker()
    val cards=teachingCards(lesson)
    val back: () -> Unit = {
        when(phase) {
            "teach" -> if(page>0) page-- else onExit()
            "guided", "guidedDevice" -> phase="guidedIntro"
            "finalSim", "finalDevice" -> phase="finalIntro"
            "finalIntro" -> phase="guidedIntro"
            "guidedIntro" -> phase="teach"
            else -> onExit()
        }
    }
    BackHandler(onBack=back)
    LaunchedEffect(phase) { if(phase=="result" && !reported) { callback(outcome);reported=true } }
    fun finish(s: PhoneState) { outcome=s.copy(hints=s.hints+guidedHints);phase="result" }
    if(phase=="guided") {
        GuidedSimulationScreen(lesson,onExit={phase="guidedIntro"},onCompleted={guidedHints=it.hints;phase="finalIntro"},guided=true);return
    }
    if(phase=="finalSim") {
        GuidedSimulationScreen(lesson,onExit={phase="finalIntro"},onCompleted={finish(it.copy(evidence="simulation_final"))},guided=false);return
    }
    if(phase=="guidedDevice" || (phase=="finalDevice" && lesson==PhoneLesson.SETTINGS)) {
        DeviceSettingsPractice(guided=phase=="guidedDevice",onFinished={ evidence ->
            if(phase=="guidedDevice") phase="finalIntro" else finish(PhoneState(lesson,stage=lesson.guides.size,guided=false,evidence=evidence))
        },onBack=back);return
    }
    if(phase=="finalDevice") {
        ExternalAppPractice(lesson,onFinished={evidence -> finish(PhoneState(lesson,stage=lesson.guides.size,guided=false,evidence=evidence))},onBack=back);return
    }
    val scroll=rememberScrollState()
    LaunchedEffect(phase,page) { scroll.scrollTo(0) }
    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text("스마트폰 교육 › 1주차",fontSize=18.sp)
        Text(lesson.title,fontSize=28.sp)
        Text("① 배우기 → ② 따라 연습 → ③ 최종 실습",fontSize=18.sp)
        when(phase) {
            "teach" -> {
                Text("배우기 ${page+1} / ${cards.size}",fontSize=21.sp)
                Text(cards[page].title,fontSize=25.sp)
                PhoneDiagram(cards[page].picture)
                Text(cards[page].text,fontSize=22.sp)
                Text("갤럭시 A16급 보급형 · Android 14 / One UI 6 계열의 기본 조작을 참고한 설명 그림이에요. 업데이트·기종에 따라 아이콘과 메뉴 위치는 달라질 수 있어요.",fontSize=17.sp)
                PhoneButton("설명 듣기") { speak(cards[page].title+". "+cards[page].text) }
                PhoneButton(if(page<cards.lastIndex) "다음 설명" else "배웠어요 · 따라 연습으로") { if(page<cards.lastIndex) page++ else phase="guidedIntro" }
            }
            "guidedIntro" -> {
                Text("이제 안내를 보며 직접 연습해요.",fontSize=24.sp)
                if(lesson==PhoneLesson.SETTINGS) PhoneButton("내 휴대폰 버튼·설정으로 따라 연습") { phase="guidedDevice" }
                PhoneButton("가상 화면에서 따라 연습") { phase="guided" }
                Text("가상 연습은 실제 발신·저장 없이 순서를 익히는 곳이에요. 완료 후 최종 실습이 이어집니다.")
            }
            "finalIntro" -> {
                Text("따라 연습을 마쳤어요!",fontSize=26.sp)
                Text("이번에는 단계별 안내 없이 스스로 해보세요. 실습 방법을 선택할 수 있어요.",fontSize=22.sp)
                PhoneButton(if(lesson==PhoneLesson.SETTINGS) "내 휴대폰에서 최종 실습" else "실제 기본 앱에서 최종 실습") { phase="finalDevice" }
                PhoneButton("가상 화면에서 최종 실습") { phase="finalSim" }
                Text("실제 통화·문자는 상대가 연습에 참여할 수 있을 때 선택하세요. 앱 연결만으로 성공 처리하지 않아요.",fontSize=18.sp)
            }
            "result" -> {
                Text("교육과 최종 실습을 완료했어요!",fontSize=27.sp)
                Text(when(outcome.evidence) {
                    "device_values_and_font_choice" -> "소리·밝기의 실제 값 변화와 글자 크기 선택을 확인했어요."
                    "user_confirmed_external_app" -> "실제 앱에서 수행했다고 직접 확인했어요. 자동 전송·수신 검증 결과는 아닙니다."
                    else -> "가상 화면에서 목표대로 수행했어요. 실제 기기 수행과 구분해 기록합니다."
                },fontSize=22.sp)
                PhoneButton("처음부터 다시 배우기") { page=0;phase="teach";reported=false;guidedHints=0;outcome=PhoneState(lesson) }
                PhoneButton("1주차 학습 목록") { onExit() }
            }
        }
        OutlinedButton(onClick=back,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) { Text("이전으로",fontSize=20.sp) }
    }
}
