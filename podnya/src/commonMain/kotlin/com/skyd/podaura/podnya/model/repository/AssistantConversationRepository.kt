package com.skyd.podaura.podnya.model.repository

import com.skyd.podaura.podnya.core.AssistantContent
import com.skyd.podaura.podnya.core.AssistantContext
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantException
import com.skyd.podaura.podnya.core.AssistantHistoryTurn
import com.skyd.podaura.podnya.core.AssistantLimits
import com.skyd.podaura.podnya.core.AssistantPhase
import com.skyd.podaura.podnya.core.AssistantRunEvent
import com.skyd.podaura.podnya.core.AssistantRuntime
import com.skyd.podaura.podnya.core.AssistantTurnRequest
import com.skyd.podaura.podnya.model.bean.AssistantSessionState
import com.skyd.podaura.podnya.model.bean.AssistantSessionTurn
import com.skyd.podaura.podnya.model.bean.AssistantTurnStatus
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** One process-local conversation. Generation belongs to this singleton, not a page or ViewModel. */
class AssistantConversationRepository(
    private val assistant: AssistantRuntime,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val mutableState = MutableStateFlow(AssistantSessionState())
    val state: StateFlow<AssistantSessionState> = mutableState.asStateFlow()
    val available: Boolean get() = assistant.available

    private val mutex = Mutex()
    private var generation = 0L
    private var requestId = 0L
    private var nextTurnId = 0L
    private var activeJob: Job? = null

    fun updateDraft(draft: String) {
        mutableState.update { it.copy(draft = draft.take(AssistantLimits.INPUT_CHARS)) }
    }

    suspend fun submit(question: String = state.value.draft): Boolean =
        mutex.withLock {
            val text = question.trim()
            if (!available || !scope.isActive || state.value.isRunning ||
                text.isEmpty() || text.length > AssistantLimits.INPUT_CHARS
            ) return@withLock false
            val context = state.value.context.snapshot()
            val turn = AssistantSessionTurn(id = ++nextTurnId, question = text, context = context)
            val request = request(text, context = context)
            mutableState.update {
                trim(
                    it.copy(
                        draft = if (it.draft.trim() == text) "" else it.draft,
                        turns = it.turns + turn,
                    )
                )
            }
            start(turn.id, request)
            true
        }

    /** Retry only the latest unsuccessful turn, replacing its assistant response in place. */
    suspend fun retry(): Boolean = mutex.withLock {
        val previous = state.value.turns.lastOrNull() ?: return@withLock false
        if (!available || !scope.isActive || previous.status !in
            listOf(AssistantTurnStatus.FAILED, AssistantTurnStatus.STOPPED)
        ) return@withLock false
        val request =
            request(previous.question, excludingTurnId = previous.id, context = previous.context)
        mutableState.update {
            it.copy(
                turns = it.turns.dropLast(1) + AssistantSessionTurn(
                    id = previous.id,
                    question = previous.question,
                    context = previous.context,
                )
            )
        }
        start(previous.id, request)
        true
    }

    /** Release the execution scope when the application/DI container closes, never on page disposal. */
    fun close() {
        scope.cancel()
    }

    suspend fun stop() {
        val job = mutex.withLock {
            generation++
            val previous = activeJob
            activeJob = null
            mutableState.update { current ->
                if (!current.isRunning) current else trim(
                    current.copy(
                        turns = current.turns.dropLast(1) + current.turns.last().copy(
                            status = AssistantTurnStatus.STOPPED, progress = null,
                        )
                    )
                )
            }
            previous
        }
        job?.cancel()
    }

    suspend fun newConversation(context: AssistantContext = state.value.context) {
        val job = mutex.withLock {
            generation++
            val previous = activeJob
            activeJob = null
            mutableState.value = AssistantSessionState(context = context.snapshot())
            previous
        }
        job?.cancel()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun start(turnId: Long, request: AssistantTurnRequest) {
        val runGeneration = generation
        val runId = ++requestId
        // ATOMIC guarantees cleanup even if the repository closes before the job is dispatched.
        activeJob = scope.launch(start = CoroutineStart.ATOMIC) {
            try {
                currentCoroutineContext().ensureActive()
                assistant.run(request).takeWhile { event ->
                    update(runGeneration, runId, turnId) { turn ->
                        when (event) {
                            is AssistantRunEvent.Progress -> turn.copy(progress = event.progress)
                            is AssistantRunEvent.ContentReady -> {
                                checkContent(event.content)
                                turn.copy(content = event.content)
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
                                    content = turn.content ?: event.content,
                                )
                            }
                        }
                    }
                    event !is AssistantRunEvent.Completed && event !is AssistantRunEvent.Failed
                }.collect()
            } catch (cancellation: CancellationException) {
                withContext(NonCancellable) {
                    update(runGeneration, runId, turnId) {
                        it.copy(status = AssistantTurnStatus.STOPPED, progress = null)
                    }
                }
                throw cancellation
            } catch (failure: Exception) {
                update(runGeneration, runId, turnId) {
                    it.copy(
                        status = AssistantTurnStatus.FAILED, progress = null,
                        error = (failure as? AssistantException)?.error ?: AssistantError.NETWORK,
                    )
                }
            } finally {
                withContext(NonCancellable) {
                    // A flow ending without Completed is an interrupted reply, never a success.
                    update(runGeneration, runId, turnId) {
                        it.copy(
                            status = AssistantTurnStatus.FAILED,
                            progress = null,
                            error = AssistantError.STREAM_INTERRUPTED
                        )
                    }
                    mutex.withLock {
                        if (generation == runGeneration && requestId == runId) activeJob = null
                    }
                }
            }
        }
    }

    private suspend fun update(
        runGeneration: Long,
        runId: Long,
        turnId: Long,
        transform: (AssistantSessionTurn) -> AssistantSessionTurn,
    ) = mutex.withLock {
        if (generation != runGeneration || requestId != runId) return@withLock
        mutableState.update { current ->
            val turn = current.turns.lastOrNull()
            if (turn?.id != turnId || turn.status != AssistantTurnStatus.RUNNING) current
            else trim(current.copy(turns = current.turns.dropLast(1) + transform(turn)))
        }
    }

    private fun request(
        question: String,
        excludingTurnId: Long? = null,
        context: AssistantContext,
    ): AssistantTurnRequest {
        var history = state.value.turns.asSequence()
            .filter { it.id != excludingTurnId && it.status != AssistantTurnStatus.RUNNING }
            .mapNotNull { turn ->
                val snapshot = turn.content?.historySnapshot()
                when {
                    // Only capability-validated snapshots survive interrupted prose.
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
        while (history.isNotEmpty() &&
            Json.encodeToString(history)
                .encodeToByteArray().size + questionBytes > AssistantLimits.PROMPT_BYTES
        ) history = history.drop(1)
        return AssistantTurnRequest(question, context.snapshot(), history)
    }

    private fun checkContent(content: AssistantContent) {
        if (content.retainedBytes !in 0..AssistantLimits.CONTENT_BYTES) {
            throw AssistantException(AssistantError.BUDGET_EXHAUSTED)
        }
    }

    private fun trim(state: AssistantSessionState): AssistantSessionState {
        var turns = state.turns
        var trimmed = state.historyTrimmed
        while (turns.size > 1 && (turns.count { it.status != AssistantTurnStatus.RUNNING } > AssistantLimits.SESSION_TURNS ||
                    turns.sumOf { it.displayBytes() } > SESSION_BYTES)
        ) {
            turns = turns.drop(1)
            trimmed = true
        }
        return state.copy(turns = turns, historyTrimmed = trimmed)
    }

    private fun AssistantSessionTurn.displayBytes(): Int =
        question.encodeToByteArray().size + reply.encodeToByteArray().size + (content?.retainedBytes
            ?: 0) + 128

    private companion object {
        const val SESSION_BYTES = AssistantLimits.SESSION_BYTES
    }
}
