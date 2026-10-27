package com.silverringo.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silverringo.app.ui.theme.SilverRingoTheme
import kotlinx.coroutines.launch

private val Navy = Color(0xFF211D4F)
private val Yellow = Color(0xFFFFCE32)

/** 호스트 화면에서 두 콜백에 각 시스템의 진입 동작을 연결합니다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLearnClick: () -> Unit,
    onGuideClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var message by rememberSaveable { mutableStateOf("") }
    var notice by rememberSaveable { mutableStateOf<String?>(null) }
    val showChatNotice = { if (message.isNotBlank()) notice = "AI 대화는 준비 중이에요. 아래에서 원하는 기능을 선택해 주세요." }
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(Modifier.widthIn(max = 340.dp)) {
                Text("Silver Ringo", Modifier.padding(24.dp), fontSize = 26.sp, fontWeight = FontWeight.Bold)
                HorizontalDivider()
                listOf("홈", "IT 기능 배우기", "생활 가이드").forEachIndexed { index, label ->
                    NavigationDrawerItem(
                        label = { Text(label, fontSize = 20.sp) },
                        selected = index == 0,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                when (index) { 1 -> onLearnClick(); 2 -> onGuideClick() }
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    title = { Text("Silver Ringo", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Yellow) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            LineIcon("menu", "전체 메뉴", Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy)
                )
            }
        ) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).imePadding()
                    .verticalScroll(rememberScrollState()).widthIn(max = 640.dp)
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Card(colors = CardDefaults.cardColors(containerColor = Navy), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("당신 곁의 디지털 도우미", color = Yellow, fontSize = 16.sp)
                                Spacer(Modifier.height(8.dp))
                                Text("어떤 것을\n도와드릴까요?", color = Color.White, fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
                            }
                            AssistantCharacter(Modifier.size(76.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = message, onValueChange = { message = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("궁금한 점을 적어 주세요") },
                                textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
                                shape = RoundedCornerShape(16.dp),
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                                    focusedBorderColor = Yellow, unfocusedBorderColor = Color.White,
                                    focusedLabelColor = Yellow, unfocusedLabelColor = Color(0xFF4F6078)
                                ),
                                trailingIcon = {
                                    IconButton(onClick = { showChatNotice() }, enabled = message.isNotBlank()) {
                                        LineIcon("send", "메시지 보내기", MaterialTheme.colorScheme.secondary)
                                    }
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = { showChatNotice() })
                            )
                            FilledIconButton(
                                onClick = { notice = "음성 입력은 준비 중이에요. 지금은 글로 입력해 주세요." },
                                modifier = Modifier.size(56.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Yellow, contentColor = Navy)
                            ) { LineIcon("mic", "음성 입력") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SuggestionChip(onClick = onLearnClick, label = { Text("IT 기능 배우기", color = Color.White, fontSize = 16.sp) })
                            SuggestionChip(onClick = onGuideClick, label = { Text("생활 가이드", color = Color.White, fontSize = 16.sp) })
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("디지털 생활, 함께 해요", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("원하는 기능을 선택해 주세요.", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FeatureCard("IT 기능 배우기", "스마트폰·디지털 기기\n사용법을 배워요.", "learn", Color(0xFFEAF5FB), onLearnClick)
                FeatureCard("생활 가이드", "하고 싶은 디지털 작업을\n한 단계씩 안내받아요.", "guide", Color(0xFFFFF6CF), onGuideClick)
                Text("자주 사용하는 기능", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFECEFF4)) {
                    Text("자주 쓰는 바로가기가 이곳에 추가될 예정이에요.", Modifier.fillMaxWidth().padding(20.dp), fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    notice?.let { text ->
        AlertDialog(onDismissRequest = { notice = null }, title = { Text("안내") },
            text = { Text(text, fontSize = 18.sp) },
            confirmButton = { TextButton(onClick = { notice = null }) { Text("확인", fontSize = 18.sp) } })
    }
}

@Composable
private fun FeatureCard(title: String, description: String, icon: String, tint: Color, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(shape = RoundedCornerShape(18.dp), color = tint) {
                Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) { LineIcon(icon, null, Navy) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(description, fontSize = 17.sp, lineHeight = 25.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LineIcon("next", null, MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun AssistantCharacter(modifier: Modifier) {
    Canvas(modifier.semantics { contentDescription = "웃는 링고 도우미 캐릭터" }) {
        val w = size.width
        drawCircle(Yellow, radius = w * .44f, center = center)
        drawCircle(Navy, w * .035f, Offset(w * .36f, w * .43f))
        drawCircle(Navy, w * .035f, Offset(w * .64f, w * .43f))
        drawArc(Navy, 0f, 180f, false, Offset(w * .34f, w * .48f), androidx.compose.ui.geometry.Size(w * .32f, w * .22f), style = Stroke(w * .035f, cap = StrokeCap.Round))
    }
}

@Composable
private fun LineIcon(kind: String, description: String?, color: Color = Navy) {
    Canvas(Modifier.size(26.dp).semantics { if (description != null) contentDescription = description }) {
        val s = size.width
        fun line(x: Float, y: Float, x2: Float, y2: Float) = drawLine(color, Offset(s*x, s*y), Offset(s*x2, s*y2), s*.075f, StrokeCap.Round)
        when (kind) {
            "menu" -> { line(.15f,.25f,.85f,.25f); line(.15f,.5f,.85f,.5f); line(.15f,.75f,.85f,.75f) }
            "mic" -> {
                drawRoundRect(color, Offset(s*.38f,s*.1f), androidx.compose.ui.geometry.Size(s*.24f,s*.48f), androidx.compose.ui.geometry.CornerRadius(s*.12f))
                drawArc(color, 0f, 180f, false, Offset(s*.22f,s*.23f), androidx.compose.ui.geometry.Size(s*.56f,s*.5f), style = Stroke(s*.07f))
                line(.5f,.74f,.5f,.9f); line(.32f,.9f,.68f,.9f)
            }
            "learn" -> {
                drawRoundRect(color, Offset(s*.25f,s*.06f), androidx.compose.ui.geometry.Size(s*.5f,s*.88f), androidx.compose.ui.geometry.CornerRadius(s*.07f), style = Stroke(s*.07f))
                line(.44f,.8f,.56f,.8f)
            }
            "guide" -> { line(.25f,.15f,.25f,.9f); line(.25f,.2f,.8f,.2f); line(.8f,.2f,.65f,.4f); line(.65f,.4f,.25f,.4f); line(.25f,.6f,.8f,.6f); line(.8f,.6f,.65f,.8f); line(.65f,.8f,.25f,.8f) }
            "send" -> { line(.15f,.5f,.85f,.5f); line(.58f,.22f,.85f,.5f); line(.58f,.78f,.85f,.5f) }
            else -> { line(.38f,.22f,.66f,.5f); line(.66f,.5f,.38f,.78f) }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun HomePreview() { SilverRingoTheme { HomeScreen({}, {}) } }
