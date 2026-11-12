package com.skyd.podaura.podnya.model.bean

import com.skyd.fundation.ext.currentTimeMillis
import com.skyd.podaura.podnya.core.AssistantContext
import kotlin.time.Clock
import kotlin.uuid.Uuid

data class AssistantSessionState(
    val id: Uuid = Uuid.random(),
    val title: String = "",
    val manuallyNamed: Boolean = false,
    val createdAt: Long = Clock.currentTimeMillis(),
    val activityAt: Long = createdAt,
    val draft: String = "",
    val editorId: String = "",
    val draftRevision: Long = 0,
    val turns: List<AssistantSessionTurn> = emptyList(),
    val context: AssistantContext = AssistantContext.Empty,
    val modelConfigId: String? = null,
    val destinations: Set<String> = emptySet(),
) {
    val isRunning: Boolean get() = turns.lastOrNull()?.status == AssistantTurnStatus.RUNNING
    val retained: Boolean get() = draft.isNotEmpty() || turns.isNotEmpty()

    // Content estimate, not heap usage. Capabilities own the size of their full cards/context.
    val retainedBytes: Long = 256L + title.encodeToByteArray().size +
            draft.encodeToByteArray().size + context.retainedBytes +
            destinations.sumOf { it.encodeToByteArray().size.toLong() } +
            turns.sumOf { it.retainedBytes }
}
