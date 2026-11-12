package com.skyd.podaura.podnya.model.repository

import com.skyd.podaura.podnya.model.bean.AssistantConversationSummary
import com.skyd.podaura.podnya.model.bean.AssistantSessionState
import com.skyd.podaura.podnya.model.bean.summary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.uuid.Uuid

class InMemoryConversationStore : ConversationStore {
    private val records = MutableStateFlow<Map<Uuid, AssistantSessionState>>(emptyMap())
    override fun observeSummaries() = records.map { records ->
        records.values
            .map { it.summary() }
            .sortedWith(
                compareByDescending<AssistantConversationSummary> { it.activityAt }
                    .thenByDescending { it.createdAt }
                    .thenBy { it.id.toString() }
            )
    }.distinctUntilChanged()

    override fun observe(id: Uuid) = records.map { it[id] }.distinctUntilChanged()
    override suspend fun get(id: Uuid) = records.value[id]
    override suspend fun save(conversation: AssistantSessionState) {
        records.update { it + (conversation.id to conversation) }
    }

    override suspend fun delete(id: Uuid) {
        records.update { it - id }
    }
}
