package com.skyd.podaura.podnya.model.repository.conversation

import com.skyd.podaura.podnya.model.bean.conversation.AssistantConversationSummary
import com.skyd.podaura.podnya.model.bean.conversation.AssistantSessionState
import com.skyd.podaura.podnya.model.bean.conversation.AssistantSessionTurn
import com.skyd.podaura.podnya.model.bean.conversation.ConversationWindow
import com.skyd.podaura.podnya.model.bean.conversation.ConversationWorkspace
import kotlin.uuid.Uuid

/** Data access only. Metadata never includes turns; every message read is explicitly bounded. */
interface ConversationStore {
    suspend fun open()
    suspend fun workspace(): ConversationWorkspace
    suspend fun metadata(id: Uuid): AssistantSessionState?
    suspend fun blank(): AssistantSessionState?
    suspend fun summaries(offset: Int, limit: Int): List<AssistantConversationSummary>
    suspend fun window(id: Uuid, anchor: Long?, limit: Int = 40): ConversationWindow
    suspend fun tail(
        id: Uuid,
        limit: Int,
        throughTurnId: Long = Long.MAX_VALUE
    ): List<AssistantSessionTurn>

    suspend fun turn(id: Uuid, turnId: Long): AssistantSessionTurn?
    /** Returns a page of recent turns for a top-level capability, in conversation order. */
    suspend fun recentCapabilityTurns(
        id: Uuid,
        capabilityId: String,
        beforeTurnId: Long = Long.MAX_VALUE,
        limit: Int,
    ): List<AssistantSessionTurn>

    /** Metadata, optional one turn, and selection are committed together or not at all. */
    suspend fun commit(
        metadata: AssistantSessionState? = null,
        turn: AssistantSessionTurn? = null,
        workspace: ConversationWorkspace? = null,
    )

    /** Persists related turns together, with optional conversation metadata. */
    suspend fun commitTurns(
        id: Uuid,
        turns: List<AssistantSessionTurn>,
        metadata: AssistantSessionState? = null,
    )

    suspend fun delete(
        id: Uuid,
        workspace: ConversationWorkspace,
        replacement: AssistantSessionState? = null
    )

    suspend fun close()
}
