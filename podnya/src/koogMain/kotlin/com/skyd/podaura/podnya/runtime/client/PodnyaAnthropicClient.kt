package com.skyd.podaura.podnya.runtime.client

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.Prompt
import ai.koog.prompt.executor.clients.ConnectionTimeoutConfig
import ai.koog.prompt.executor.clients.anthropic.AnthropicClientSettings
import ai.koog.prompt.executor.clients.anthropic.AnthropicLLMClient
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import com.skyd.podaura.podnya.core.AssistantConnection
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantException
import com.skyd.podaura.podnya.core.AssistantLimits
import com.skyd.podaura.podnya.runtime.PodnyaHttpFactory
import com.skyd.podaura.podnya.runtime.normalizeNativeFrames
import com.skyd.podaura.podnya.runtime.podnyaJson
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

internal fun podnyaAnthropicClient(
    connection: AssistantConnection,
    httpFactory: PodnyaHttpFactory,
    model: LLModel,
    timeoutConfig: ConnectionTimeoutConfig,
): AnthropicLLMClient = object : AnthropicLLMClient(
    apiKey = connection.apiKey,
    settings = AnthropicClientSettings(
        modelVersionsMap = mapOf(model to connection.modelId),
        baseUrl = connection.baseUrl,
        messagesPath = "messages",
        timeoutConfig = timeoutConfig,
    ),
    httpClientFactory = httpFactory,
) {
    override suspend fun execute(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>
    ): Message.Assistant {
        val response = super.execute(prompt = prompt, model = model, tools = tools)
        return response.copy(finishReason = if (response.finishReason == "tool_use") "tool_calls" else "stop")
    }

    override fun executeStreaming(prompt: Prompt, model: LLModel, tools: List<ToolDescriptor>) =
        super.executeStreaming(prompt = prompt, model = model, tools = tools)
            .normalizeNativeFrames("end_turn")
}

/** Exact content blocks are scoped to this transport, including blocks the SDK cannot represent. */
internal class PodnyaAnthropicExchange {
    private val exchanges = mutableMapOf<String, JsonArray>()
    private var bytes = 0

    fun response(raw: String): String {
        val response = podnyaJson.parseToJsonElement(raw) as? JsonObject ?: invalid()
        val reason = response["stop_reason"]?.jsonPrimitive?.content
        if (reason !in setOf("end_turn", "tool_use") || response["error"] != null) invalid()
        val content = response["content"] as? JsonArray ?: invalid()
        val calls =
            content.filter { (it as? JsonObject)?.get("type")?.jsonPrimitive?.content == "tool_use" }
        if ((reason == "tool_use") != calls.isNotEmpty()) invalid()
        content.forEach {
            val type = (it as? JsonObject)?.get("type")?.jsonPrimitive?.content
            if (type !in setOf("text", "tool_use", "thinking", "redacted_thinking")) invalid()
        }
        if (calls.isNotEmpty()) {
            bytes += raw.encodeToByteArray().size
            if (bytes > AssistantLimits.PROMPT_BYTES || exchanges.size >= AssistantLimits.MODEL_CALLS) {
                throw AssistantException(AssistantError.BUDGET_EXHAUSTED)
            }
            val id = (calls.first() as JsonObject)["id"]?.jsonPrimitive?.content ?: invalid()
            if (id in exchanges) invalid()
            exchanges[id] = content
        }
        // Koog 1.3 has no redacted_thinking DTO. Keep it in the exact exchange, outside SDK messages.
        return JsonObject(response + ("content" to JsonArray(content.filter {
            (it as JsonObject)["type"]?.jsonPrimitive?.content != "redacted_thinking"
        }))).toString()
    }

    fun request(raw: String): String {
        val request = podnyaJson.parseToJsonElement(raw) as? JsonObject ?: invalid()
        val messages = request["messages"] as? JsonArray ?: invalid()
        return JsonObject(request + ("messages" to JsonArray(messages.map { item ->
            val message = item as? JsonObject ?: invalid()
            if (message["role"]?.jsonPrimitive?.content != "assistant") return@map message
            val content = message["content"] as? JsonArray ?: return@map message
            val ids = content.mapNotNull { part ->
                val block = part as? JsonObject ?: invalid()
                if (block["type"]?.jsonPrimitive?.content == "tool_use") block["id"]?.jsonPrimitive?.content else null
            }
            val exact = ids.firstOrNull()?.let(exchanges::get) ?: return@map message
            val exactIds = exact.mapNotNull {
                val block = it as JsonObject
                if (block["type"]?.jsonPrimitive?.content == "tool_use") block["id"]?.jsonPrimitive?.content else null
            }
            if (ids != exactIds) invalid()
            JsonObject(message + ("content" to exact))
        }))).toString()
    }

    private fun invalid(): Nothing = throw AssistantException(AssistantError.INVALID_OUTPUT)
}

internal class AnthropicStreamGuard {
    private var started = false
    private var stopped = false
    private var terminal = false
    private val blocks = mutableMapOf<Int, String>()
    private var nextIndex = 0

    fun accept(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return true
        if (stopped) invalid()
        val event = podnyaJson.parseToJsonElement(raw) as? JsonObject ?: invalid()
        when (event["type"]?.jsonPrimitive?.content) {
            "ping" -> Unit
            "message_start" -> {
                if (started) invalid()
                started = true
            }

            "content_block_start" -> {
                if (!started || terminal || blocks.isNotEmpty() || index(event) != nextIndex++) invalid()
                val block = event["content_block"] as? JsonObject ?: invalid()
                val type = block["type"]?.jsonPrimitive?.content ?: invalid()
                if (type !in setOf("text", "thinking", "redacted_thinking")) invalid()
                blocks[index(event)] = type
                // Redacted blocks have no visible content and Koog 1.3 has no DTO for them.
                return type != "redacted_thinking"
            }

            "content_block_delta" -> {
                if (!started || terminal) invalid()
                val type = blocks[index(event)] ?: invalid()
                val delta = event["delta"] as? JsonObject ?: invalid()
                val deltaType = delta["type"]?.jsonPrimitive?.content
                if (type == "text" && deltaType != "text_delta") invalid()
                if (type == "thinking" && deltaType !in setOf(
                        "thinking_delta",
                        "signature_delta"
                    )
                ) invalid()
                if (type == "redacted_thinking") invalid()
            }

            "content_block_stop" -> {
                val type = blocks.remove(index(event)) ?: invalid()
                return type != "redacted_thinking"
            }

            "message_delta" -> {
                val delta = event["delta"] as? JsonObject ?: invalid()
                if (!started || blocks.isNotEmpty()) invalid()
                val reason = delta["stop_reason"]?.jsonPrimitive?.content
                if (reason == null || reason == "null") return false // Usage-only update.
                if (terminal || reason != "end_turn") invalid()
                terminal = true
            }

            "message_stop" -> {
                if (!terminal) invalid()
                stopped = true
            }

            else -> invalid()
        }
        return true
    }

    fun finish() {
        if (!stopped) invalid()
    }

    private fun index(event: JsonObject): Int =
        event["index"]?.jsonPrimitive?.content?.toIntOrNull() ?: invalid()

    private fun invalid(): Nothing = throw AssistantException(AssistantError.STREAM_INTERRUPTED)
}
