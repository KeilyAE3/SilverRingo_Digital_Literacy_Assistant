package kr.silverringo.learning.phone

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay

private fun Context.activity(): Activity? = when(this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}
private fun launch(context: Context, intent: Intent): Boolean = runCatching { context.startActivity(intent); true }.getOrDefault(false)

@Composable
internal fun DeviceSettingsPractice(guided: Boolean, onFinished: (String) -> Unit, onBack: () -> Unit) {
    val context=LocalContext.current
    val audio=remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    var active by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    var lastVolume by rememberSaveable { mutableIntStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC)) }
    var rose by rememberSaveable { mutableStateOf(false) }
    var fell by rememberSaveable { mutableStateOf(false) }
    var baseline by rememberSaveable { mutableIntStateOf(-1) }
    var currentBrightness by remember { mutableIntStateOf(-1) }
    var brightnessArmed by rememberSaveable { mutableStateOf(false) }
    var brightnessChanged by rememberSaveable { mutableStateOf(false) }
    var fontSeen by rememberSaveable { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    fun readBrightness() = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, -1)
    DisposableEffect(lifecycle) {
        val activity=context.activity();val previous=activity?.volumeControlStream
        activity?.volumeControlStream=AudioManager.STREAM_MUSIC
        val observer=LifecycleEventObserver { _, _ -> active=lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); if(previous!=null) activity?.volumeControlStream=previous }
    }
    LaunchedEffect(active) {
        while(active) {
            val now=audio.getStreamVolume(AudioManager.STREAM_MUSIC)
            val changes=volumeEvidence(lastVolume, now)
            rose=rose || changes.first;fell=fell || changes.second;lastVolume=now
            currentBrightness=readBrightness()
            if(brightnessArmed && baseline>=0 && currentBrightness>=0 && currentBrightness!=baseline) brightnessChanged=true
            delay(400)
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text(if(guided) "실제 기기로 따라 연습" else "혼자 하는 최종 실습",fontSize=28.sp)
        Text("목표: 미디어 소리를 높이고 낮추기 · 밝기 바꾸기 · 글자 크기 선택하기",fontSize=22.sp)
        if(guided) { PhoneDiagram("volume");Text("휴대폰 옆면의 음량 버튼 위쪽과 아래쪽을 각각 눌러 보세요. 최댓값이면 먼저 낮추고, 최솟값이면 먼저 높입니다.") }
        Text("미디어 음량 $lastVolume / ${audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)}\n높이기 ${if(rose) "확인 ✓" else "기다리는 중"} · 낮추기 ${if(fell) "확인 ✓" else "기다리는 중"}",fontSize=22.sp)
        if(guided) Text("다음은 밝기예요. 아래 버튼을 누른 다음 설정 → 디스플레이에서 밝기를 움직이고 뒤로가기로 돌아오세요. 빠른 설정창에서 조절해도 돼요.")
        PhoneButton("밝기 연습 시작 · 설정 열기") {
            baseline=readBrightness();brightnessArmed=true;brightnessChanged=false
            if(!launch(context,Intent(Settings.ACTION_DISPLAY_SETTINGS))) notice="설정 화면을 열 수 없어요. 기기의 설정 앱에서 디스플레이를 찾아 주세요."
        }
        Text(if(brightnessChanged) "밝기 값 변화 확인 ✓" else if(currentBrightness<0) "이 기기에서는 밝기 값을 읽을 수 없어요. 가상 실습을 선택할 수 있어요." else "밝기 변경 후 이 화면으로 돌아오세요.",fontSize=20.sp)
        Text("자동 밝기로 인한 변화도 감지될 수 있어요. 확인 표시는 값의 변화이며 조작 경로를 확인한 것은 아니에요.",fontSize=17.sp)
        if(guided) Text("글자 크기: 설정 → 디스플레이 → 글자 크기와 스타일. 미리보기로 편한 크기를 찾으세요. 지금 크기가 편하면 유지해도 됩니다.")
        PhoneButton("글자 크기 설정 찾아보기") { if(!launch(context,Intent(Settings.ACTION_DISPLAY_SETTINGS))) notice="기기 설정 → 디스플레이에서 글자 크기를 찾아 주세요." }
        PhoneButton(if(fontSeen) "글자 크기 선택 확인 ✓" else "현재 크기를 유지 / 편한 크기로 확인했어요") { fontSeen=true }
        Text("글자 크기 변경은 필수가 아니에요. 소리·밝기도 마친 뒤 편한 상태로 맞춰 주세요.",fontSize=18.sp)
        if(notice.isNotEmpty()) Text(notice,modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite })
        Button(onClick={ onFinished("device_values_and_font_choice") }, enabled=settingsPracticePassed(rose,fell,brightnessChanged,fontSeen),modifier=Modifier.fillMaxWidth().heightIn(min=60.dp)) { Text(if(guided) "따라 연습 완료" else "최종 실습 완료",fontSize=22.sp) }
        OutlinedButton(onClick=onBack,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) { Text("연습 선택으로 돌아가기") }
    }
}

