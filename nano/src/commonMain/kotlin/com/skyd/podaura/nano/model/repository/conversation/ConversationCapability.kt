package com.skyd.podaura.nano.model.repository.conversation

import com.skyd.podaura.nano.core.AssistantAction
import com.skyd.podaura.nano.core.AssistantContent
import com.skyd.podaura.nano.core.AssistantFailure
import com.skyd.podaura.nano.core.AssistantHandoff
import com.skyd.podaura.nano.core.AssistantRequestEvidence
import com.skyd.podaura.nano.model.bean.conversation.AssistantSessionTurn
import kotlinx.coroutines.CoroutineScope
import kotlin.uuid.Uuid

/** Capability callbacks share the repository's write lock, except for read-only request evidence. */
internal interface ConversationCapability {
    suspend fun evidence(
        conversation: ConversationHistory,
        history: List<AssistantSessionTurn>,
        currentTurnId: Long,
    ): AssistantRequestEvidence? = null

    suspend fun accept(handoff: AssistantHandoff, admission: ConversationAdmission): Boolean = false
    fun handles(action: AssistantAction): Boolean = false
    suspend fun perform(conversation: ConversationAccess, action: AssistantAction) = Unit

    suspend fun restore(
        conversation: ConversationAccess,
        turn: AssistantSessionTurn
    ): AssistantSessionTurn = turn

    /** The turn has been restored. Return true to handle its retry, or false to continue normal generation. */
    suspend fun retry(conversation: ConversationAccess, turn: AssistantSessionTurn): Boolean = false
}

internal interface ConversationHistory {
    suspend fun turn(turnId: Long, allowCached: Boolean = false): AssistantSessionTurn?
    suspend fun recentTurns(
        limit: Int,
        throughTurnId: Long = Long.MAX_VALUE
    ): List<AssistantSessionTurn>

    suspend fun recentCapabilityTurns(
        capabilityId: String,
        beforeTurnId: Long,
        limit: Int
    ): List<AssistantSessionTurn>

}

internal interface ConversationAccess : ConversationHistory {
    val id: Uuid

    /** Work launched here finishes before the repository closes its store. */
    val scope: CoroutineScope

    suspend fun updateTurns(
        turns: List<AssistantSessionTurn>,
        showOnSaveFailure: Boolean = false
    ): Boolean

    suspend fun refresh(turn: AssistantSessionTurn): AssistantSessionTurn
    fun reject(error: AssistantFailure)

    /** Application jobs use this to serialize updates after doing their external work. */
    suspend fun write(block: suspend () -> Unit)
}

internal interface ConversationAdmission {
    val conversation: ConversationAccess
    val turn: AssistantSessionTurn

    /** Save recoverable content before claiming this turn. A concurrent stop can still win during the write. */
    suspend fun checkpoint(content: AssistantContent): Boolean

    /** Complete the current turn and atomically persist edits to other turns in the same conversation. */
    suspend fun commit(
        content: AssistantContent,
        updatedTurns: List<AssistantSessionTurn> = emptyList(),
    ): Boolean
}
