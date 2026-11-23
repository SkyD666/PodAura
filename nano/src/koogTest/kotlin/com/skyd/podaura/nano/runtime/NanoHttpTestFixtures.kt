package com.skyd.podaura.nano.runtime

import com.skyd.podaura.nano.core.AssistantConnection
import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.sse.DefaultClientSSESession
import io.ktor.client.plugins.sse.SSECapability
import io.ktor.client.plugins.sse.SSEClientContent
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlin.test.assertEquals

internal const val TEST_KEY = "synthetic-test-secret"
internal fun testConnection() = AssistantConnection("https://example.test/gateway/v1/", "user-model", TEST_KEY)

internal data class HttpReply(val body: String, val sse: Boolean = false, val status: HttpStatusCode = HttpStatusCode.OK)

internal class HttpScript(vararg replies: HttpReply) {
    private val replies = ArrayDeque(replies.toList())
    val requests = mutableListOf<JsonObject>()
    val paths = mutableListOf<String>()
    val timeouts = mutableListOf<HttpTimeoutConfig?>()
    fun client() = HttpClient(SseMockEngine { request ->
            assertEquals("Bearer $TEST_KEY", request.headers[HttpHeaders.Authorization])
            paths += request.url.encodedPath
            timeouts += request.getCapabilityOrNull(HttpTimeoutCapability)
            requests += nanoJson.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject
            val next = replies.removeFirst()
            respond(next.body, next.status, headersOf(HttpHeaders.ContentType,
                if (next.sse) "text/event-stream" else "application/json"))
        }) { followRedirects = false }
}

/** MockEngine omits SSECapability; use Ktor's real SSE parser over its scripted byte channel. */
@OptIn(InternalAPI::class)
@Suppress("DEPRECATION")
internal class SseMockEngine(handler: MockRequestHandler) : MockEngine(MockEngineConfig().apply { addHandler(handler) }) {
    override val supportedCapabilities = super.supportedCapabilities + SSECapability
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        val response = super.execute(data)
        val content = data.body as? SSEClientContent ?: return response
        return HttpResponseData(response.statusCode, response.requestTime, response.headers, response.version,
            DefaultClientSSESession(content, response.body as ByteReadChannel), response.callContext)
    }
}

internal fun chatCompletion(text: String) = response(buildJsonObject { put("role", "assistant"); put("content", text) })

internal fun chatToolCalls(name: String, vararg calls: Pair<String, String>, reasoning: String? = null) = response(buildJsonObject {
    put("role", "assistant")
    if (reasoning != null) put("reasoning_content", reasoning)
    put("tool_calls", buildJsonArray {
        calls.forEach { (id, args) -> add(buildJsonObject {
            put("id", id); put("type", "function")
            put("function", buildJsonObject { put("name", name); put("arguments", args) })
        }) }
    })
}, finishReason = "tool_calls")

private fun response(message: JsonObject, finishReason: String = "stop") = HttpReply(buildJsonObject {
    put("id", "test"); put("object", "chat.completion"); put("created", 0); put("model", "user-model")
    put("choices", buildJsonArray { add(buildJsonObject {
        put("index", 0); put("finish_reason", finishReason); put("message", message)
    }) })
}.toString())

internal fun chatSse(finishReason: String? = "stop"): HttpReply {
    fun chunk(delta: JsonObject, finish: String? = null) = "data: " + buildJsonObject {
        put("id", "test"); put("object", "chat.completion.chunk"); put("created", 0); put("model", "user-model")
        put("choices", buildJsonArray { add(buildJsonObject {
            put("index", 0); put("delta", delta); if (finish != null) put("finish_reason", finish)
        }) })
    } + "\n\n"
    return HttpReply(
        chunk(buildJsonObject { put("reasoning_content", "do not display") }) +
            chunk(buildJsonObject { put("content", "Found ") }) +
            chunk(buildJsonObject { put("content", "one.") }) +
            (finishReason?.let { chunk(buildJsonObject {}, it) } ?: "") + "data: [DONE]\n\n",
        sse = true,
    )
}

internal const val ENCRYPTED = "opaque-encrypted-reasoning"
internal fun astra() = AssistantConnection("https://api.openai.com/v1/", "gpt-6-astra", TEST_KEY)
internal fun responsesReasoning() = buildJsonObject {
    put("type", "reasoning"); put("id", "rs-test"); put("summary", buildJsonArray {})
    put("encrypted_content", ENCRYPTED)
}
internal fun responsesCall(id: String, arguments: String = "{}", name: String = "nano_echo") = buildJsonObject {
    put("type", "function_call"); put("id", "fc-$id"); put("call_id", id)
    put("name", name); put("arguments", arguments); put("status", "completed")
}
internal fun responsesMessage(text: String, phase: String = "final_answer") = buildJsonObject {
    put("type", "message"); put("id", "msg-test"); put("role", "assistant")
    put("phase", phase); put("status", "completed")
    put("content", buildJsonArray { add(buildJsonObject {
        put("type", "output_text"); put("text", text); put("annotations", buildJsonArray {})
    }) })
}
internal fun responsesObject(vararg output: JsonObject, status: String = "completed") = buildJsonObject {
    put("id", "resp-test"); put("object", "response"); put("created_at", 0); put("model", "gpt-6-astra")
    put("parallel_tool_calls", false); put("status", status); put("output", JsonArray(output.toList()))
    put("text", buildJsonObject { put("format", buildJsonObject { put("type", "text") }) })
}
internal fun responsesReply(vararg output: JsonObject) = HttpReply(responsesObject(*output).toString())
internal fun responsesStream(
    terminal: Boolean = true, duplicate: Boolean = false, terminalType: String = "response.completed",
    status: String = "completed", finalText: String = "Found one.",
): HttpReply {
    fun event(type: String, sequence: Int, text: String? = null, complete: Boolean = false) =
        "event: $type\ndata: " + buildJsonObject {
            put("type", type); put("sequence_number", sequence)
            if (text != null) {
                put("item_id", "msg-test"); put("output_index", 1); put("content_index", 0); put("delta", text)
            }
            if (complete) put("response", responsesObject(responsesReasoning(), responsesMessage(finalText), status = status))
        } + "\n\n"
    val completion = event(terminalType, 3, complete = true)
    return HttpReply(
        event("response.reasoning_text.delta", 0, "hidden reasoning") +
            event("response.output_text.delta", 1, "Found ") + event("response.output_text.delta", 2, "one.") +
            (if (terminal) completion else "") + (if (duplicate) completion else ""),
        sse = true,
    )
}
