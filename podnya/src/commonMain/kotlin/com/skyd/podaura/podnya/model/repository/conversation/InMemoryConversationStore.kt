package com.skyd.podaura.podnya.model.repository.conversation

import com.skyd.podaura.podnya.model.bean.conversation.AssistantConversationSummary
import com.skyd.podaura.podnya.model.bean.conversation.AssistantSessionState
import com.skyd.podaura.podnya.model.bean.conversation.AssistantSessionTurn
import com.skyd.podaura.podnya.model.bean.conversation.AssistantTurnStatus
import com.skyd.podaura.podnya.model.bean.conversation.ConversationWindow
import com.skyd.podaura.podnya.model.bean.conversation.ConversationWorkspace
import com.skyd.podaura.podnya.model.bean.conversation.summary
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.uuid.Uuid

/** Test implementation of the same atomic contract; never used as an I/O failure fallback. */
class InMemoryConversationStore : ConversationStore {
    private val mutex = Mutex()
    private val records = mutableMapOf<Uuid, AssistantSessionState>()
    private var selection = ConversationWorkspace()

    override suspend fun open() = mutex.withLock {
        records.replaceAllValues { value ->
            value.copy(
                lastStatus = if (value.lastStatus == AssistantTurnStatus.RUNNING) AssistantTurnStatus.INTERRUPTED else value.lastStatus,
                turns = value.turns.map {
                    if (it.status == AssistantTurnStatus.RUNNING) {
                        it.copy(status = AssistantTurnStatus.INTERRUPTED, progress = null)
                    } else it
                },
            )
        }
    }

    override suspend fun workspace() = mutex.withLock { selection }
    override suspend fun metadata(id: Uuid) =
        mutex.withLock { records[id]?.copy(turns = emptyList()) }

    override suspend fun blank() =
        mutex.withLock { records.values.firstOrNull { !it.retained }?.copy(turns = emptyList()) }

    override suspend fun summaries(offset: Int, limit: Int) = mutex.withLock {
        records.values.asSequence().filter { it.retained }.map { it.summary() }
            .sortedWith(compareByDescending<AssistantConversationSummary> { it.activityAt }
                .thenByDescending { it.createdAt }.thenBy { it.id.toString() })
            .drop(offset).take(limit).toList()
    }

    override suspend fun tail(id: Uuid, limit: Int) = mutex.withLock {
        records[id]?.turns.orEmpty().takeLast(limit)
    }

    override suspend fun window(id: Uuid, anchor: Long?, limit: Int): ConversationWindow =
        mutex.withLock {
            val all = records[id]?.turns.orEmpty()
            val index =
                anchor?.let { target -> all.indexOfFirst { it.id >= target }.takeIf { it >= 0 } }
            val start = if (index == null) (all.size - limit).coerceAtLeast(0) else {
                (index - limit / 2).coerceAtLeast(0)
                    .coerceAtMost((all.size - limit).coerceAtLeast(0))
            }
            ConversationWindow(
                turns = all.drop(start).take(limit),
                hasBefore = start > 0,
                hasAfter = start + limit < all.size,
            )
        }

    override suspend fun commit(
        metadata: AssistantSessionState?,
        turn: AssistantSessionTurn?,
        workspace: ConversationWorkspace?,
    ) = mutex.withLock {
        require(turn == null || metadata != null)
        metadata?.let { value ->
            val old = records[value.id]?.turns.orEmpty()
            val turns =
                if (turn == null) old else (old.filter { it.id != turn.id } + turn).sortedBy { it.id }
            records[value.id] = value.copy(turns = turns)
        }
        workspace?.let { selection = it }
        Unit
    }

    override suspend fun delete(
        id: Uuid,
        workspace: ConversationWorkspace,
        replacement: AssistantSessionState?
    ) = mutex.withLock {
        records.remove(id)
        replacement?.let { records[it.id] = it.copy(turns = emptyList()) }
        selection = workspace
    }

    override suspend fun close() = Unit

    private fun <K, V> MutableMap<K, V>.replaceAllValues(transform: (V) -> V) {
        keys.toList().forEach { key -> this[key] = transform(getValue(key)) }
    }
}
