package com.skyd.podaura.nano.model.bean.conversation

import com.skyd.fundation.ext.currentTimeMillis
import com.skyd.podaura.nano.core.AssistantContent
import com.skyd.podaura.nano.core.AssistantContext
import com.skyd.podaura.nano.core.AssistantFailure
import com.skyd.podaura.nano.core.AssistantPhase
import com.skyd.podaura.nano.core.AssistantProgress
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
    val rawSource: String? = null,
    val rawError: String? = null,
    val error: AssistantFailure? = null,
)
