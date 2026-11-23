package com.skyd.podaura.nano.runtime

import ai.koog.prompt.streaming.StreamFrame
import com.skyd.podaura.nano.core.AssistantError
import com.skyd.podaura.nano.core.AssistantException
import com.skyd.podaura.nano.core.AssistantFailure
import com.skyd.podaura.nano.core.AssistantLimits
import com.skyd.podaura.nano.core.AssistantReasoningEffort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Per-transport state; only this turn's tool exchanges are retained, never chat history. */
internal class NanoResponses(private val reasoningEffort: AssistantReasoningEffort) {
    private data class Exchange(val output: JsonArray, val projected: List<JsonObject>)

    private val exchanges = mutableListOf<Exchange>()

    fun request(body: String): String {
        val request = nanoJson.parseToJsonElement(body).jsonObjectOrFail()
        val input = (request["input"] as? JsonArray)?.toMutableList() ?: invalidResponse()
        // Koog 1.3 drops message phase/id and splits content parts. Restore exact output items,
        // anchored by unique tool call IDs, so reasoning and commentary keep their original order.
        for (exchange in exchanges.asReversed()) {
            val anchor = exchange.projected.indexOfFirst { it.text("type") == "function_call" }
            val callId = exchange.projected[anchor].text("call_id")
            val found = input.indexOfFirst {
                it is JsonObject && it.text("type") == "function_call" && it.text("call_id") == callId
            }
            if (found < 0) continue // Selection/reply prompts intentionally contain fresh snapshots.
            val start = found - anchor
            if (start < 0 || start + exchange.projected.size > input.size ||
                exchange.projected.indices.any {
                    !matches(
                        exchange.projected[it],
                        input[start + it]
                    )
                }
            ) invalidResponse()
            repeat(exchange.projected.size) { input.removeAt(start) }
            input.addAll(start, exchange.output)
        }
        return JsonObject(request.toMutableMap().apply {
            put("input", JsonArray(input))
            put("store", JsonPrimitive(false))
            put("include", buildJsonArray { add(JsonPrimitive("reasoning.encrypted_content")) })
            put("reasoning", buildJsonObject { put("effort", reasoningEffort.id) })
            remove("temperature")
            remove("top_p")
            remove("top_logprobs")
            remove("logprobs")
            (request["tools"] as? JsonArray)?.let { tools ->
                put("tools", JsonArray(tools.map { tool ->
                    val function = tool.jsonObjectOrFail()
                    if (function.text("type") != "function") invalidResponse()
                    JsonObject(function + ("strict" to JsonPrimitive(false)))
                }))
            }
        }).toString()
    }

    fun response(body: String) {
        val response = nanoJson.parseToJsonElement(body).jsonObjectOrFail()
        val output = completedOutput(response, AssistantError.INVALID_OUTPUT)
        if (output.none { (it as? JsonObject)?.text("type") == "function_call" }) return
        if (exchanges.size >= AssistantLimits.MODEL_CALLS) throw AssistantException(AssistantError.BUDGET_EXHAUSTED)
        val projected = output.flatMap { item ->
            val value = item.jsonObjectOrFail()
            when (value.text("type")) {
                "reasoning", "function_call" -> listOf(value)
                "message" -> (value["content"] as? JsonArray)?.map { content ->
                    val part = content.jsonObjectOrFail()
                    val text = part.text("text") ?: part.text("refusal") ?: invalidResponse()
                    buildJsonObject { put("type", "message"); put("text", text) }
                } ?: invalidResponse()

                else -> invalidResponse()
            }
        }
        val knownIds = exchanges
            .flatMap { it.projected }
            .filter { it.text("type") == "function_call" }
            .map { it.text("call_id") }
        val newIds = projected
            .filter { it.text("type") == "function_call" }
            .map { it.text("call_id") }
        if (newIds.any { it.isNullOrBlank() || it in knownIds } || newIds.distinct().size != newIds.size) invalidResponse()
        exchanges += Exchange(output, projected)
    }

