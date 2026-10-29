package kr.silverringo.learning.phone

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

data class TeachingCard(val title: String, val text: String, val picture: String)
fun teachingCards(lesson: PhoneLesson): List<TeachingCard> = when(lesson) {
    PhoneLesson.SETTINGS -> listOf(
        TeachingCard("휴대폰 옆면의 음량 버튼", "일반적인 갤럭시는 오른쪽 옆면의 긴 버튼으로 음량을 조절해요. 위쪽은 크게, 아래쪽은 작게 합니다. 그 아래 측면 버튼은 화면 켜기·끄기에 쓰므로 구분해 주세요. 이번 연습은 미디어 음량을 조절합니다.", "volume"),
        TeachingCard("화면 밝기는 빠른 설정이나 설정에서", "화면 위에서 아래로 쓸어내려 빠른 설정창을 엽니다. 이전 One UI는 한 번 더 내려 펼칩니다. 최근 One UI의 분리된 화면에서는 오른쪽 위에서 내려 보세요. 해 모양 밝기 막대를 움직입니다. 또는 설정 → 디스플레이에서 조절할 수 있어요.", "brightness"),
        TeachingCard("글자 크기는 내 눈에 편하게", "설정 → 디스플레이 → 글자 크기와 스타일에서 미리보기를 보며 크기를 고릅니다. 지금 크기가 편하면 바꿀 필요가 없어요. 자동 밝기가 켜져 있다면 밝기가 주변 환경에 따라 달라질 수 있습니다.", "font")
    )
    PhoneLesson.CALLS -> listOf(
        TeachingCard("전화 앱과 숫자판", "초록색 전화 앱을 열고 키패드(숫자판)를 선택해요. 번호를 입력하고 한 자리씩 확인합니다. 이번 지정 번호는 010-6344-2138입니다.", "keypad"),
        TeachingCard("걸기와 종료", "통화 버튼을 누르면 실제 전화가 연결될 수 있어요. 상대가 연습에 참여할 수 있을 때 진행합니다. 통화가 끝나면 빨간 종료 버튼을 누릅니다.", "call"),
        TeachingCard("걸려 온 전화 받기", "상대의 이름·번호를 확인하고 녹색 받기 아이콘을 화면이 안내하는 방향으로 밀거나 누릅니다. 설정에 따라 받는 방식은 달라요. 실제 수신 연습은 상대가 전화를 걸어 줘야 합니다.", "call")
    )
    PhoneLesson.CONTACTS -> listOf(
        TeachingCard("번호를 이름과 함께 저장", "연락처 앱에서 + 또는 새 연락처 추가를 누릅니다. 저장 위치를 묻는 경우 사용할 계정을 선택해요.", "contact"),
        TeachingCard("입력하고 저장하기", "이름에 연습 친구, 전화번호에 010-6344-2138을 입력해요. 번호를 확인한 뒤 저장을 누릅니다. 실제 앱 실습에서는 기기의 연락처에 저장됩니다.", "contact"),
        TeachingCard("돋보기로 다시 찾기", "연락처의 검색(돋보기)을 누르고 연습 친구를 입력합니다. 결과를 열어 번호가 맞는지 확인해요. 이미 같은 연락처가 있다면 중복 저장하지 말고 찾아보세요.", "search")
    )
    PhoneLesson.MESSAGES -> listOf(
        TeachingCard("받은 문자 읽기", "문자 앱에서 상대의 대화방을 열어 받은 내용을 읽습니다. 메시지 앱은 삼성 메시지 또는 Google 메시지일 수 있어요. 카카오톡과 문자(SMS)는 다른 앱입니다.", "message"),
        TeachingCard("받는 사람과 내용 확인", "새 대화 또는 작성 버튼을 누르고 010-6344-2138을 입력합니다. 내용에 안녕하세요를 써 보세요. 보내기 화살표를 누르면 실제 문자가 전송될 수 있어요.", "message"),
        TeachingCard("답장은 같은 대화방에서", "상대에게 답장을 요청한 뒤 같은 대화방을 열어 확인해요. 앱을 열었다는 사실만으로 전송·수신 성공을 알 수는 없어 실습 뒤 직접 확인합니다.", "message")
    )
}

/** Original schematic, not a screenshot or exact model replica. Text supplies accessible meaning. */
@Composable
internal fun PhoneDiagram(kind: String) {
    Canvas(Modifier.fillMaxWidth().height(230.dp).semantics { contentDescription = when(kind) {
        "volume" -> "휴대폰 오른쪽 위 옆면의 긴 음량 버튼과 그 아래 측면 버튼 위치 그림"
        "brightness" -> "빠른 설정창 아래에 있는 밝기 막대 그림"
        "font" -> "작은 글자와 큰 글자 미리보기 위치 그림"
        else -> "휴대폰 앱 화면의 개념 그림"
    } }) {
        val w=size.width; val h=size.height; val left=w*.27f;val width=w*.46f
        drawRoundRect(Color(0xFF211C50),Offset(left,4f),Size(width,h-8f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(22f))
        drawRoundRect(Color.White,Offset(left+10,18f),Size(width-20,h-36),cornerRadius=androidx.compose.ui.geometry.CornerRadius(14f))
        if(kind=="volume") {
            drawRoundRect(Color(0xFFFFD337),Offset(left+width+3,h*.17f),Size(12f,h*.25f))
            drawLine(Color(0xFF211C50),Offset(left+width+25,h*.30f),Offset(w*.94f,h*.30f),6f)
            drawRect(Color(0xFF59647A),Offset(left+width+3,h*.52f),Size(10f,h*.12f))
        } else if(kind=="brightness" || kind=="font") {
            for(i in 0..2) drawCircle(Color(0xFFD9DFEA),12f,Offset(left+width*(.25f+i*.25f),h*.28f))
            drawLine(Color(0xFF211C50),Offset(left+25,h*.64f),Offset(left+width-25,h*.64f),8f)
            drawCircle(Color(0xFFC38A00),14f,Offset(left+width*.65f,h*.64f))
        } else if(kind=="keypad") {
            for(y in 0..3) for(x in 0..2) drawCircle(Color(0xFFCDD7E5),11f,Offset(left+width*(.25f+x*.25f),h*(.3f+y*.14f)))
        } else if(kind=="call") {
            drawCircle(Color(0xFF13704A),24f,Offset(left+width*.32f,h*.72f));drawCircle(Color(0xFFAD2C37),24f,Offset(left+width*.7f,h*.72f))
        } else {
            for(i in 0..2) drawRoundRect(if(i%2==0) Color(0xFFDBE6F3) else Color(0xFFFFE797),Offset(left+20,h*(.25f+i*.18f)),Size(width-40,30f))
        }
    }
}
