package com.skyd.podaura.podnya.model.repository.conversation

import com.skyd.podaura.podnya.core.AssistantContext
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantException
import com.skyd.podaura.podnya.core.AssistantFailure
import com.skyd.podaura.podnya.core.AssistantLimits
import com.skyd.podaura.podnya.core.AssistantRunEvent
import com.skyd.podaura.podnya.core.AssistantRuntime
import com.skyd.podaura.podnya.core.UnreadableContext
import com.skyd.podaura.podnya.model.bean.conversation.AssistantConversationsState
import com.skyd.podaura.podnya.model.bean.conversation.AssistantReplySource
import com.skyd.podaura.podnya.model.bean.conversation.AssistantSessionState
import com.skyd.podaura.podnya.model.bean.conversation.AssistantSessionTurn
import com.skyd.podaura.podnya.model.bean.conversation.AssistantTurnStatus
import com.skyd.podaura.podnya.model.bean.conversation.ConversationPosition
import com.skyd.podaura.podnya.model.bean.conversation.ConversationStorageException
import com.skyd.podaura.podnya.model.bean.conversation.ConversationStorageState
import com.skyd.podaura.podnya.model.bean.conversation.ConversationWorkspace
import com.skyd.podaura.podnya.settings.AssistantSettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlin.uuid.Uuid

