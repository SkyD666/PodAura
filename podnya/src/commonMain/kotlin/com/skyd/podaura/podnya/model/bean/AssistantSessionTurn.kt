package com.skyd.podaura.podnya.model.bean

import com.skyd.podaura.podnya.core.AssistantContent
import com.skyd.podaura.podnya.core.AssistantContext
import com.skyd.podaura.podnya.core.AssistantFailure
import com.skyd.podaura.podnya.core.AssistantPhase
import com.skyd.podaura.podnya.core.AssistantProgress

data class AssistantSessionTurn(
    val id: Long,
    val question: String,
    /** Only actual network deltas; checked business data remains in [content]. */
    val reply: String = "",
    val content: AssistantContent? = null,
    val context: AssistantContext = AssistantContext.Empty,
    val progress: AssistantProgress? = AssistantPhase.PREPARING,
    val status: AssistantTurnStatus = AssistantTurnStatus.RUNNING,
    val error: AssistantFailure? = null,
)
