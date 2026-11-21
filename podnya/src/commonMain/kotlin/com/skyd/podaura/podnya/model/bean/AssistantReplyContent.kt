package com.skyd.podaura.podnya.model.bean

import com.skyd.podaura.podnya.core.AssistantAnswerKind
import com.skyd.podaura.podnya.core.AssistantContent
import com.skyd.podaura.podnya.core.AssistantHistorySnapshot
import com.skyd.podaura.podnya.core.AssistantLimits
import kotlinx.serialization.json.Json

internal const val REFERENCE_KIND_PREFIX = "reference-kind:"

/** One reply can contain outputs from several capabilities, or plain clarification text. */
class AssistantReplyContent(
    val parts: List<AssistantContent>,
    val kind: AssistantAnswerKind,
    val text: String,
    /** Remember a verified reference without rendering its recommendation cards. */
    val reference: AssistantContent? = null,
) : AssistantContent {
    override val retainedBytes =
        parts.sumOf { it.retainedBytes } +
                (reference?.retainedBytes ?: 0) + text.encodeToByteArray().size + 128

    override fun historySnapshot() = snapshot(AssistantContent::historySnapshot)
    override fun promptHistorySnapshot() = snapshot(AssistantContent::promptHistorySnapshot)

    private fun snapshot(project: (AssistantContent) -> AssistantHistorySnapshot?) =
        AssistantHistorySnapshot(
            capabilityId = "assistant",
            summary = text.ifBlank { kind.name }.take(AssistantLimits.HISTORY_REPLY_CHARS),
            payload = Json.encodeToString(
                parts.mapNotNull(project) + listOfNotNull(
                    reference?.let(project)
                        ?.copy(summary = "$REFERENCE_KIND_PREFIX${kind.name}")
                )
            )
        )
}

internal fun AssistantHistorySnapshot?.forCapability(id: String): AssistantHistorySnapshot? = when {
    this == null -> null
    capabilityId == id -> this
    capabilityId == "assistant" -> runCatching {
        Json.decodeFromString<List<AssistantHistorySnapshot>>(payload)
            .firstOrNull { it.capabilityId == id }
    }.getOrNull()

    else -> null
}
