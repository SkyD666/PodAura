package com.skyd.podaura.podnya.model.repository

import com.skyd.podaura.podnya.core.AssistantContent
import com.skyd.podaura.podnya.core.AssistantContext
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantException
import com.skyd.podaura.podnya.core.AssistantFailure
import com.skyd.podaura.podnya.core.AssistantHistoryTurn
import com.skyd.podaura.podnya.core.AssistantLimits
import com.skyd.podaura.podnya.core.AssistantPhase
import com.skyd.podaura.podnya.core.AssistantRunEvent
import com.skyd.podaura.podnya.core.AssistantRuntime
import com.skyd.podaura.podnya.core.AssistantTurnRequest
import com.skyd.podaura.podnya.model.bean.AssistantConversationsState
import com.skyd.podaura.podnya.model.bean.AssistantReplySource
import com.skyd.podaura.podnya.model.bean.AssistantSessionState
import com.skyd.podaura.podnya.model.bean.AssistantSessionTurn
import com.skyd.podaura.podnya.model.bean.AssistantTurnStatus
import com.skyd.podaura.podnya.settings.AssistantSettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.uuid.Uuid

/** Application-owned commands and tasks. Store is the only source of conversation data. */
class AssistantConversationRepository(
    private val assistant: AssistantRuntime,
    private val settings: AssistantSettingsRepository,
    private val store: ConversationStore = InMemoryConversationStore(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val mutableState = MutableStateFlow(AssistantConversationsState())
    val state = mutableState.asStateFlow()
    val available: Boolean get() = assistant.available
    private val notifications = MutableSharedFlow<Notice>(extraBufferCapacity = 64)
    val notices = notifications.asSharedFlow()
    private val mutex = Mutex()
    private var selectedId = state.value.selected.id
    private var initialized = false
    private var lastModelId: String? = null
    private val runs = mutableMapOf<Uuid, Run>()

    data class Confirmation(
        val conversationId: Uuid,
        val editorId: String,
        val revision: Long,
        val configId: String,
        val configRevision: Long,
        val destination: String,
        val question: String,
        val retryTurnId: Long? = null,
    ) {
        override fun toString() = "Confirmation($conversationId, [redacted])"
    }

    sealed interface Notice {
        data class Rejected(val conversationId: Uuid, val error: AssistantFailure) : Notice
        data class DraftRejected(
            val conversationId: Uuid,
            val editorId: String,
            val revision: Long,
            val draft: String
        ) : Notice

        data class Accepted(val conversationId: Uuid, val editorId: String, val revision: Long) :
            Notice

        data class ConfirmDestination(val confirmation: Confirmation) : Notice
    }

    private class Run(val id: Uuid = Uuid.random(), var reserved: Long, var turnId: Long? = null) {
        lateinit var job: Job
        var valid = true
    }

    init {
        scope.launch { mutex.withLock { initialize(); publish() } }
    }

    private suspend fun initialize() {
        if (!initialized) {
            save(state.value.selected)
            initialized = true
        }
    }

    suspend fun updateDraft(id: Uuid, text: String, editorId: String, revision: Long): Boolean =
        mutex.withLock {
            initialize()
            val current = store.get(id) ?: return@withLock false
            if (text.length > AssistantLimits.INPUT_CHARS ||
                (current.editorId == editorId && revision <= current.draftRevision)
            ) return@withLock false
            val next = current.copy(draft = text, editorId = editorId, draftRevision = revision)
            if (!fits(current, next)) {
                // The rollback is itself the latest editor revision, even though its text did not grow.
                save(current.copy(editorId = editorId, draftRevision = revision))
                publish()
                notifications.tryEmit(Notice.DraftRejected(id, editorId, revision, current.draft))
                return@withLock reject(id, AssistantError.CONVERSATION_CAPACITY)
            }
            save(next)
            removeOtherBlank(selectedId)
            publish()
            true
        }

    suspend fun select(id: Uuid) = mutex.withLock {
        initialize()
        if (store.get(id) != null) {
            selectedId = id; publish()
        }
    }

    suspend fun newConversation(context: AssistantContext? = null): Uuid = mutex.withLock {
        initialize()
        val summaries = store.observeSummaries().first()
        val blank =
            summaries.firstOrNull { !it.retained && it.id !in runs }?.let { store.get(it.id) }
        val current = store.get(selectedId)!!
        val next = blank?.copy(
            context = (context ?: current.context).snapshot(),
            modelConfigId = lastModelId
        )
            ?: AssistantSessionState(
                context = (context ?: current.context).snapshot(),
                modelConfigId = lastModelId
            )
        // One empty editor is allowed even when all retained conversation slots are occupied.
        if (!fits(blank, next)) {
            reject(current.id, AssistantError.CONVERSATION_CAPACITY); return@withLock current.id
        }
        save(next)
        selectedId = next.id
        publish()
        next.id
    }

    suspend fun rename(id: Uuid, title: String) = mutex.withLock {
        initialize()
        val current = store.get(id) ?: return@withLock
        val next = current.copy(
            title = title.trim().take(AssistantLimits.TITLE_CHARS),
            manuallyNamed = true
        )
        if (next.title.isEmpty()) return@withLock
        if (!fits(current, next)) {
            reject(id, AssistantError.CONVERSATION_CAPACITY); return@withLock
        }
        save(next); publish()
    }

    suspend fun selectModel(id: Uuid, configId: String) {
        val config = settings.snapshot()
        mutex.withLock {
            initialize()
            val current = store.get(id) ?: return@withLock
            if (config.configId != configId || !config.configured) {
                reject(id, AssistantError.CONFIGURATION_REQUIRED); return@withLock
            }
            save(current.copy(modelConfigId = configId))
            lastModelId = configId
            publish()
        }
    }

    suspend fun delete(id: Uuid, stopReplyConfirmed: Boolean = false): Boolean = mutex.withLock {
        initialize()
        if (id in runs && !stopReplyConfirmed) return@withLock false
        runs[id]?.let { it.valid = false; it.job.cancel() }
        store.delete(id)
        if (selectedId == id) {
            selectedId =
                store.observeSummaries().first().firstOrNull()?.id ?: AssistantSessionState(
                    modelConfigId = lastModelId
                ).also { save(it) }.id
        }
        publish()
        true
    }

    suspend fun stop(id: Uuid) = mutex.withLock {
        val run = runs[id] ?: return@withLock
        run.valid = false
        try {
            finishTurn(id, run, AssistantTurnStatus.STOPPED)
        } finally {
            run.job.cancel()
        }
        publish()
        // Keep its slot/reservation until the task's finally has actually finished.
    }

    /** Admission is short. Resolving credentials happens in the application task, outside the lock. */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun submit(
        id: Uuid, editorId: String, revision: Long, configId: String?, configRevision: Long,
        retryTurnId: Long? = null, confirmation: Confirmation? = null,
    ): Boolean = mutex.withLock {
        initialize()
        val current = store.get(id) ?: return@withLock false
        if (!available || !scope.isActive) return@withLock reject(id, AssistantError.UNAVAILABLE)
        if (id in runs || runs.size >= AssistantLimits.GENERATIONS) return@withLock reject(
            id,
            AssistantError.GENERATION_LIMIT
        )
        if (current.editorId != editorId || current.draftRevision != revision) return@withLock reject(
            id,
            AssistantError.STALE_INPUT
        )
        val retry = retryTurnId?.let { target ->
            current.turns.lastOrNull()?.takeIf {
                it.id == target && it.status in listOf(
                    AssistantTurnStatus.FAILED,
                    AssistantTurnStatus.STOPPED
                )
            } ?: return@withLock false
        }
        val question = retry?.question ?: current.draft.trim()
        if (question.isBlank() || question.length > AssistantLimits.INPUT_CHARS) return@withLock false
        val model = current.modelConfigId ?: configId
        if (model == null || model != configId) return@withLock reject(
            id,
            AssistantError.CONFIGURATION_REQUIRED
        )
        val context = (retry?.context ?: current.context).snapshot()
        val reservation = AssistantLimits.TURN_BYTES.toLong() + context.retainedBytes + 8192
        val next = current.copy(modelConfigId = model)
        val count = store.observeSummaries().first().count { it.retained || it.id in runs }
        if ((!current.retained && count >= AssistantLimits.CONVERSATIONS) ||
            totalBytes() + reservedBytes() + reservation > AssistantLimits.CONVERSATION_BYTES
        ) {
            return@withLock reject(id, AssistantError.CONVERSATION_CAPACITY)
        }
        val run = Run(reserved = reservation)
        save(next)
        runs[id] = run
        lastModelId = model
        run.job = scope.launch(start = CoroutineStart.ATOMIC) {
            try {
                currentCoroutineContext().ensureActive()
                val connection = settings.resolveConnection(model, configRevision)
                val started = mutex.withLock {
                    val latest = store.get(id)
                    if (runs[id]?.id != run.id || !run.valid || latest == null) return@withLock false
                    if (confirmation != null && (latest.editorId != editorId ||
                                latest.draftRevision != revision || latest.modelConfigId != model)
                    ) {
                        reject(id, AssistantError.STALE_INPUT)
                        return@withLock false
                    }
                    val ticket = Confirmation(
                        id,
                        editorId,
                        revision,
                        model,
                        configRevision,
                        connection.baseUrl,
                        question,
                        retryTurnId
                    )
                    if (latest.turns.isNotEmpty() && connection.baseUrl !in latest.destinations && confirmation != ticket) {
                        notifications.tryEmit(Notice.ConfirmDestination(ticket)); return@withLock false
                    }
                    val turnId = retry?.id ?: ((latest.turns.lastOrNull()?.id ?: 0) + 1)
                    val turn = AssistantSessionTurn(
                        id = turnId,
                        question = question,
                        context = context,
                        source = AssistantReplySource(
                            configId = model,
                            revision = configRevision,
                            baseUrl = connection.baseUrl,
                            modelId = connection.modelId
                        ),
                    )
                    save(
                        latest.copy(
                            draft = if (retry == null && latest.editorId == editorId && latest.draftRevision == revision) "" else latest.draft,
                            title = if (!latest.manuallyNamed && latest.turns.isEmpty()) question.take(
                                AssistantLimits.TITLE_CHARS
                            ) else latest.title,
                            turns = (if (retry == null) latest.turns else latest.turns.dropLast(1)) + turn,
                            destinations = latest.destinations + connection.baseUrl,
                            activityAt = now(),
                        )
                    )
                    run.turnId = turnId
                    run.reserved = reservation - turn.retainedBytes
                    if (retry == null) notifications.tryEmit(
                        Notice.Accepted(id, editorId, revision)
                    )
                    publish()
                    true
                }
                if (!started) return@launch
                val request = request(current, question, retryTurnId, context)
                currentCoroutineContext().ensureActive()
                assistant.run(request, connection).takeWhile { event ->
                    update(id, run) { turn ->
                        when (event) {
                            is AssistantRunEvent.Progress -> turn.copy(progress = event.progress)
                            is AssistantRunEvent.ContentReady -> {
                                checkContent(event.content); turn.copy(content = event.content)
                            }

                            is AssistantRunEvent.ReplyDelta -> turn.copy(
                                reply = (turn.reply + event.text.take(AssistantLimits.REPLY_CHARS))
                                    .take(AssistantLimits.REPLY_CHARS),
                                progress = AssistantPhase.STREAMING,
                            )

                            AssistantRunEvent.Completed -> turn.copy(
                                status = AssistantTurnStatus.COMPLETED,
                                progress = null
                            )

                            is AssistantRunEvent.Failed -> {
                                event.content?.let(::checkContent)
                                turn.copy(
                                    status = AssistantTurnStatus.FAILED,
                                    progress = null,
                                    error = event.error,
                                    content = turn.content ?: event.content
                                )
                            }
                        }
                    }
                    event !is AssistantRunEvent.Completed && event !is AssistantRunEvent.Failed
                }.collect()
            } catch (cancelled: CancellationException) {
                withContext(NonCancellable) {
                    mutex.withLock {
                        try {
                            finishTurn(id, run, AssistantTurnStatus.STOPPED)
                        } catch (storageFailure: AssistantException) {
                            reject(id, storageFailure.error)
                        }
                    }
                }
                throw cancelled
            } catch (failure: Exception) {
                val error = (failure as? AssistantException)?.error ?: AssistantError.NETWORK
                mutex.withLock {
                    if (run.turnId == null && run.valid && store.get(id) != null) reject(id, error)
                    try {
                        finishTurn(id, run, AssistantTurnStatus.FAILED, error)
                    } catch (storageFailure: AssistantException) {
                        reject(id, storageFailure.error)
                    }
                }
            } finally {
                withContext(NonCancellable) {
                    mutex.withLock {
                        try {
                            finishTurn(
                                id = id,
                                run = run,
                                status = AssistantTurnStatus.FAILED,
                                error = AssistantError.STREAM_INTERRUPTED
                            )
                        } catch (storageFailure: AssistantException) {
                            reject(id, storageFailure.error)
                        } finally {
                            if (runs[id]?.id == run.id) runs.remove(id)
                            publish()
                        }
                    }
                }
            }
        }
        publish()
        true
    }

    fun close() {
        scope.cancel()
    }

    private suspend fun update(
        id: Uuid,
        run: Run,
        transform: (AssistantSessionTurn) -> AssistantSessionTurn
    ) = mutex.withLock {
        if (runs[id]?.id != run.id || !run.valid) return@withLock
        val current = store.get(id) ?: return@withLock
        val turn = current.turns.lastOrNull() ?: return@withLock
        if (turn.id != run.turnId || turn.status != AssistantTurnStatus.RUNNING) return@withLock
        val next = transform(turn)
        run.reserved = (run.reserved - (next.retainedBytes - turn.retainedBytes)).coerceAtLeast(0)
        save(
            current.copy(
                turns = current.turns.dropLast(1) + next,
                activityAt = if (next.status == AssistantTurnStatus.RUNNING) current.activityAt else now()
            )
        )
        publish()
    }

    private suspend fun finishTurn(
        id: Uuid,
        run: Run,
        status: AssistantTurnStatus,
        error: AssistantFailure? = null
    ) {
        if (runs[id]?.id != run.id) return
        val current = store.get(id) ?: return
        val turn = current.turns.lastOrNull() ?: return
        if (turn.id != run.turnId || turn.status != AssistantTurnStatus.RUNNING) return
        save(
            current.copy(
                turns = current.turns.dropLast(1) + turn.copy(
                    status = status,
                    progress = null,
                    error = error
                ), activityAt = now()
            )
        )
    }

    private suspend fun save(conversation: AssistantSessionState) {
        try {
            store.save(conversation)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            throw AssistantException(AssistantError.CONVERSATION_STORAGE)
        }
    }

    private suspend fun publish() {
        val all = store.observeSummaries().first()
        val selected = store.get(selectedId) ?: return
        mutableState.value =
            AssistantConversationsState(
                all.filter { it.retained || it.id in runs }, selected,
                runs.keys.toSet(), all.sumOf { it.retainedBytes }, reservedBytes()
            )
    }

    private suspend fun removeOtherBlank(keep: Uuid) {
        store.observeSummaries().first()
            .filter { !it.retained && it.id != keep && it.id != selectedId && it.id !in runs }
            .forEach { store.delete(it.id) }
    }

    private suspend fun totalBytes() = store.observeSummaries().first().sumOf { it.retainedBytes }
    private fun reservedBytes() = runs.values.sumOf { it.reserved }
    private suspend fun fits(
        previous: AssistantSessionState?,
        next: AssistantSessionState
    ): Boolean {
        val all = store.observeSummaries().first()
        return all.count { (it.retained || it.id in runs) && it.id != next.id } + (if (next.retained || next.id in runs) 1 else 0) <= AssistantLimits.CONVERSATIONS &&
                all.sumOf { it.retainedBytes } - (previous?.retainedBytes
            ?: 0) + next.retainedBytes + reservedBytes() <= AssistantLimits.CONVERSATION_BYTES
    }

    private fun reject(id: Uuid, error: AssistantFailure): Boolean {
        notifications.tryEmit(Notice.Rejected(id, error)); return false
    }

    private fun now() = Clock.System.now().toEpochMilliseconds()
    private fun checkContent(content: AssistantContent) {
        if (content.retainedBytes !in 0..AssistantLimits.CONTENT_BYTES) throw AssistantException(
            AssistantError.BUDGET_EXHAUSTED
        )
    }

    private fun request(
        current: AssistantSessionState,
        question: String,
        excluding: Long?,
        context: AssistantContext
    ): AssistantTurnRequest {
        var history = current.turns.asSequence()
            .filter { it.id != excluding && it.status != AssistantTurnStatus.RUNNING }
            .mapNotNull { turn ->
                val snapshot = turn.content?.historySnapshot()
                when {
                    snapshot != null -> AssistantHistoryTurn(
                        turn.question,
                        snapshot.summary,
                        snapshot
                    )

                    turn.status == AssistantTurnStatus.COMPLETED -> AssistantHistoryTurn(
                        turn.question,
                        turn.reply
                    )

                    else -> null
                }
            }.toList().takeLast(AssistantLimits.HISTORY_TURNS)
        val questionBytes = Json.encodeToString(question).encodeToByteArray().size
        while (history.isNotEmpty() && Json.encodeToString(history)
                .encodeToByteArray().size + questionBytes > AssistantLimits.PROMPT_BYTES
        ) history = history.drop(1)
        return AssistantTurnRequest(question, context, history)
    }
}
