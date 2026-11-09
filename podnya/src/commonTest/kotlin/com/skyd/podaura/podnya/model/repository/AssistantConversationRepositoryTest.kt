package com.skyd.podaura.podnya.model.repository

import com.skyd.podaura.podnya.model.bean.AssistantTurnStatus
import com.skyd.podaura.podnya.core.AssistantConnectionResult
import com.skyd.podaura.podnya.core.AssistantContent
import com.skyd.podaura.podnya.core.AssistantContext
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantHistorySnapshot
import com.skyd.podaura.podnya.core.AssistantLimits
import com.skyd.podaura.podnya.core.AssistantRunEvent
import com.skyd.podaura.podnya.core.AssistantRuntime
import com.skyd.podaura.podnya.core.AssistantTurnRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AssistantConversationRepositoryTest {
    @Test
    fun stoppingCancelsWorkPreservesPartialDisplayAndUsesOnlyValidatedContext() = runTest {
        var cancelled = false
        val assistant = FakeAssistant { _, index -> flow {
            if (index == 0) {
                try {
                    emit(AssistantRunEvent.ContentReady(content()))
                    emit(AssistantRunEvent.ReplyDelta("unfinished network reply"))
                    awaitCancellation()
                } finally {
                    cancelled = true
                }
            } else emit(AssistantRunEvent.Completed)
        } }
        val repository = AssistantConversationRepository(assistant, backgroundScope)
        repository.updateDraft("first question")
        assertTrue(repository.submit())
        assertFalse(repository.submit("duplicate"))
        runCurrent()
        repository.stop()
        runCurrent()
        assertTrue(cancelled)
        val stopped = repository.state.value.turns.single()
        assertEquals(AssistantTurnStatus.STOPPED, stopped.status)
        assertEquals("unfinished network reply", stopped.reply)
        assertEquals("episode-id", (stopped.content as SampleContent).payload)
        assertEquals("", repository.state.value.draft)
        assertTrue(repository.submit("follow up"))
        runCurrent()
        assertEquals("RESULTS", assistant.requests.last().history.single().reply)
        assertEquals("episode-id", assistant.requests.last().history.single().content?.payload)
    }

    @Test
    fun emptyResultDraftsKeepTheOutcomeAndCardsInFollowUpContext() = runTest {
        val assistant = FakeAssistant { _, index -> flow {
            if (index == 0) {
                emit(AssistantRunEvent.ContentReady(SampleContent("NO_MATCH", "")))
                emit(AssistantRunEvent.Failed(AssistantError.STREAM_INTERRUPTED))
            } else {
                emit(AssistantRunEvent.ContentReady(content()))
                emit(AssistantRunEvent.Completed)
            }
        } }
        val repository = AssistantConversationRepository(assistant, backgroundScope)
        repository.submit("first search")
        runCurrent()
        assertEquals("", (repository.state.value.turns.single().content as SampleContent).payload)
        repository.submit("try another keyword")
        runCurrent()
        repository.submit("more like that")
        runCurrent()
        val history = assistant.requests.last().history
        assertEquals(listOf("NO_MATCH", "RESULTS"), history.map { it.reply })
        assertTrue(history.first().content!!.payload.isEmpty())
        assertEquals("episode-id", history.last().content?.payload)
    }

    @Test
    fun repositoryClosureBeforeDispatchClearsBusyAndRejectsFurtherWork() = runTest {
        val owner = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val assistant = FakeAssistant { _, _ -> flow { awaitCancellation() } }
        val repository = AssistantConversationRepository(assistant, owner)
        assertTrue(repository.submit("question"))
        repository.close()
        runCurrent()
        assertFalse(repository.state.value.isRunning)
        assertEquals(AssistantTurnStatus.STOPPED, repository.state.value.turns.single().status)
        assertTrue(assistant.requests.isEmpty())
        assertFalse(repository.submit("another"))
    }

    @Test
    fun cancellingTheSubmittingPageDoesNotCancelItsReply() = runTest {
        val finish = CompletableDeferred<Unit>()
        val assistant = FakeAssistant { _, _ -> flow {
            emit(AssistantRunEvent.ReplyDelta("partial"))
            finish.await()
            emit(AssistantRunEvent.ReplyDelta(" complete"))
            emit(AssistantRunEvent.Completed)
        } }
        val repository = AssistantConversationRepository(assistant, backgroundScope)
        val page = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        page.launch { repository.submit("question") }
        runCurrent()
        assertEquals("partial", repository.state.value.turns.single().reply)
        page.cancel()
        runCurrent()
        assertTrue(repository.state.value.isRunning)
        finish.complete(Unit)
        runCurrent()
        assertEquals(AssistantTurnStatus.COMPLETED, repository.state.value.turns.single().status)
        assertEquals("partial complete", repository.state.value.turns.single().reply)
        assertEquals(1, assistant.requests.size)
    }

    @Test
    fun newConversationRejectsLateEventsEvenFromNonCooperativeFlow() = runTest {
        val lateEvents = CompletableDeferred<Unit>()
        val assistant = FakeAssistant { _, index ->
            if (index == 0) object : Flow<AssistantRunEvent> {
                override suspend fun collect(collector: FlowCollector<AssistantRunEvent>) {
                    withContext(NonCancellable) {
                        lateEvents.await()
                        collector.emit(AssistantRunEvent.ReplyDelta("stale text"))
                        collector.emit(AssistantRunEvent.ContentReady(content()))
                        collector.emit(AssistantRunEvent.Completed)
                    }
                }
            } else flow {
                emit(AssistantRunEvent.ReplyDelta("new reply"))
                emit(AssistantRunEvent.Completed)
            }
        }
        val repository = AssistantConversationRepository(assistant, backgroundScope)
        repository.submit("old question")
        runCurrent()
        repository.newConversation()
        assertTrue(repository.state.value.turns.isEmpty())
        repository.submit("new question")
        runCurrent()
        lateEvents.complete(Unit)
        runCurrent()
        val current = repository.state.value.turns.single()
        assertEquals("new question", current.question)
        assertEquals("new reply", current.reply)
        assertEquals(null, current.content)
        assertEquals(AssistantTurnStatus.COMPLETED, current.status)
    }

    @Test
    fun retryReplacesFailedResponseWithoutDuplicatingQuestionOrIncludingFailureInContext() = runTest {
        val assistant = FakeAssistant { _, index -> flow {
            when (index) {
                0 -> {
                    emit(AssistantRunEvent.ContentReady(content()))
                    emit(AssistantRunEvent.Completed)
                }
                1 -> {
                    emit(AssistantRunEvent.ReplyDelta("partial failed reply"))
                    emit(AssistantRunEvent.Failed(AssistantError.NETWORK, SampleContent("fallback", "fallback", validated = false)))
                }
                else -> {
                    emit(AssistantRunEvent.ReplyDelta("retry reply"))
                    emit(AssistantRunEvent.Completed)
                }
            }
        } }
        val repository = AssistantConversationRepository(assistant, backgroundScope)
        repository.submit("success")
        runCurrent()
        repository.submit("retry me")
        runCurrent()
        val failedId = repository.state.value.turns.last().id
        assertEquals("fallback", (repository.state.value.turns.last().content as SampleContent).payload)
        assertTrue(repository.retry())
        assertFalse(repository.retry())
        runCurrent()
        assertEquals(listOf("success", "retry me"), repository.state.value.turns.map { it.question })
        assertEquals(failedId, repository.state.value.turns.last().id)
        assertEquals("retry reply", repository.state.value.turns.last().reply)
        assertEquals(null, repository.state.value.turns.last().content)
        assertEquals(listOf("success"), assistant.requests.last().history.map { it.question })
        assertFalse(repository.retry())
    }

    @Test
    fun draftSurvivesRequestAndDisplayAndContextKeepDifferentTurnLimits() = runTest {
        val assistant = FakeAssistant { _, _ -> flow {
            emit(AssistantRunEvent.ContentReady(content()))
            emit(AssistantRunEvent.Completed)
        } }
        val repository = AssistantConversationRepository(assistant, backgroundScope)
        repository.updateDraft("saved draft")
        repeat(60) {
            assertTrue(repository.submit("question $it"))
            runCurrent()
        }
        assertEquals("saved draft", repository.state.value.draft)
        assertEquals(50, repository.state.value.turns.size)
        assertEquals("question 10", repository.state.value.turns.first().question)
        assertTrue(repository.state.value.historyTrimmed)
        assertEquals((53..58).map { "question $it" }, assistant.requests.last().history.map { it.question })
        repository.newConversation()
        assertEquals("", repository.state.value.draft)
        assertFalse(repository.state.value.historyTrimmed)
    }

    @Test
    fun historyBudgetCountsUtf8AndDropsWholeTurns() = runTest {
        val assistant = FakeAssistant { _, _ -> flow {
            emit(AssistantRunEvent.ContentReady(content(payload = "喵".repeat(10_000))))
            emit(AssistantRunEvent.Completed)
        } }
        val repository = AssistantConversationRepository(assistant, backgroundScope)
        repeat(7) {
            repository.submit("question $it")
            runCurrent()
        }
        val request = assistant.requests.last()
        assertEquals(listOf("question 4", "question 5"), request.history.map { it.question })
        assertTrue(Json.encodeToString(request.history).encodeToByteArray().size <= AssistantLimits.PROMPT_BYTES)
    }

    @Test
    fun displayByteBudgetEvictsWholeTurnsAndRejectsOversizedContent() = runTest {
        val assistant = FakeAssistant { _, index -> flow {
            val title = if (index == 12) "x".repeat(2 * 1024 * 1024) else "x".repeat(200_000)
            emit(AssistantRunEvent.ContentReady(content(title)))
            emit(AssistantRunEvent.Completed)
        } }
        val repository = AssistantConversationRepository(assistant, backgroundScope)
        repeat(12) {
            repository.submit("question $it")
            runCurrent()
        }
        assertEquals(10, repository.state.value.turns.size)
        assertTrue(repository.state.value.historyTrimmed)
        repository.submit("oversized card")
        runCurrent()
        assertEquals("oversized card", repository.state.value.turns.last().question)
        assertEquals(AssistantError.BUDGET_EXHAUSTED, repository.state.value.turns.last().error)
        assertEquals(null, repository.state.value.turns.last().content)
        assertTrue(repository.state.value.turns.dropLast(1).all { (it.content as SampleContent).payload.length == 200_000 })
    }

    @Test
    fun contextIsFrozenForSubmissionAndRetryAndFlowWithoutCompletionIsFailure() = runTest {
        val feeds = mutableSetOf("feed-a")
        val assistant = FakeAssistant { _, _ -> flow { emit(AssistantRunEvent.ReplyDelta("partial")) } }
        val repository = AssistantConversationRepository(assistant, backgroundScope)
        repository.newConversation(SampleContext(feeds))
        feeds += "feed-b"
        repository.submit("question")
        runCurrent()
        assertEquals(setOf("feed-a"), (assistant.requests.single().context as SampleContext).ids)
        assertEquals(AssistantError.STREAM_INTERRUPTED, repository.state.value.turns.single().error)
        assertEquals(AssistantTurnStatus.FAILED, repository.state.value.turns.single().status)
        assertTrue(repository.retry())
        runCurrent()
        assertEquals(setOf("feed-a"), (assistant.requests.last().context as SampleContext).ids)
    }

    private class FakeAssistant(
        private val events: (AssistantTurnRequest, Int) -> Flow<AssistantRunEvent>,
    ) : AssistantRuntime {
        override val available = true
        val requests = mutableListOf<AssistantTurnRequest>()
        override fun run(request: AssistantTurnRequest): Flow<AssistantRunEvent> {
            val index = requests.size
            requests += request
            return events(request, index)
        }
        override suspend fun verify() = AssistantConnectionResult(success = true)
    }

    // A non-article capability proves that the shared session has no article DTO dependency.
    private data class SampleContext(val ids: Set<String>) : AssistantContext {
        override fun snapshot(): AssistantContext = copy(ids = ids.toSet())
    }

    private data class SampleContent(
        val summary: String,
        val payload: String,
        val validated: Boolean = true,
    ) : AssistantContent {
        override val retainedBytes: Int get() = summary.encodeToByteArray().size + payload.encodeToByteArray().size
        override fun historySnapshot(): AssistantHistorySnapshot? = if (validated)
            AssistantHistorySnapshot("episodes", summary, payload) else null
    }

    private fun content(payload: String = "episode-id") = SampleContent("RESULTS", payload)
}
