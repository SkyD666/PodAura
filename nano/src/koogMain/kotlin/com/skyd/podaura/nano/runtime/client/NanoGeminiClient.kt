package com.skyd.podaura.nano.runtime.client

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.Prompt
import ai.koog.prompt.executor.clients.ConnectionTimeoutConfig
import ai.koog.prompt.executor.clients.google.GoogleClientSettings
import ai.koog.prompt.executor.clients.google.GoogleLLMClient
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import com.skyd.podaura.nano.core.AssistantConnection
import com.skyd.podaura.nano.core.AssistantError
import com.skyd.podaura.nano.core.AssistantException
import com.skyd.podaura.nano.core.AssistantLimits
import com.skyd.podaura.nano.runtime.NanoHttpFactory
import com.skyd.podaura.nano.runtime.nanoJson
import com.skyd.podaura.nano.runtime.normalizeNativeFrames
import io.ktor.http.encodeURLPathPart
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

internal fun nanoGeminiClient(
    connection: AssistantConnection,
    httpFactory: NanoHttpFactory,
    exchange: NanoGeminiExchange,
    timeoutConfig: ConnectionTimeoutConfig,
): GoogleLLMClient {
    return object : GoogleLLMClient(
        apiKey = connection.apiKey,
        settings = GoogleClientSettings(
            baseUrl = connection.baseUrl,
            defaultPath = "models",
            timeoutConfig = timeoutConfig,
        ),
        httpClientFactory = httpFactory,
    ) {
        override suspend fun execute(
            prompt: Prompt,
            model: LLModel,
            tools: List<ToolDescriptor>
        ): Message.Assistant = exchange.assistant(
            super.execute(
                prompt = prompt,
                model = model.copy(id = model.id.encodeURLPathPart()),
                tools = tools
            )
        )

        override fun executeStreaming(prompt: Prompt, model: LLModel, tools: List<ToolDescriptor>) =
            super.executeStreaming(
                prompt = prompt,
                model = model.copy(id = model.id.encodeURLPathPart()),
                tools = tools
            ).normalizeNativeFrames("STOP")
    }
}

/** The SDK regenerates function IDs and drops some part signatures; retain exact model content. */
internal class NanoGeminiExchange {
    private var pending: JsonObject? = null
    private val exchanges = mutableMapOf<String, JsonObject>()
    private var bytes = 0
    private val originalIds = mutableMapOf<String, String?>()

    fun response(raw: String): String {
        val response = nanoJson.parseToJsonElement(raw) as? JsonObject ?: invalid()
        val candidate = geminiCandidate(response = response, streaming = false)
        val content = candidate["content"] as? JsonObject ?: invalid()
        if (pending != null) invalid()
        pending = content
        return raw
    }

    fun assistant(response: Message.Assistant): Message.Assistant {
        val content = pending ?: invalid()
        pending = null
        val exact = content["parts"] as? JsonArray ?: invalid()
        val calls = exact.mapNotNull { (it as? JsonObject)?.get("functionCall") as? JsonObject }
        val projected = response.parts.filterIsInstance<MessagePart.Tool.Call>()
        if (calls.size != projected.size) invalid()
        val ids = calls.mapIndexed { index, call ->
            call["id"]?.jsonPrimitive?.content ?: projected[index].id ?: invalid()
        }
        if (ids.any { it.isBlank() } || ids.distinct().size != ids.size) invalid()
        if (ids.isNotEmpty()) {
            bytes += content.toString().encodeToByteArray().size
            if (bytes > AssistantLimits.PROMPT_BYTES || exchanges.size >= AssistantLimits.MODEL_CALLS) {
                throw AssistantException(AssistantError.BUDGET_EXHAUSTED)
            }
            if (ids.any { it in originalIds }) invalid()
            ids.forEachIndexed { index, id ->
                originalIds[id] = calls[index]["id"]?.jsonPrimitive?.content
            }
            exchanges[ids.first()] = content
        }
        var index = 0
        return response.copy(
            parts = response.parts.map { part ->
                if (part is MessagePart.Tool.Call) {
                    MessagePart.Tool.Call(id = ids[index++], tool = part.tool, args = part.args)
                } else {
                    part
                }
            },
            finishReason = if (calls.isEmpty()) "stop" else "tool_calls",
        )
    }

