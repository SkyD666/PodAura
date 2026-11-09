package com.skyd.podaura.podnya.runtime

import ai.koog.agents.core.tools.Tool
import ai.koog.prompt.message.MessagePart
import com.skyd.podaura.podnya.core.AssistantAnswerKind
import com.skyd.podaura.podnya.core.AssistantContent
import com.skyd.podaura.podnya.core.AssistantFailure
import com.skyd.podaura.podnya.core.AssistantHistorySnapshot
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * One capability instance per turn. It owns its tools, evidence and output schema.
 * Tools may read data or prepare an action. Model calls and validate() must never commit a
 * user-facing change. Such changes belong to application actions confirmed by the user.
 */
internal interface KoogCapability {
    val id: String
    val tools: List<Tool<*, *>>
    val instructions: String
    val selectionInstructions: String
    val selectionSchema: JsonObject
    val failure: AssistantFailure?

    /** Check raw arguments before Koog decodes them. The runtime owns the shared call budget. */
    fun admit(call: MessagePart.Tool.Call): MessagePart.Tool.Result?
    fun snapshot(textLimit: Int): JsonElement
    fun history(snapshot: AssistantHistorySnapshot, textLimit: Int): JsonElement?

    /** Check model output against this turn's evidence; produce trusted UI data and reply facts. */
    suspend fun validate(kind: AssistantAnswerKind, output: JsonObject): CapabilityResult
}

internal class CapabilityResult(
    val content: AssistantContent,
    val replyData: JsonElement,
    val itemCount: Int,
)
