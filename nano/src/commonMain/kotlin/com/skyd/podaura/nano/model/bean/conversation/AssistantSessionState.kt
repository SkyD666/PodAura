package com.skyd.podaura.nano.model.bean.conversation

import com.skyd.fundation.ext.currentTimeMillis
import com.skyd.podaura.nano.core.AssistantContext
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
    val selectionStart: Int = draft.length,
    val selectionEnd: Int = selectionStart,
    val position: ConversationPosition = ConversationPosition(),
    val lastTurnId: Long = turns.lastOrNull()?.id ?: 0,
    val lastStatus: AssistantTurnStatus? = turns.lastOrNull()?.status,
    val hasBefore: Boolean = false,
    val hasAfter: Boolean = false,
    /** Preserve a damaged field until the user explicitly replaces it. */
    val rawDestinations: String? = null,
    val rawPosition: String? = null,
    val destinations: Set<String> = emptySet(),
) {
    val isRunning: Boolean get() = lastStatus == AssistantTurnStatus.RUNNING
    val retained: Boolean get() = draft.isNotEmpty() || lastTurnId > 0 || turns.isNotEmpty()

}