@Composable
internal fun ExternalAppPractice(lesson: PhoneLesson, onFinished: (String) -> Unit, onBack: () -> Unit) {
    val context=LocalContext.current
    var openedFirst by rememberSaveable { mutableStateOf(false) }
    var openedSecond by rememberSaveable { mutableStateOf(false) }
    var firstConfirmed by rememberSaveable { mutableStateOf(false) }
    var secondConfirmed by rememberSaveable { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    val labels=when(lesson) {
        PhoneLesson.CALLS -> listOf("전화 앱 열기", "전화 걸고 종료했어요", "상대에게 걸려 온 전화를 받아 종료했어요")
        PhoneLesson.CONTACTS -> listOf("새 연락처 화면 열기", "연습 친구의 이름과 번호를 저장·확인했어요", "연락처 앱에서 이름으로 찾아 번호를 확인했어요")
        else -> listOf("문자 작성 화면 열기", "받는 번호와 내용을 확인하고 직접 전송했어요", "상대의 답장을 대화방에서 읽었어요")
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text("실제 앱에서 최종 실습",fontSize=28.sp)
        Text(lesson.title,fontSize=24.sp)
        Text("실습 번호: 010-6344-2138",fontSize=24.sp)
        Text("실제 전화·문자는 상대와 연습할 수 있을 때 진행해요. 연결된 앱의 발신·전송·저장 버튼은 직접 누릅니다. 수신 실습에는 상대의 전화나 답장이 필요해요.")
        Text("완료는 본인 확인으로 기록해요. 앱 실행만으로 발신·수신·저장 성공을 판정하지 않습니다.",fontSize=19.sp)
        PhoneButton(labels[0]) {
            val intent=when(lesson) {
                PhoneLesson.CALLS -> Intent(Intent.ACTION_DIAL, Uri.parse("tel:$PRACTICE_PHONE"))
                PhoneLesson.CONTACTS -> Intent(Intent.ACTION_INSERT).setType(ContactsContract.Contacts.CONTENT_TYPE)
                    .putExtra(ContactsContract.Intents.Insert.NAME,"연습 친구").putExtra(ContactsContract.Intents.Insert.PHONE,PRACTICE_PHONE)
                else -> Intent(Intent.ACTION_SENDTO,Uri.parse("smsto:$PRACTICE_PHONE")).putExtra("sms_body","안녕하세요")
            }
            if(launch(context,intent)) openedFirst=true else notice="연결할 앱이 없어요. 가상 실습으로 연습할 수 있어요."
        }
        Button(onClick={firstConfirmed=true},enabled=openedFirst,modifier=Modifier.fillMaxWidth().heightIn(min=60.dp)) { Text((if(firstConfirmed) "✓ " else "")+labels[1],fontSize=21.sp) }
        if(lesson==PhoneLesson.CONTACTS) PhoneButton("연락처 목록 열기") {
            if(launch(context,Intent(Intent.ACTION_VIEW,ContactsContract.Contacts.CONTENT_URI))) openedSecond=true else notice="연락처 앱을 직접 열고 이름을 검색해 주세요."
        }
        Button(onClick={secondConfirmed=true},enabled=firstConfirmed && (lesson!=PhoneLesson.CONTACTS || openedSecond),modifier=Modifier.fillMaxWidth().heightIn(min=60.dp)) { Text((if(secondConfirmed) "✓ " else "")+labels[2],fontSize=21.sp) }
        if(notice.isNotEmpty()) Text(notice,modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite })
        Button(onClick={onFinished("user_confirmed_external_app")},enabled=externalPracticePassed(openedFirst,firstConfirmed,secondConfirmed),modifier=Modifier.fillMaxWidth().heightIn(min=60.dp)) { Text("최종 실습 완료",fontSize=22.sp) }
        OutlinedButton(onClick=onBack,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) { Text("연습 선택으로 돌아가기") }
    }
}
