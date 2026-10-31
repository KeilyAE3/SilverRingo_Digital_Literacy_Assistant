package com.silverlingo.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.test.espresso.Espresso
import com.silverlingo.app.ui.theme.SilverLingoTheme
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.net.InetAddress
import java.net.ServerSocket
import java.util.Collections

class ChatNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun answerButtonsStayWithMessagesAndBothFeaturesReturnToChat() {
        val replies = listOf(
            """{"reply":"학습 답변","sources":[{"id":"course:kiosk:level:5","category":"learning","status":"prototype"}],"suggested_actions":[{"id":"open_kiosk_course","type":"navigate","target":"learning.kiosk","label":"ignored"}],"request_id":"1"}""",
            """{"reply":"일반 답변","sources":[],"suggested_actions":[{"id":"unknown","type":"navigate","target":"https://example.invalid","label":"위험 버튼"}],"request_id":"2"}""",
            """{"reply":"가이드 답변","sources":[{"id":"guide:ddocdoc","category":"assistant","status":"prototype"}],"suggested_actions":[{"id":"open_guide_home","type":"navigate","target":"guides.home","label":"ignored"}],"request_id":"3"}"""
        )
        ServerSocket(0, 3, InetAddress.getByName("127.0.0.1")).use { server ->
            server.soTimeout = 60_000
            val requests = Collections.synchronizedList(mutableListOf<JSONObject>())
            val worker = Thread {
                replies.forEach { reply ->
                    server.accept().use { socket ->
                        socket.soTimeout = 5_000
                        val input = socket.getInputStream()
                        val head = StringBuilder()
                        while (!head.endsWith("\r\n\r\n")) {
                            val next = input.read()
                            if (next == -1) return@Thread
                            head.append(next.toChar())
                        }
                        val length = Regex("Content-Length: (\\d+)", RegexOption.IGNORE_CASE).find(head)!!.groupValues[1].toInt()
                        val body = ByteArray(length)
                        for (i in body.indices) body[i] = input.read().toByte()
                        requests.add(JSONObject(String(body, Charsets.UTF_8)))
                        val bytes = reply.toByteArray(Charsets.UTF_8)
                        socket.getOutputStream().apply {
                            write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
                            write(bytes)
                        }
                    }
                }
            }.apply { isDaemon = true; start() }
            try {
                val restoration = StateRestorationTester(compose)
                restoration.setContent { SilverLingoTheme { SilverRingoApp(chatServerUrl = "http://127.0.0.1:${server.localPort}") } }
                compose.onNodeWithText("AI 도우미와 대화하기").performScrollTo().performClick()
                fun ask(question: String, answer: String) {
                    compose.onNodeWithText("질문 입력").performTextInput(question)
                    compose.onNodeWithText("보내기").performClick()
                    compose.waitUntil(10_000) { compose.onAllNodesWithText(answer).fetchSemanticsNodes().isNotEmpty() }
                }
                ask("키오스크 결제", "학습 답변")
                compose.onNodeWithText("키오스크 배우기 열기").performScrollTo().performClick()
                compose.onNodeWithText("키오스크 배우기").assertIsDisplayed()
                Espresso.pressBack()
                compose.onNodeWithText("학습 답변").assertExists()
                ask("와이파이 연결", "일반 답변")
                compose.onNodeWithText("위험 버튼").assertDoesNotExist()
                compose.onNode(hasText("키오스크 배우기 열기") and hasAnyAncestor(hasTestTag("chat-message-1")), useUnmergedTree = true).assertExists()
                compose.onNode(hasText("키오스크 배우기 열기") and hasAnyAncestor(hasTestTag("chat-message-3")), useUnmergedTree = true).assertDoesNotExist()
                restoration.emulateSavedInstanceStateRestore()
                compose.onNodeWithText("일반 답변").assertExists()
                ask("병원 접수", "가이드 답변")
                compose.onNodeWithText("병원 접수 도우미 열기").performScrollTo().performClick()
                compose.onNodeWithText("병원 안심 당일 접수").assertExists()
                compose.onNodeWithText("채팅으로 돌아가기").performClick()
                compose.onNodeWithText("가이드 답변").assertExists()
                compose.onNodeWithText("병원 접수 도우미 열기").performScrollTo().performClick()
                Espresso.pressBack()
                compose.onNodeWithText("가이드 답변").assertExists()
                assertEquals(3, requests.size)
                assertEquals(2, requests[1].getJSONArray("history").length())
                assertEquals(4, requests[2].getJSONArray("history").length())
            } finally {
                server.close()
                worker.join(1_000)
            }
        }
    }
}