    fun request(raw: String): String {
        val request = nanoJson.parseToJsonElement(raw) as? JsonObject ?: invalid()
        val contents = request["contents"] as? JsonArray ?: invalid()
        val restored = contents.map { value ->
            val content = value as? JsonObject ?: invalid()
            val parts = content["parts"] as? JsonArray ?: invalid()
            if (content["role"]?.jsonPrimitive?.content != "model") {
                return@map JsonObject(content + ("parts" to JsonArray(parts.map partMap@{ value ->
                    val part = value as? JsonObject ?: invalid()
                    val result = part["functionResponse"] as? JsonObject ?: return@partMap part
                    val id = result["id"]?.jsonPrimitive?.content ?: invalid()
                    if (id !in originalIds) invalid()
                    if (originalIds[id] == null) {
                        JsonObject(part + ("functionResponse" to JsonObject(result - "id")))
                    } else {
                        part
                    }
                })))
            }
            val calls = parts.mapNotNull { (it as? JsonObject)?.get("functionCall") as? JsonObject }
            if (calls.isEmpty()) return@map content
            val id = calls.first()["id"]?.jsonPrimitive?.content ?: invalid()
            val exact = exchanges[id] ?: invalid()
            val originalCalls = (exact["parts"] as JsonArray).mapNotNull {
                (it as? JsonObject)?.get("functionCall") as? JsonObject
            }
            if (calls.size != originalCalls.size || calls.indices.any {
                    calls[it]["name"] != originalCalls[it]["name"] || calls[it]["args"] != originalCalls[it]["args"] ||
                            originalIds[calls[it]["id"]?.jsonPrimitive?.content] != originalCalls[it]["id"]?.jsonPrimitive?.content
                }
            ) {
                invalid()
            }
            exact
        }
        return JsonObject(request + ("contents" to JsonArray(restored))).toString()
    }

    private fun invalid(): Nothing = throw AssistantException(AssistantError.INVALID_OUTPUT)
}

internal class GeminiStreamGuard {
    private var finished = false

    fun accept(raw: String?) {
        if (raw.isNullOrBlank()) return
        if (finished) invalid()
        val response = nanoJson.parseToJsonElement(raw) as? JsonObject ?: invalid()
        val candidate = geminiCandidate(response = response, streaming = true)
        val content = candidate["content"] as? JsonObject
        (content?.get("parts") as? JsonArray)?.forEach {
            val part = it as? JsonObject ?: invalid()
            if (part["functionCall"] != null || part["text"] == null) invalid()
        }
        if (candidate["finishReason"]?.jsonPrimitive?.content == "STOP") finished = true
    }

    fun finish() {
        if (!finished) invalid()
    }

    private fun invalid(): Nothing = throw AssistantException(AssistantError.STREAM_INTERRUPTED)
}

private fun geminiCandidate(response: JsonObject, streaming: Boolean): JsonObject {
    fun invalid(): Nothing = throw AssistantException(
        if (streaming) AssistantError.STREAM_INTERRUPTED else AssistantError.INVALID_OUTPUT
    )
    if (response["error"] != null) invalid()
    val feedback = response["promptFeedback"] as? JsonObject
    if (feedback?.get("blockReason") != null) invalid()
    val candidates = response["candidates"] as? JsonArray ?: invalid()
    val candidate = candidates.singleOrNull() as? JsonObject ?: invalid()
    val reason = candidate["finishReason"]?.jsonPrimitive?.content
    if (reason != "STOP" && !(streaming && reason == null)) invalid()
    (candidate["safetyRatings"] as? JsonArray)?.forEach {
        if ((it as? JsonObject)?.get("blocked") == JsonPrimitive(true)) invalid()
    }
    return candidate
}