/** Application-owned tasks and uncommitted deltas. Disk I/O never holds [stateMutex]. */
class AssistantConversationRepository(
    private val assistant: AssistantRuntime,
    private val settings: AssistantSettingsRepository,
    private val store: ConversationStore = InMemoryConversationStore(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    saveClock: () -> Long = run {
        val origin = TimeSource.Monotonic.markNow()
        return@run { origin.elapsedNow().inWholeMilliseconds }
    },
) {
    private val mutableState = MutableStateFlow(AssistantConversationsState())
    val state = mutableState.asStateFlow()
    val available: Boolean get() = assistant.available
    private val notifications = MutableSharedFlow<Notice>(extraBufferCapacity = 64)
    val notices = notifications.asSharedFlow()

    // Only disk commands take this lock. Input and streaming deltas can arrive during a write.
    private val writes = Mutex()
    private val stateMutex = Mutex()
    private var initialized = false
    private var workspace = ConversationWorkspace()
    private val pending = ConversationPendingChanges(saveClock)
    private val runs = mutableMapOf<Uuid, ConversationGeneration>()

    sealed interface Notice {
        data class Rejected(val conversationId: Uuid, val error: AssistantFailure) : Notice
        data class Accepted(val conversationId: Uuid, val editorId: String, val revision: Long) :
            Notice

    }

    init {
        scope.launch { writes.withLock { initialize() } }
        scope.launch {
            while (isActive) {
                delay(100.milliseconds)
                writes.withLock {
                    if (state.value.storage == ConversationStorageState.READY) {
                        val ids = stateMutex.withLock {
                            pending.dueIds()
                        }
                        for (id in ids) {
                            if (!flush(id)) break
                        }
                    }
                }
            }
        }
    }

    private suspend fun initialize(): Boolean {
        if (initialized) return true
        stateMutex.withLock {
            mutableState.value = state.value.copy(storage = ConversationStorageState.LOADING)
        }
        return try {
            store.open()
            workspace = store.workspace()
            val selected = workspace.selectedId?.let { store.metadata(it) }
                ?: store.summaries(offset = 0, limit = 1).firstOrNull()
                    ?.let { store.metadata(it.id) }
                ?: store.blank()
                ?: state.value.selected
            workspace = workspace.copy(selectedId = selected.id)
            store.commit(metadata = selected, workspace = workspace)
            initialized = true
            loadSelected(selected.id)
            refreshSummaries()
            stateMutex.withLock {
                mutableState.value = state.value.copy(storage = ConversationStorageState.READY)
            }
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            initialized = false
            stateMutex.withLock {
                mutableState.value = state.value.copy(
                    storage = (failure as? ConversationStorageException)?.state
                        ?: ConversationStorageState.OPEN_FAILED
                )
            }
            false
        }
    }

    suspend fun retrySave() = writes.withLock {
        if (!initialized) {
            initialize()
            return@withLock
        }
        val ids = stateMutex.withLock { pending.keys.toList() }
        for (id in ids) {
            if (!flush(id)) return@withLock
        }
        try {
            refreshSummaries()
            stateMutex.withLock {
                mutableState.value = state.value.copy(storage = ConversationStorageState.READY)
            }
        } catch (_: Exception) {
            storageFailure()
        }
    }

    suspend fun updateDraft(
        id: Uuid,
        text: String,
        editorId: String,
        revision: Long,
        selectionStart: Int = text.length,
        selectionEnd: Int = selectionStart,
    ): Boolean {
        if (!initialized && !writes.withLock { initialize() }) return false
        if (text.length > AssistantLimits.INPUT_CHARS) return false
        suspend fun edit(current: AssistantSessionState): Boolean = stateMutex.withLock {
            val latest = pending[id]?.metadata ?: current
            if (latest.editorId == editorId && revision <= latest.draftRevision) return@withLock false
            stageLocked(
                metadata = latest.copy(
                    draft = text,
                    editorId = editorId,
                    draftRevision = revision,
                    selectionStart = selectionStart.coerceIn(0, text.length),
                    selectionEnd = selectionEnd.coerceIn(0, text.length),
                ),
                delayMillis = 900,
            )
            true
        }

        val current = stateMutex.withLock {
            pending[id]?.metadata ?: state.value.selected.takeIf { it.id == id }
        }
        return if (current != null) {
            edit(current)
        } else {
            writes.withLock {
                readStorage(id) { store.metadata(id) }?.let { edit(it) } ?: false
            }
        }
    }

    suspend fun updatePosition(id: Uuid, position: ConversationPosition) = stateMutex.withLock {
        val current = pending[id]?.metadata ?: state.value.selected.takeIf { it.id == id }
        ?: return@withLock
        stageLocked(current.copy(position = position, rawPosition = null), delayMillis = 900)
    }

    suspend fun flushPending() = writes.withLock {
        val ids = stateMutex.withLock { pending.keys.toList() }
        for (id in ids) {
            if (!flush(id)) break
        }
    }

    suspend fun select(id: Uuid) = writes.withLock {
        if (!initialize() || state.value.storage != ConversationStorageState.READY) return@withLock
        if (!flush(state.value.selected.id)) return@withLock
        val target = metadata(id) ?: return@withLock
        val next = workspace.copy(selectedId = id)
        if (commit(target, workspaceUpdate = next)) {
            workspace = next
            loadSelected(id)
        }
    }

    suspend fun newConversation(context: AssistantContext? = null): Uuid = writes.withLock {
        if (!initialize() || state.value.storage != ConversationStorageState.READY) return@withLock state.value.selected.id
        if (!flush(state.value.selected.id)) return@withLock state.value.selected.id
        val current = state.value.selected
        val blank = current.takeIf { !it.retained && it.id !in state.value.runningIds }
            ?: readStorage { store.blank() }?.takeUnless { it.id in state.value.runningIds }
        val next = (blank ?: AssistantSessionState()).copy(
            context = (context ?: current.context).snapshot(),
        )
        val nextWorkspace = workspace.copy(selectedId = next.id)
        if (commit(next, workspaceUpdate = nextWorkspace)) {
            workspace = nextWorkspace
            loadSelected(next.id)
        }
        state.value.selected.id
    }

    suspend fun rename(id: Uuid, title: String) = writes.withLock {
        if (!ready()) return@withLock
        val current = metadata(id) ?: return@withLock
        val next = current.copy(
            title = title.trim().take(AssistantLimits.TITLE_CHARS),
            manuallyNamed = true
        )
        if (next.title.isNotEmpty() && commit(next)) refreshSummaries()
    }

    suspend fun resetContext(id: Uuid) = writes.withLock {
        if (!ready()) return@withLock
        val current = metadata(id) ?: return@withLock
        commit(current.copy(context = AssistantContext.Empty))
    }

    suspend fun delete(id: Uuid, stopReplyConfirmed: Boolean = false): Boolean = writes.withLock {
        if (!ready()) return@withLock false
        val run = stateMutex.withLock { runs[id] }
        if (run != null && !stopReplyConfirmed) return@withLock false
        stateMutex.withLock { run?.valid = false }
        run?.job?.cancel()
        try {
            val selected = if (workspace.selectedId == id) {
                store.summaries(offset = 0, limit = 2).firstOrNull { it.id != id }?.id
            } else workspace.selectedId
            val replacement = if (selected == null) {
                store.blank()?.takeUnless { it.id == id || it.id in state.value.runningIds }
                    ?: AssistantSessionState()
            } else null
            val nextWorkspace = workspace.copy(selectedId = selected ?: replacement!!.id)
            withContext(NonCancellable) {
                store.delete(id = id, workspace = nextWorkspace, replacement = replacement)
                workspace = nextWorkspace
                stateMutex.withLock {
                    pending.remove(id)
                    runMetadata.remove(id)
                }
                if (state.value.selected.id == id) loadSelected(nextWorkspace.selectedId!!)
            }
            refreshSummaries()
            true
        } catch (_: Exception) {
            storageFailure()
            false
        }
    }

    suspend fun stop(id: Uuid) {
        val run = stateMutex.withLock {
            runs[id]?.also { it.valid = false }
        } ?: return
        run.job?.cancel()
        withContext(NonCancellable) {
            writes.withLock { finish(id = id, run = run, status = AssistantTurnStatus.STOPPED) }
        }
    }

    /** Preparation owns a slot before resolving credentials, but does not yet acknowledge a send. */
    suspend fun submit(
        id: Uuid,
        editorId: String,
        revision: Long,
        retryTurnId: Long? = null,
    ): Boolean = writes.withLock {
        if (!ready()) return@withLock reject(id, AssistantError.CONVERSATION_STORAGE)
        val current = metadata(id) ?: return@withLock false
        if (!available || !scope.isActive) return@withLock reject(id, AssistantError.UNAVAILABLE)
        if (stateMutex.withLock { id in runs || runs.size >= AssistantLimits.GENERATIONS }) {
            return@withLock reject(id, AssistantError.GENERATION_LIMIT)
        }
        if (current.editorId != editorId || current.draftRevision != revision) {
            return@withLock reject(id, AssistantError.STALE_INPUT)
        }
        val retry = retryTurnId?.let { target ->
            readStorage(id) { store.tail(id = id, limit = 1) }.lastOrNull()?.takeIf {
                it.id == target && it.status in setOf(
                    AssistantTurnStatus.FAILED,
                    AssistantTurnStatus.STOPPED,
                    AssistantTurnStatus.INTERRUPTED
                )
            } ?: return@withLock false
        }
        val question = retry?.question ?: current.draft.trim()
        if (question.isBlank() || question.length > AssistantLimits.INPUT_CHARS) return@withLock false
        val context = (retry?.context ?: current.context).snapshot()
        if (context is UnreadableContext) {
            return@withLock reject(id, AssistantError.SEARCH_CONTEXT_REQUIRED)
        }
        val profile = settings.snapshot()
        val configId =
            profile.configId ?: return@withLock reject(id, AssistantError.CONFIGURATION_REQUIRED)
        val run = ConversationGeneration()
        stateMutex.withLock {
            runs[id] = run
            publishRunningLocked()
        }
        run.job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                val connection =
                    settings.resolveConnection(
                        configId = configId,
                        expectedRevision = profile.revision
                    )
                val accepted = writes.withLock {
                    val latest = metadata(id) ?: return@withLock false
                    if (!stateMutex.withLock { run.valid && runs[id] === run } || !ready()) return@withLock false
                    val turn = AssistantSessionTurn(
                        id = retry?.id ?: (latest.lastTurnId + 1),
                        question = question,
                        context = context,
                        source = AssistantReplySource(
                            configId = configId,
                            profileName = profile.name,
                            revision = profile.revision,
                            baseUrl = connection.baseUrl,
                            modelId = connection.modelId,
                            protocol = connection.protocol.id,
                            reasoningEffort = connection.reasoningEffort?.id,
                        ),
                    )
                    val clear =
                        retry == null && latest.editorId == editorId && latest.draftRevision == revision
                    val next = latest.copy(
                        draft = if (clear) "" else latest.draft,
                        selectionStart = if (clear) 0 else latest.selectionStart,
                        selectionEnd = if (clear) 0 else latest.selectionEnd,
                        title = if (!latest.manuallyNamed && latest.lastTurnId == 0L) {
                            question.take(AssistantLimits.TITLE_CHARS)
                        } else {
                            latest.title
                        },
                        lastTurnId = turn.id,
                        lastStatus = turn.status,
                        activityAt = now(),
                    )
                    if (!commit(metadata = next, turn = turn)) return@withLock false
                    stateMutex.withLock { run.turn = turn }
                    if (retry == null) {
                        notifications.tryEmit(
                            Notice.Accepted(
                                conversationId = id,
                                editorId = editorId,
                                revision = revision
                            )
                        )
                    }
                    refreshSummaries()
                    true
                }
                if (!accepted) return@launch
                val history = readStorage(id) {
                    store.tail(id = id, limit = AssistantLimits.HISTORY_TURNS + 1)
                }
                val request = run.request(
                    turns = history,
                    question = question,
                    context = context
                )
                currentCoroutineContext().ensureActive()
                assistant.run(request = request, connection = connection).takeWhile { event ->
                    val terminal =
                        event is AssistantRunEvent.Completed || event is AssistantRunEvent.Failed
                    update(id = id, run = run, event = event)
                    if (terminal) writes.withLock { flush(id) }
                    !terminal && stateMutex.withLock { run.valid }
                }.collect()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                val error = (failure as? AssistantException)?.error ?: AssistantError.NETWORK
                withContext(NonCancellable) {
                    writes.withLock {
                        if (run.turn == null) {
                            reject(id, error)
                        } else {
                            finish(
                                id = id,
                                run = run,
                                status = AssistantTurnStatus.FAILED,
                                error = error
                            )
                        }
                    }
                }
            } finally {
                withContext(NonCancellable) {
                    try {
                        writes.withLock {
                            finish(
                                id = id,
                                run = run,
                                status = if (run.valid) AssistantTurnStatus.FAILED else AssistantTurnStatus.STOPPED,
                                error = if (run.valid) AssistantError.STREAM_INTERRUPTED else null,
                            )
                        }
                    } finally {
                        stateMutex.withLock {
                            if (runs[id] === run) runs.remove(id)
                            publishRunningLocked()
                        }
                    }
                }
            }
        }
        run.job!!.start()
        true
    }

    private suspend fun update(
        id: Uuid,
        run: ConversationGeneration,
        event: AssistantRunEvent,
    ) = stateMutex.withLock {
        if (runs[id] !== run || !run.valid) return@withLock
        val next = run.update(event) ?: return@withLock
        val current = pending[id]?.metadata ?: runMetadata[id] ?: return@withLock
        stageLocked(
            metadata = current.copy(
                lastStatus = next.status,
                activityAt = if (next.status == AssistantTurnStatus.RUNNING) current.activityAt else now(),
            ),
            turn = next,
            delayMillis = if (next.status == AssistantTurnStatus.RUNNING) 1900 else 0,
        )
    }

    // At most three metadata rows, no completed conversation cache.
    private val runMetadata = mutableMapOf<Uuid, AssistantSessionState>()

    private suspend fun finish(
        id: Uuid,
        run: ConversationGeneration,
        status: AssistantTurnStatus,
        error: AssistantFailure? = null
    ) {
        val current = stateMutex.withLock { pending[id]?.metadata ?: runMetadata[id] } ?: return
        stateMutex.withLock {
            if (runs[id] !== run) return@withLock
            val turn =
                run.turn?.takeIf { it.status == AssistantTurnStatus.RUNNING } ?: return@withLock
            val next = turn.copy(status = status, progress = null, error = error)
            run.turn = next
            stageLocked(
                metadata = current.copy(lastStatus = status, activityAt = now()),
                turn = next,
                delayMillis = 0
            )
        }
        if (state.value.storage == ConversationStorageState.READY) flush(id)
    }

    /** Must hold stateMutex. The first deadline is retained under continuous changes. */
    private fun stageLocked(
        metadata: AssistantSessionState,
        turn: AssistantSessionTurn? = null,
        delayMillis: Long
    ) {
        val next = pending.stage(metadata = metadata, turn = turn, delayMillis = delayMillis)
        projectPendingLocked(next)
    }

    private fun projectPendingLocked(value: PendingConversationChange) {
        mutableState.value = state.value.withPending(value)
    }

    private suspend fun metadata(id: Uuid): AssistantSessionState? =
        stateMutex.withLock { pending[id]?.metadata } ?: readStorage(id) { store.metadata(id) }

    /** Capture version before I/O; reconcile edits made while SQLite was committing. */
    private suspend fun commit(
        metadata: AssistantSessionState,
        turn: AssistantSessionTurn? = null,
        workspaceUpdate: ConversationWorkspace? = null,
    ): Boolean {
        val captured = stateMutex.withLock { pending[metadata.id] }
        val writing = pending.prepare(metadata = metadata, captured = captured)
        return try {
            withContext(NonCancellable) {
                store.commit(
                    metadata = writing,
                    turn = turn ?: captured?.turn,
                    workspace = workspaceUpdate
                )
            }
            stateMutex.withLock {
                val merged = pending.committed(
                    writing = writing,
                    turn = turn ?: captured?.turn,
                    captured = captured,
                )
                if (metadata.id in runs) runMetadata[metadata.id] = merged.copy(turns = emptyList())
                val remaining = pending[metadata.id]
                if (remaining?.turn != null) {
                    projectPendingLocked(remaining)
                } else {
                    projectLocked(metadata = merged, turn = turn ?: captured?.turn)
                }
            }
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            storageFailure(metadata.id)
            false
        }
    }

    private suspend fun flush(id: Uuid): Boolean {
        val value = stateMutex.withLock { pending[id] } ?: return true
        val success = commit(metadata = value.metadata, turn = value.turn)
        if (success && value.turn?.status != AssistantTurnStatus.RUNNING) {
            try {
                refreshSummaries()
            } catch (_: Exception) {
                storageFailure()
                return false
            }
        }
        return success
    }

    private suspend fun storageFailure(id: Uuid = state.value.selected.id) {
        stateMutex.withLock {
            mutableState.value = state.value.copy(storage = ConversationStorageState.SAVE_FAILED)
            pending.values.forEach { value ->
                projectLocked(
                    metadata = value.metadata.copy(lastStatus = value.turn?.let { AssistantTurnStatus.STOPPED }
                        ?: value.metadata.lastStatus),
                    turn = value.turn?.copy(
                        status = AssistantTurnStatus.STOPPED,
                        progress = null,
                        error = AssistantError.CONVERSATION_STORAGE
                    ),
                )
            }
            runs.values.forEach {
                it.valid = false
                it.job?.cancel()
            }
        }
        notifications.tryEmit(
            Notice.Rejected(
                conversationId = id,
                error = AssistantError.CONVERSATION_STORAGE
            )
        )
    }

    private suspend fun <T> readStorage(
        id: Uuid = state.value.selected.id,
        read: suspend () -> T
    ): T = try {
        read()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        storageFailure(id)
        throw AssistantException(AssistantError.CONVERSATION_STORAGE)
    }

    private suspend fun ready() =
        initialize() && state.value.storage == ConversationStorageState.READY

    private fun projectLocked(metadata: AssistantSessionState, turn: AssistantSessionTurn?) {
        mutableState.value = state.value.withConversation(metadata = metadata, turn = turn)
    }

    private fun publishRunningLocked() {
        runMetadata.keys.retainAll(runs.keys)
        mutableState.value = state.value.copy(runningIds = runs.keys.toSet())
    }

    private suspend fun refreshSummaries(offset: Int = state.value.summaryOffset) {
        val values = readStorage { store.summaries(offset = offset, limit = 51) }
        stateMutex.withLock {
            mutableState.value = state.value.copy(
                summaries = values.take(50),
                summaryOffset = offset,
                hasMoreSummaries = values.size > 50
            )
            pending.values.forEach { projectPendingLocked(it) }
        }
    }

    suspend fun loadSummaries(offset: Int) = writes.withLock {
        if (ready()) refreshSummaries(offset.coerceAtLeast(0))
    }

    private suspend fun loadSelected(id: Uuid, anchor: Long? = null) {
        val metadata = metadata(id) ?: return
        val position = metadata.position
        val window = readStorage(id) {
            store.window(
                id = id,
                anchor = anchor ?: position.turnId.takeUnless { position.followLatest })
        }
        stateMutex.withLock {
            val latest = pending[id]?.metadata ?: metadata
            mutableState.value = state.value.copy(
                selected = latest.copy(
                    turns = window.turns,
                    hasBefore = window.hasBefore,
                    hasAfter = window.hasAfter
                ),
                windowRevision = state.value.windowRevision + 1,
            )
            pending[id]?.let { projectPendingLocked(it) }
        }
    }

    suspend fun loadWindow(id: Uuid, anchor: Long?) = writes.withLock {
        if (ready() && state.value.selected.id == id) loadSelected(id = id, anchor = anchor)
    }

    /** Best effort shutdown; periodic bounded saves remain the crash guarantee. */
    fun close(): Job = scope.launch {
        withContext(NonCancellable) {
            val jobs = stateMutex.withLock {
                runs.values.mapNotNull { it.job }.also { jobs ->
                    runs.values.forEach { it.valid = false }
                    jobs.forEach { it.cancel() }
                }
            }
            jobs.joinAll()
            try {
                writes.withLock {
                    stateMutex.withLock { pending.keys.toList() }.forEach { flush(it) }
                    store.close()
                }
            } catch (_: Exception) {
                storageFailure()
            } finally {
                scope.cancel()
            }
        }
    }

    private fun reject(id: Uuid, error: AssistantFailure): Boolean {
        notifications.tryEmit(Notice.Rejected(conversationId = id, error = error))
        return false
    }

    private fun now() = Clock.System.now().toEpochMilliseconds()
}
