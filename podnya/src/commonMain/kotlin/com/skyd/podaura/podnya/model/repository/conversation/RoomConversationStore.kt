package com.skyd.podaura.podnya.model.repository.conversation

import com.skyd.podaura.podnya.model.bean.conversation.AssistantConversationSummary
import com.skyd.podaura.podnya.model.bean.conversation.AssistantSessionState
import com.skyd.podaura.podnya.model.bean.conversation.AssistantSessionTurn
import com.skyd.podaura.podnya.model.bean.conversation.AssistantTurnStatus
import com.skyd.podaura.podnya.model.bean.conversation.ConversationStorageException
import com.skyd.podaura.podnya.model.bean.conversation.ConversationStorageState
import com.skyd.podaura.podnya.model.bean.conversation.ConversationWindow
import com.skyd.podaura.podnya.model.bean.conversation.ConversationWorkspace
import com.skyd.podaura.podnya.model.db.ConversationDatabase
import com.skyd.podaura.podnya.model.db.ConversationDatabaseLock
import com.skyd.podaura.podnya.model.db.ConversationRecordCodec
import com.skyd.podaura.podnya.model.db.ConversationWorkspaceRecord
import com.skyd.podaura.podnya.model.db.builder
import com.skyd.podaura.podnya.model.db.conversationDatabasePath
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlin.uuid.Uuid

/** Owns the database handle and OS lock, with no second mutable conversation cache. */
class RoomConversationStore(
    private val codec: ConversationRecordCodec,
    private val path: () -> String = ::conversationDatabasePath,
    private val build: (String) -> ConversationDatabase = {
        ConversationDatabase.builder(it).build()
    },
) : ConversationStore {
    private var database: ConversationDatabase? = null
    private var lock: ConversationDatabaseLock? = null
    private val dao get() = checkNotNull(database).conversationDao()

    override suspend fun open() = withContext(Dispatchers.IO) {
        if (database != null) return@withContext
        val location = path()
        val acquired = ConversationDatabaseLock(location)
        acquired.acquire()
        var opened: ConversationDatabase? = null
        try {
            opened = build(location)
            // Forces opening/migration while exclusively owned, before recovering old runs.
            opened.conversationDao().recover()
            database = opened
            lock = acquired
        } catch (failure: Throwable) {
            opened?.close()
            acquired.release()
            if (failure is CancellationException) throw failure
            // Room reports an unsupported downgrade before any destructive action. Never fall back.
            val newer = generateSequence(failure) { it.cause }.any {
                it.message?.contains("migration from", ignoreCase = true) == true &&
                        it.message?.contains("to 1") == true ||
                        it.message?.contains("downgrade", ignoreCase = true) == true
            }
            throw ConversationStorageException(
                if (newer) ConversationStorageState.NEWER_VERSION else ConversationStorageState.OPEN_FAILED
            )
        }
    }

    override suspend fun workspace(): ConversationWorkspace {
        val value = dao.workspace() ?: return ConversationWorkspace()
        return ConversationWorkspace(
            selectedId = value.selectedConversationId?.let { runCatching { Uuid.parse(it) }.getOrNull() },
            lastModelId = value.lastModelConfigId,
        )
    }

    override suspend fun metadata(id: Uuid) = dao.metadata(id.toString())?.let(codec::decode)
    override suspend fun blank() = dao.blank()?.let(codec::decode)
    override suspend fun summaries(offset: Int, limit: Int) =
        dao.summaries(offset = offset, limit = limit).mapNotNull {
            val id =
                runCatching { Uuid.parse(it.conversationId) }.getOrNull() ?: return@mapNotNull null
            AssistantConversationSummary(
                id = id,
                title = it.title,
                createdAt = it.createdAt,
                activityAt = it.activityAt,
                status = it.lastStatus?.let { raw ->
                    runCatching { AssistantTurnStatus.valueOf(raw) }
                        .getOrDefault(AssistantTurnStatus.INTERRUPTED)
                },
                retained = it.retained,
            )
        }

    override suspend fun tail(id: Uuid, limit: Int) =
        dao.before(id = id.toString(), anchor = Long.MAX_VALUE, limit = limit)
            .asReversed()
            .map(codec::decode)

    override suspend fun window(id: Uuid, anchor: Long?, limit: Int): ConversationWindow {
        require(limit in 2..100)
        val before = dao.before(
            id = id.toString(),
            anchor = anchor ?: Long.MAX_VALUE,
            limit = if (anchor == null) limit + 1 else limit / 2 + 1
        )
        val left = before.take(if (anchor == null) limit else limit / 2).asReversed()
        val after = if (anchor == null) emptyList() else dao.after(
            id = id.toString(),
            anchor = anchor,
            limit = limit - left.size + 1
        )
        val right = after.take(limit - left.size)
        return ConversationWindow(
            turns = (left + right).map(codec::decode),
            hasBefore = before.size > left.size,
            hasAfter = after.size > right.size,
        )
    }

    override suspend fun commit(
        metadata: AssistantSessionState?,
        turn: AssistantSessionTurn?,
        workspace: ConversationWorkspace?,
    ) = withContext(Dispatchers.IO) {
        require(turn == null || metadata != null)
        dao.commit(
            metadata = metadata?.let(codec::encode),
            turn = turn?.let { codec.encode(id = metadata!!.id, value = it) },
            workspace = workspace?.record(),
        )
    }

    override suspend fun delete(
        id: Uuid,
        workspace: ConversationWorkspace,
        replacement: AssistantSessionState?
    ) = dao.delete(
        id = id.toString(),
        workspace = workspace.record(),
        replacement = replacement?.let(codec::encode)
    )

    override suspend fun close() = withContext(Dispatchers.IO) {
        try {
            database?.close()
        } finally {
            database = null
            lock?.release()
            lock = null
        }
        Unit
    }

    private fun ConversationWorkspace.record() = ConversationWorkspaceRecord(
        selectedConversationId = selectedId?.toString(),
        lastModelConfigId = lastModelId,
    )
}
