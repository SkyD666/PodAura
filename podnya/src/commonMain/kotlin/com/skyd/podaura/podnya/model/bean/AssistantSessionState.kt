package com.skyd.podaura.podnya.model.bean

import com.skyd.podaura.podnya.core.AssistantContext

data class AssistantSessionState(
    val draft: String = "",
    val turns: List<AssistantSessionTurn> = emptyList(),
    val context: AssistantContext = AssistantContext.Empty,
    val historyTrimmed: Boolean = false,
) {
    val isRunning: Boolean get() = turns.lastOrNull()?.status == AssistantTurnStatus.RUNNING
}
