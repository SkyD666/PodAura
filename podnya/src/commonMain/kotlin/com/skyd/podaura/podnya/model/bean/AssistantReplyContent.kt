package com.skyd.podaura.podnya.model.bean

import com.skyd.podaura.podnya.core.AssistantAnswerKind
import com.skyd.podaura.podnya.core.AssistantContent
import com.skyd.podaura.podnya.core.AssistantHistorySnapshot
import kotlinx.serialization.json.Json

/** One reply can contain outputs from several capabilities, or plain clarification text. */
class AssistantReplyContent(
    val parts: List<AssistantContent>,
    val kind: AssistantAnswerKind,
    val text: String,
) : AssistantContent {
    override val retainedBytes =
        parts.sumOf { it.retainedBytes } + text.encodeToByteArray().size + 128

    override fun historySnapshot() = AssistantHistorySnapshot(
        "assistant",
        text.ifBlank { kind.name },
        Json.encodeToString(parts.mapNotNull { it.historySnapshot() })
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
