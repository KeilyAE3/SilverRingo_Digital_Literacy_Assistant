package com.silverlingo.app.chat

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket

class ChatClientTest {
    private val validReply = """{"reply":"카드 결제를 배워요","sources":[{"id":"course:kiosk:level:5","category":"learning","status":"implemented"}],"suggested_actions":[],"request_id":"test-1"}"""

    @Test fun postsActualContractAndKeepsOnlySixHistoryItems() {
        LocalServer(200, validReply).use { server ->
            val result = ChatClient(true).send(server.url, "카드 결제", List(8) { ChatTurn("user", "질문 $it") })
            assertEquals("카드 결제를 배워요", result.reply)
            assertEquals("course:kiosk:level:5", result.sources.single().id)
            assertTrue(result.actions.isEmpty())
            assertTrue(server.headers.startsWith("POST /v1/chat HTTP/1.1"))
            val received = JSONObject(server.received)
            assertEquals("카드 결제", received.getString("message"))
            assertEquals(6, received.getJSONArray("history").length())
            assertEquals("질문 2", received.getJSONArray("history").getJSONObject(0).getString("content"))
        }
    }

    @Test fun serverErrorsAndMalformedRepliesBecomeReadableFailures() {
        fun check(status: Int, body: String, expected: String) {
            LocalServer(status, body).use { server ->
                val error = assertThrows(ChatFailure::class.java) { ChatClient(true).send(server.url, "질문", emptyList()) }
                assertTrue(error.message.orEmpty().contains(expected))
            }
        }
        check(503, """{"error":"llm_unavailable","message":"잠시 후 다시 시도해 주세요."}""", "잠시 후")
        check(500, """{"error":"internal","message":"private stack trace"}""", "서버에서 답변하지 못했어요")
        check(200, "<html>not an API</html>", "서버 답변 형식")
        check(200, """{"reply":"missing sources and id"}""", "서버 답변 형식")
    }

    @Test fun timesOutAndHandlesAnUnavailableServer() {
        val server = LocalServer(200, validReply, delayMs = 300)
        server.use {
            val error = assertThrows(ChatFailure::class.java) { ChatClient(true, 50).send(server.url, "질문", emptyList()) }
            assertTrue(error.message.orEmpty().contains("응답 시간"))
        }
        val error = assertThrows(ChatFailure::class.java) { ChatClient(true).send(server.url, "질문", emptyList()) }
        assertTrue(error.message.orEmpty().contains("서버에 연결할 수 없어요"))
    }

    @Test fun rateLimitAndUnavailableStatusesHaveSafeNoticesEvenWithoutJson() {
        for (status in listOf(429, 503)) {
            LocalServer(status, "<html>private proxy diagnostics</html>").use { server ->
                val failure = assertThrows(ChatFailure::class.java) {
                    ChatClient(true).send(server.url, "질문", emptyList())
                }
                assertEquals(status, failure.statusCode)
                assertTrue(failure.message.orEmpty().contains("다시 시도"))
                assertFalse(failure.message.orEmpty().contains("private"))
            }
        }
    }

    @Test fun refusesCredentialsAndReleaseCleartextBeforeConnecting() {
        for (url in listOf("http://localhost:8000", "https://user:password@example.com", "file:///secret", "https://example.com?token=secret")) {
            assertThrows(ChatFailure::class.java) { ChatClient().send(url, "질문", emptyList()) }
        }
        assertThrows(ChatFailure::class.java) { ChatClient().send("https://example.com", "a".repeat(1001), emptyList()) }
    }

    // One local HTTP exchange exercises the real client without a test library or remote API.
    private class LocalServer(status: Int, body: String, delayMs: Long = 0) : Closeable {
        private val socket = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        val url = "http://127.0.0.1:${socket.localPort}"
        @Volatile var headers = ""
        @Volatile var received = ""
        private val worker = Thread {
            try {
                socket.accept().use { client ->
                    client.soTimeout = 2000
                    val input = client.getInputStream()
                    val head = StringBuilder()
                    while (!head.endsWith("\r\n\r\n")) {
                        val byte = input.read()
                        if (byte == -1) throw IOException("Incomplete headers")
                        head.append(byte.toChar())
                    }
                    headers = head.toString()
                    val length = Regex("Content-Length: (\\d+)", RegexOption.IGNORE_CASE).find(headers)!!.groupValues[1].toInt()
                    val output = ByteArrayOutputStream()
                    repeat(length) { output.write(input.read()) }
                    received = output.toString("UTF-8")
                    Thread.sleep(delayMs)
                    val bytes = body.toByteArray(Charsets.UTF_8)
                    client.getOutputStream().apply {
                        write("HTTP/1.1 $status Response\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
                        write(bytes)
                        flush()
                    }
                }
            } catch (_: IOException) { /* The timeout test intentionally closes its client. */ }
        }.apply { isDaemon = true; start() }
        override fun close() { socket.close(); worker.join(2500) }
    }
}
