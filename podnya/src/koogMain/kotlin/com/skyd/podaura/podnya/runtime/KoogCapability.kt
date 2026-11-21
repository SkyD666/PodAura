package com.skyd.podaura.podnya.runtime

import ai.koog.agents.core.tools.Tool
import ai.koog.prompt.message.MessagePart
import com.skyd.podaura.podnya.core.AssistantAnswerKind
import com.skyd.podaura.podnya.core.AssistantContent
import com.skyd.podaura.podnya.core.AssistantFailure
import com.skyd.podaura.podnya.core.AssistantHandoff
import com.skyd.podaura.podnya.core.AssistantHistorySnapshot
import com.skyd.podaura.podnya.core.AssistantTurnRequest
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

/**
 * One capability instance per turn. It owns its tools, evidence and output schema.
 * Tools may read data or prepare an action. Model calls and validate() must never commit a
 * user-facing change. Such changes belong to the conversation repository.
 */
internal interface KoogCapability {
    val id: String
    val tools: List<Tool<*, *>>
    val instructions: String
    val selectionInstructions: String
    val selectionSchema: JsonObject
    val failure: AssistantFailure?

    val lookupTool: ReplyLookupTool? get() = null
    suspend fun prepare() = Unit

    /** Check raw arguments before Koog decodes them. The runtime owns the shared call budget. */
    fun admit(call: MessagePart.Tool.Call): MessagePart.Tool.Result?
    fun snapshot(textLimit: Int): JsonElement
    fun toolSnapshot(textLimit: Int, lookupTitleLimit: Int): JsonElement = snapshot(textLimit)
    fun history(snapshot: AssistantHistorySnapshot, textLimit: Int): JsonElement?

    /** Check model output against this turn's evidence; produce trusted UI data and reply facts. */
    suspend fun validate(kind: AssistantAnswerKind, output: JsonObject): CapabilityResult
}

/** A standalone lookup can publish its locally checked result immediately. */
internal interface ReplyLookupTool {
    val name: String
    val instructions: String
    val reply: ValidatedAssistantReply?
    suspend fun prepare() = Unit
}

internal class CapabilityResult private constructor(
    val content: AssistantContent?,
    val replyData: JsonElement,
    val itemCount: Int,
    val handoff: AssistantHandoff?,
) {
    constructor(content: AssistantContent, replyData: JsonElement, itemCount: Int) :
            this(content = content, replyData = replyData, itemCount = itemCount, handoff = null)

    constructor(handoff: AssistantHandoff) :
            this(content = null, replyData = JsonNull, itemCount = 1, handoff = handoff)
}

internal fun interface KoogCapabilityFactory {
    fun create(request: AssistantTurnRequest): KoogCapability?
}

/** Plans can consume evidence from any of this turn's installed tools. */
internal fun interface KoogPlanFactory {
    fun create(request: AssistantTurnRequest, sources: List<KoogCapability>): KoogCapability?
}

internal class KoogTurnCapabilities(
    private val capabilities: List<KoogCapabilityFactory>,
    private val plans: List<KoogPlanFactory>,
) {
    fun create(request: AssistantTurnRequest): List<KoogCapability> {
        val sources = capabilities.mapNotNull { it.create(request) }
        return sources + plans.mapNotNull { it.create(request, sources) }
    }
}
