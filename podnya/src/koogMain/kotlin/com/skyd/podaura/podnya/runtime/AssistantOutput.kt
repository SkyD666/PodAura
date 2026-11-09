package com.skyd.podaura.podnya.runtime

import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.structure.json.JsonStructure
import com.skyd.podaura.podnya.core.AssistantAnswerKind
import com.skyd.podaura.podnya.core.AssistantContent
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantException
import com.skyd.podaura.podnya.core.AssistantLimits
import com.skyd.podaura.podnya.model.bean.AssistantReplyContent
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

@Serializable
internal data class AssistantOutput(
    val kind: AssistantAnswerKind,
    val text: String,
    val outputs: Map<String, JsonObject> = emptyMap(),
)

internal class ValidatedAssistantReply(
    val kind: AssistantAnswerKind,
    val text: String,
    val results: Map<String, CapabilityResult>,
) {
    fun content(): AssistantContent {
        val parts = results.values.map { it.content }
        return parts.singleOrNull() ?: AssistantReplyContent(parts, kind, text)
    }
}

/** Combine the registered schemas; the runtime never needs to know their domain fields. */
internal fun outputStructure(capabilities: List<KoogCapability>): JsonStructure<AssistantOutput> {
    val definitions = buildMap {
        capabilities.forEach { capability ->
            capability.selectionSchema["\$defs"]?.jsonObject?.forEach { (name, schema) ->
                require(name !in this || this[name] == schema) { "Conflicting capability schema: $name" }
                put(name, schema)
            }
        }
    }
    val schema = buildJsonObject {
        put("type", "object")
        put("additionalProperties", false)
        put("\$defs", JsonObject(definitions))
        put("required", JsonArray(listOf("kind", "text", "outputs").map(::JsonPrimitive)))
        put("properties", buildJsonObject {
            put("kind", buildJsonObject {
                put("type", "string")
                put("enum", JsonArray(AssistantAnswerKind.entries.map { JsonPrimitive(it.name) }))
            })
            put(
                "text",
                buildJsonObject {
                    put("type", "string"); put(
                    "maxLength",
                    AssistantLimits.REPLY_CHARS
                )
                })
            put("outputs", buildJsonObject {
                put("type", "object"); put("additionalProperties", false)
                put("properties", JsonObject(capabilities.associate { capability ->
                    capability.id to JsonObject(capability.selectionSchema.filterKeys { it != "\$defs" && it != "\$id" })
                }))
            })
        })
    }
    return JsonStructure(
        id = "assistant_reply",
        schema = LLMParams.Schema.JSON.Standard("assistant_reply", schema),
        examples = emptyList(),
        serializer = AssistantOutput.serializer(),
        json = podnyaJson,
    )
}

internal suspend fun validateOutput(
    selection: AssistantOutput,
    capabilities: List<KoogCapability>,
): ValidatedAssistantReply {
    val explanation =
        selection.kind == AssistantAnswerKind.CLARIFICATION || selection.kind == AssistantAnswerKind.UNSUPPORTED
    val invalid = selection.text.length > AssistantLimits.REPLY_CHARS ||
            (explanation && (selection.text.isBlank() || selection.outputs.isNotEmpty())) ||
            (!explanation && selection.outputs.isEmpty()) ||
            selection.outputs.keys.any { id -> capabilities.none { it.id == id } }
    if (invalid) throw AssistantException(AssistantError.INVALID_OUTPUT)

    val results = selection.outputs.mapValues { (id, output) ->
        try {
            capabilities.first { it.id == id }.validate(selection.kind, output)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (expected: AssistantException) {
            throw expected
        } catch (_: Exception) {
            throw AssistantException(AssistantError.INVALID_OUTPUT)
        }
    }
    val count = results.values.sumOf { it.itemCount }
    val invalidCount =
        results.values.any { it.itemCount !in 0..AssistantLimits.RESULT_ITEMS } || count > AssistantLimits.RESULT_ITEMS ||
                (selection.kind == AssistantAnswerKind.RESULTS && count == 0) ||
                (selection.kind == AssistantAnswerKind.NO_MATCH && count != 0)
    if (invalidCount) {
        throw AssistantException(AssistantError.INVALID_OUTPUT)
    }
    return ValidatedAssistantReply(selection.kind, if (explanation) selection.text else "", results)
}
