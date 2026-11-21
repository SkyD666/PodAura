package com.skyd.podaura.podnya.core

import kotlinx.serialization.Serializable

/** Capability-owned context must freeze mutable selections before it enters a conversation. */
interface AssistantContext {
    val retainedBytes: Int get() = 0
    fun snapshot(): AssistantContext

    data object Empty : AssistantContext {
        override fun snapshot() = this
    }
}

/** Checked, bounded business content. The chat host never decodes its domain fields. */
interface AssistantContent {
    val retainedBytes: Int
    fun historySnapshot(): AssistantHistorySnapshot?

    /** Keep earlier local evidence when displaying this reply. */
    val keepsPreviousLocalSnapshot: Boolean get() = false

    /** Compact history used when budgeting and building model requests. */
    fun promptHistorySnapshot(): AssistantHistorySnapshot? = historySnapshot()
}

/** In-memory capability history. Disk records must preserve full capability
 * cards/navigation/context with versioned payloads and readable fallback text. Unknown search
 * contexts must require explicit replacement; interrupted runs restore as stopped, never resend.
 */
@Serializable
data class AssistantHistorySnapshot(
    val capabilityId: String,
    val summary: String,
    val payload: String
)

@Serializable
data class AssistantHistoryTurn(
    val question: String,
    val reply: String,
    val content: AssistantHistorySnapshot? = null,
)

/** Capability-owned local facts supplied by the application for this turn. */
interface AssistantRequestEvidence

/** A validated capability result for application-owned work. */
interface AssistantHandoff

data class AssistantTurnRequest(
    val question: String,
    val context: AssistantContext = AssistantContext.Empty,
    val history: List<AssistantHistoryTurn> = emptyList(),
    /** Complete evidence from the most recent eligible turn, reserved for local capability checks. */
    val latestLocalSnapshot: AssistantHistorySnapshot? = null,
    val evidence: List<AssistantRequestEvidence> = emptyList(),
)

interface AssistantProgress

/** Feature-specific progress (for example candidate counts) belongs to the capability. */
enum class AssistantPhase : AssistantProgress { PREPARING, PROCESSING, STREAMING }

/**
 * Actions created by validated capability code, never decoded directly from model output.
 * The UI emits them after a user gesture. Navigation and confirmed changes are handled by the host.
 */
interface AssistantAction

interface AssistantFailure

enum class AssistantError : AssistantFailure {
    CONFIGURATION_REQUIRED, CONFIGURATION_CHANGED, CONFIGURATION_STORAGE, CONVERSATION_STORAGE, SEARCH_CONTEXT_REQUIRED, GENERATION_LIMIT, STALE_INPUT, DISCLOSURE_REQUIRED, INVALID_ADDRESS,
    CREDENTIAL_TOO_LONG, CREDENTIAL_STORAGE_UNAVAILABLE, AUTHENTICATION, ENDPOINT_OR_MODEL, RATE_LIMIT,
    QUOTA_EXCEEDED, REQUEST_LIMITED,
    NETWORK, TIMEOUT, TOOLS_UNSUPPORTED, INVALID_OUTPUT,
    BUDGET_EXHAUSTED, STREAM_INTERRUPTED, UNAVAILABLE,
}

class AssistantException(val error: AssistantFailure) : Exception(error.toString())

sealed interface AssistantRunEvent {
    data class Progress(val progress: AssistantProgress) : AssistantRunEvent
    data class ContentReady(val content: AssistantContent) : AssistantRunEvent
    data class Handoff(val handoff: AssistantHandoff) : AssistantRunEvent
    data class ReplyDelta(val text: String) : AssistantRunEvent
    data object Completed : AssistantRunEvent
    data class Failed(val error: AssistantFailure, val content: AssistantContent? = null) :
        AssistantRunEvent
}

data class AssistantConnectionResult(val success: Boolean, val error: AssistantFailure? = null)

object AssistantLimits {
    const val INPUT_CHARS = 2_000
    const val REPLY_CHARS = 8_000
    const val HISTORY_REPLY_CHARS = 800

    // Independent provider token budget, with room for non-ASCII text and structured outputs.
    // Tokens are not characters; provider truncation must still fail protocol validation.
    const val OUTPUT_TOKENS = 16_384
    const val HISTORY_TURNS = 6
    const val TOOL_CALLS = 3
    const val RESULT_ITEMS = 20
    const val MODEL_CALLS = 7
    const val TURN_TIMEOUT_MILLIS = 90_000L
    const val PROMPT_BYTES = 64 * 1024
    const val GENERATIONS = 3
    const val TITLE_CHARS = 100
    const val TURN_BYTES = 2 * 1024 * 1024

    // Leave room for the host's input, reply and turn bookkeeping.
    const val CONTENT_BYTES = TURN_BYTES - (INPUT_CHARS + REPLY_CHARS) * 4 - 128
}