    private fun matches(expected: JsonObject, actual: JsonElement): Boolean {
        val item = actual as? JsonObject ?: return false
        if (item.text("type") != expected.text("type")) return false
        return when (expected.text("type")) {
            "reasoning" -> item.text("id") == expected.text("id")
            "function_call" -> listOf("call_id", "name", "arguments")
                .all { item[it] == expected[it] }

            "message" -> item.text("role") == "assistant" &&
                    ((item["content"] as? JsonArray)?.singleOrNull() as? JsonObject)?.text("text") == expected.text(
                "text"
            )

            else -> false
        }
    }
}

/** Responses terminates with response.completed, not Chat Completions' [DONE]. */
internal class ResponsesStreamGuard {
    private val text = StringBuilder()
    private var completed = false

    fun accept(raw: String?) {
        if (raw.isNullOrBlank()) return
        if (completed) interrupted()
        val event = runCatching { nanoJson.parseToJsonElement(raw) as? JsonObject }.getOrNull()
            ?: interrupted()
        when (event.text("type")) {
            "response.failed", "response.incomplete", "error" -> interrupted()
            "response.output_text.delta" -> {
                text.append(event.text("delta") ?: interrupted())
                if (text.length > AssistantLimits.REPLY_CHARS) interrupted()
            }

            "response.completed" -> {
                val response = event["response"] as? JsonObject ?: interrupted()
                val output = completedOutput(response, AssistantError.STREAM_INTERRUPTED)
                val finalText = buildString {
                    output.forEach { item ->
                        val value = item as? JsonObject ?: interrupted()
                        when (value.text("type")) {
                            "reasoning" -> Unit
                            "message" -> {
                                if (value.text("role") != "assistant") interrupted()
                                val content = value["content"] as? JsonArray ?: interrupted()
                                content.forEach {
                                    val part = it as? JsonObject ?: interrupted()
                                    if (part.text("type") != "output_text") interrupted()
                                    append(part.text("text") ?: interrupted())
                                }
                            }

                            else -> interrupted()
                        }
                    }
                }
                if (text.isBlank() || finalText != text.toString()) interrupted()
                completed = true
            }
        }
    }

    fun finish() {
        if (!completed) interrupted()
    }
}

/** Koog 1.3 omits TextComplete for normal Responses messages and supplies End(null). */
internal fun Flow<StreamFrame>.normalizeResponsesFrames(): Flow<StreamFrame> = flow {
    val text = StringBuilder()
    collect { frame ->
        when (frame) {
            is StreamFrame.TextDelta -> {
                text.append(frame.text)
                if (text.length > AssistantLimits.REPLY_CHARS) interrupted()
                emit(frame)
            }

            is StreamFrame.End -> {
                // The HTTP guard already matched these deltas to the successful terminal output.
                emit(StreamFrame.TextComplete(text.toString()))
                emit(StreamFrame.End("stop", frame.metaInfo))
            }

            is StreamFrame.TextComplete -> Unit
            else -> emit(frame)
        }
    }
}

private fun completedOutput(response: JsonObject, failure: AssistantFailure): JsonArray {
    if (response.text("status") != "completed" ||
        response["error"].let { it != null && it != JsonNull } ||
        response["incomplete_details"].let { it != null && it != JsonNull }
    ) throw AssistantException(failure)
    val output = response["output"] as? JsonArray ?: throw AssistantException(failure)
    if (output.isEmpty() || output.any {
            it !is JsonObject || it.text("status")
                .let { status -> status != null && status != "completed" }
        }
    ) throw AssistantException(failure)
    return output
}

private fun JsonObject.text(name: String) =
    (get(name) as? JsonPrimitive)?.takeIf { it.isString }?.content

private fun JsonElement.jsonObjectOrFail() = this as? JsonObject ?: invalidResponse()
private fun invalidResponse(): Nothing = throw AssistantException(AssistantError.INVALID_OUTPUT)
private fun interrupted(): Nothing = throw AssistantException(AssistantError.STREAM_INTERRUPTED)
