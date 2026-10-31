package com.silverlingo.app.chat

import org.junit.Assert.*
import org.junit.Test
import java.io.*

class ChatActionsTest {
    private val kiosk = ChatAction("open_kiosk_course", "navigate", "learning.kiosk", "untrusted label")
    private val guide = ChatAction("open_guide_home", "navigate", "guides.home", "untrusted label")
    private val sources = listOf(ChatSource("course:kiosk:level:5", "learning", "prototype"),
        ChatSource("guide:ddocdoc", "assistant", "prototype"))

    @Test fun onlyKnownIdTypeTargetAndRelatedCurrentSourceCanNavigate() {
        val reply = ChatReply("안내", sources, listOf(kiosk, guide), "id")
        assertEquals(listOf("learning", "guide"), ChatActions.allowed(reply).map { ChatActions.destination(it) })
        for (action in listOf(kiosk.copy(id = "unknown"), kiosk.copy(type = "execute"),
            kiosk.copy(target = "https://example.invalid"), kiosk.copy(target = "guides.home"))) {
            assertNull(ChatActions.destination(action))
            assertTrue(ChatActions.allowed(reply.copy(actions = listOf(action))).isEmpty())
        }
        assertTrue(ChatActions.allowed(reply.copy(sources = emptyList())).isEmpty())
        assertTrue(ChatActions.allowed(reply.copy(sources = sources.map { it.copy(status = "placeholder") })).isEmpty())
        assertTrue(ChatActions.allowed(reply.copy(sources = listOf(sources[1]), actions = listOf(kiosk))).isEmpty())
    }

    @Test fun duplicatesAreRemovedAndOnlyLocalLabelsAreUsed() {
        val allowed = ChatActions.allowed(ChatReply("안내", sources, listOf(kiosk, kiosk, guide, guide), "id"))
        assertEquals(2, allowed.size)
        assertEquals("키오스크 배우기 열기", allowed[0].label)
        assertEquals("병원 접수 도우미 열기", allowed[1].label)
    }

    @Test fun savedConversationKeepsMetadataOnItsOriginalAnswer() {
        val conversation = arrayListOf(ChatMessage("user", "결제 연습"),
            ChatMessage("assistant", "학습 답변", ChatReply("학습 답변", sources, listOf(kiosk), "1")),
            ChatMessage("user", "와이파이"), ChatMessage("assistant", "일반 답변", ChatReply("일반 답변", emptyList(), emptyList(), "2")))
        val bytes = ByteArrayOutputStream()
        ObjectOutputStream(bytes).use { it.writeObject(conversation) }
        val restored = ObjectInputStream(ByteArrayInputStream(bytes.toByteArray())).use { it.readObject() as List<*> }
        assertEquals(listOf(kiosk), (restored[1] as ChatMessage).response!!.actions)
        assertTrue((restored[3] as ChatMessage).response!!.actions.isEmpty())
    }
}
