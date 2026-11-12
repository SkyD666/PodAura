package com.skyd.podaura.podnya.model.repository

import com.skyd.podaura.podnya.model.bean.AssistantConversationSummary
import com.skyd.podaura.podnya.model.bean.AssistantSessionState
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

/** Data only. The repository is the sole writer; no runtime or jobs belong here.
 * Future durable saves must propagate failures, and encode full capability cards and contexts
 * separately from AssistantHistorySnapshot. List queries must not load every conversation.
 */
interface ConversationStore {
    fun observeSummaries(): Flow<List<AssistantConversationSummary>>
    fun observe(id: Uuid): Flow<AssistantSessionState?>
    suspend fun get(id: Uuid): AssistantSessionState?
    suspend fun save(conversation: AssistantSessionState)
    suspend fun delete(id: Uuid)
}
