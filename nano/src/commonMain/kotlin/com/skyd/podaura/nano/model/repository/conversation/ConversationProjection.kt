package com.skyd.podaura.nano.model.repository.conversation

import com.skyd.podaura.nano.model.bean.conversation.AssistantConversationSummary
import com.skyd.podaura.nano.model.bean.conversation.AssistantConversationsState
import com.skyd.podaura.nano.model.bean.conversation.AssistantSessionState
import com.skyd.podaura.nano.model.bean.conversation.AssistantSessionTurn
import com.skyd.podaura.nano.model.bean.conversation.AssistantTurnStatus
import com.skyd.podaura.nano.model.bean.conversation.ConversationStorageState
import com.skyd.podaura.nano.model.bean.conversation.summary
import kotlin.uuid.Uuid

/** Pure projections: no mutable cache, database access or task ownership. */
internal fun AssistantConversationsState.withConversation(
    metadata: AssistantSessionState,
    turn: AssistantSessionTurn?,
): AssistantConversationsState {
    val selectedSession = if (selected.id == metadata.id) {
        var turns = selected.turns
        if (turn != null) {
            turns = if (turns.any { it.id == turn.id }) {
                turns.map { if (it.id == turn.id) turn else it }
            } else if (!selected.hasAfter && (turns.size < 40 || selected.position.followLatest)) {
                (turns + turn).takeLast(40)
            } else turns
        }
        metadata.copy(
            turns = turns,
            hasBefore = selected.hasBefore || turns.firstOrNull()?.id?.let { it > 1 } == true,
            hasAfter = turns.lastOrNull()?.id?.let { it < metadata.lastTurnId } == true,
        )
    } else selected
    val summary = metadata.summary()
    val belongsToPage = summaryOffset == 0 || summaries.any { it.id == metadata.id }
    val updatedSummaries =
        (summaries.filter { it.id != metadata.id } + listOfNotNull(summary.takeIf { it.retained && belongsToPage }))
            .sortedWith(
                compareByDescending<AssistantConversationSummary> { it.activityAt }
                    .thenByDescending { it.createdAt }
                    .thenBy { it.id.toString() }
            )
            .take(50)
    return copy(selected = selectedSession, summaries = updatedSummaries)
}

internal fun AssistantConversationsState.withUpdatedTurns(
    id: Uuid,
    updates: List<AssistantSessionTurn>,
): AssistantConversationsState {
    if (selected.id != id) return this
    val byId = updates.associateBy { it.id }
    return copy(selected = selected.copy(turns = selected.turns.map { byId[it.id] ?: it }))
}

internal fun AssistantConversationsState.withPending(
    change: PendingConversationChange,
): AssistantConversationsState {
    val turn = change.turn
    return if (turn != null && turn.status != AssistantTurnStatus.RUNNING) {
        val visibleStatus = if (storage == ConversationStorageState.SAVE_FAILED) {
            AssistantTurnStatus.STOPPED
        } else AssistantTurnStatus.RUNNING
        withConversation(
            metadata = change.metadata.copy(lastStatus = visibleStatus),
            turn = turn.copy(status = visibleStatus),
        )
    } else {
        withConversation(metadata = change.metadata, turn = turn)
    }
}
