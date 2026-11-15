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
    suspend fun tail(id: Uuid, limit: Int): List<AssistantSessionTurn>

    /** Metadata, optional one turn, and selection are committed together or not at all. */
    suspend fun commit(
        metadata: AssistantSessionState? = null,
        turn: AssistantSessionTurn? = null,
        workspace: ConversationWorkspace? = null,
    )

    suspend fun delete(
        id: Uuid,
        workspace: ConversationWorkspace,
        replacement: AssistantSessionState? = null
    )

    suspend fun close()
}
