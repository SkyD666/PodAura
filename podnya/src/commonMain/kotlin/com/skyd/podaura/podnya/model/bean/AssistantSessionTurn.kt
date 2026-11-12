package com.skyd.podaura.podnya.model.bean

import com.skyd.fundation.ext.currentTimeMillis
import com.skyd.podaura.podnya.core.AssistantContent
import com.skyd.podaura.podnya.core.AssistantContext
import com.skyd.podaura.podnya.core.AssistantFailure
import com.skyd.podaura.podnya.core.AssistantPhase
import com.skyd.podaura.podnya.core.AssistantProgress
import kotlin.time.Clock

data class AssistantSessionTurn(
    val id: Long,
    val question: String,
    val createdAt: Long = Clock.currentTimeMillis(),
    val source: AssistantReplySource? = null,
    /** Only actual network deltas; checked business data remains in [content]. */
    val reply: String = "",
    val content: AssistantContent? = null,
    val context: AssistantContext = AssistantContext.Empty,
    val progress: AssistantProgress? = AssistantPhase.PREPARING,
    val status: AssistantTurnStatus = AssistantTurnStatus.RUNNING,
    val error: AssistantFailure? = null,
) {
    val retainedBytes: Long = question.encodeToByteArray().size.toLong() +
            reply.encodeToByteArray().size + (content?.retainedBytes ?: 0) + context.retainedBytes +
            (source?.baseUrl?.encodeToByteArray()?.size ?: 0) +
            (source?.modelId?.encodeToByteArray()?.size ?: 0) + 256
}
