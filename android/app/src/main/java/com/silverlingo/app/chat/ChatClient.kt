package com.silverlingo.app.chat

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URL

data class ChatTurn(val role: String, val content: String)
data class ChatSource(val id: String, val category: String, val status: String)
data class ChatAction(val id: String, val type: String, val target: String, val label: String)
data class ChatReply(val reply: String, val sources: List<ChatSource>, val actions: List<ChatAction>, val requestId: String)
class ChatFailure(message: String) : IOException(message)

/** Calls the existing D API; keys and LLM execution belong to the server. */
class ChatClient(private val allowHttp: Boolean = false, private val readTimeoutMs: Int = 50_000) {
    fun send(baseUrl: String, message: String, history: List<ChatTurn>): ChatReply {
        if (message.isBlank() || message.length > 1000) throw ChatFailure("질문은 1~1,000자로 적어 주세요.")
        val target = endpoint(baseUrl)
        val turns = history.takeLast(6)
        if (turns.any { it.role !in setOf("user", "assistant") || it.content.isBlank() || it.content.length > 1000 }) {
            throw ChatFailure("대화 내용을 확인해 주세요.")
        }
        val request = JSONObject().put("message", message).put("history", JSONArray().apply {
            turns.forEach { put(JSONObject().put("role", it.role).put("content", it.content)) }
        }).toString().toByteArray(Charsets.UTF_8)
        var connection: HttpURLConnection? = null
        try {
            connection = target.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "POST"
                connectTimeout = 5_000
                readTimeout = readTimeoutMs
                instanceFollowRedirects = false
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setFixedLengthStreamingMode(request.size)
            }
            connection.outputStream.use { it.write(request) }
            val status = connection.responseCode
            val stream = if (status == 200) connection.inputStream else connection.errorStream
            val bytes = stream?.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (output.size() <= 65_536) {
                    val count = input.read(buffer, 0, minOf(buffer.size, 65_537 - output.size()))
                    if (count == -1) break
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            } ?: ByteArray(0)
            if (bytes.size > 65_536) throw ChatFailure("서버 답변이 너무 커요. 다시 시도해 주세요.")
            val json = try { JSONObject(String(bytes, Charsets.UTF_8)) } catch (_: Exception) {
                throw ChatFailure("서버 답변 형식을 확인할 수 없어요. 서버 주소를 확인해 주세요.")
            }
            if (status != 200) {
                // Display only documented public errors, never proxy/SDK diagnostics.
                val known = setOf("invalid_request", "request_too_large", "rate_limited", "llm_unavailable", "knowledge_unavailable")
                val text = json.optString("message")
                throw ChatFailure(if (json.optString("error") in known && text.isNotBlank() && text.length <= 1000) text
                    else "서버에서 답변하지 못했어요. 잠시 후 다시 시도해 주세요.")
            }
            return parseReply(json)
        } catch (error: ChatFailure) { throw error
        } catch (_: SocketTimeoutException) { throw ChatFailure("응답 시간이 길어졌어요. 잠시 후 다시 시도해 주세요.")
        } catch (_: IOException) { throw ChatFailure("서버에 연결할 수 없어요. 인터넷과 서버 주소를 확인해 주세요.")
        } catch (_: Exception) { throw ChatFailure("서버 답변 형식을 확인할 수 없어요.")
        } finally { connection?.disconnect() }
    }

    internal fun endpoint(baseUrl: String): URL {
        val base = try { URI(baseUrl.trim()) } catch (_: Exception) { throw ChatFailure("서버 주소를 확인해 주세요.") }
        if (base.host.isNullOrBlank() || base.rawUserInfo != null || base.rawQuery != null || base.rawFragment != null ||
            (base.scheme != "https" && !(allowHttp && base.scheme == "http"))) {
            throw ChatFailure("서버 주소를 확인해 주세요. 정식 앱은 HTTPS 주소가 필요해요.")
        }
        return URI(base.toString().trimEnd('/') + "/v1/chat").toURL()
    }

    private fun parseReply(json: JSONObject): ChatReply {
        val reply = json.getString("reply")
        val requestId = json.getString("request_id")
        require(reply.isNotBlank() && reply.length <= 10_000 && requestId.isNotBlank())
        val sources = json.getJSONArray("sources")
        val actions = json.getJSONArray("suggested_actions")
        require(sources.length() <= 100 && actions.length() <= 100)
        return ChatReply(reply, List(sources.length()) { index ->
            val source = sources.getJSONObject(index)
            ChatSource(source.getString("id"), source.getString("category"), source.getString("status"))
        }, List(actions.length()) { index ->
            val action = actions.getJSONObject(index)
            ChatAction(action.getString("id"), action.getString("type"), action.getString("target"), action.getString("label"))
        }, requestId)
    }
}
