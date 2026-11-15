package com.skyd.podaura.podnya.model.repository.conversation

import com.skyd.podaura.podnya.core.*
import com.skyd.podaura.podnya.model.bean.conversation.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlin.test.*
import kotlin.uuid.Uuid

class ConversationTransactionRegressionTest {
    private class Runtime : AssistantRuntime {
        override val available = true
        val events = Channel<AssistantRunEvent>(Channel.UNLIMITED)
        override fun run(request: AssistantTurnRequest, connection: AssistantConnection) = flow {
            for (event in events) emit(event)
        }
        override suspend fun verify() = AssistantConnectionResult(true)
    }

    @Test
    fun terminalStateMustWaitForTerminalTransaction() = runTest {
        val memory = InMemoryConversationStore()
        val intermediateEntered = CompletableDeferred<Unit>()
        val releaseIntermediate = CompletableDeferred<Unit>()
        val terminalEntered = CompletableDeferred<Unit>()
        val releaseTerminal = CompletableDeferred<Unit>()
        var gate = false
        val store = object : ConversationStore by memory {
            override suspend fun commit(metadata: AssistantSessionState?, turn: AssistantSessionTurn?, workspace: ConversationWorkspace?) {
                if (gate && turn != null) {
                    if (turn.status == AssistantTurnStatus.RUNNING) {
                        intermediateEntered.complete(Unit)
                        releaseIntermediate.await()
                    } else {
                        terminalEntered.complete(Unit)
                        releaseTerminal.await()
                    }
                }
                memory.commit(metadata = metadata, turn = turn, workspace = workspace)
            }
        }
        val runtime = Runtime()
        val settings = conversationSettings()
        val repo = AssistantConversationRepository(assistant = runtime, settings = settings, store = store, scope = backgroundScope)
        val observedStates = mutableListOf<Triple<AssistantTurnStatus?, AssistantTurnStatus?, AssistantTurnStatus?>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repo.state.collect { state ->
                observedStates += Triple(
                    first = state.selected.lastStatus,
                    second = state.selected.turns.lastOrNull()?.status,
                    third = state.summaries.firstOrNull { it.id == state.selected.id }?.status,
                )
            }
        }
        repo.editDraft("question")
        repo.sendDraft(settings)
        runCurrent()
        runtime.events.send(AssistantRunEvent.ReplyDelta("partial"))
        runCurrent()
        gate = true
        backgroundScope.launch { repo.flushPending() }
        runCurrent()
        assertTrue(intermediateEntered.isCompleted)
        runtime.events.send(AssistantRunEvent.Completed)
        runCurrent()
        releaseIntermediate.complete(Unit)
        runCurrent()
        assertTrue(terminalEntered.isCompleted)
        val observed = repo.state.value.selected.turns.single().status
        val disk = memory.tail(id = repo.state.value.selected.id, limit = 1).single().status
        assertEquals(AssistantTurnStatus.RUNNING, disk)
        val premature = observedStates.filter { it.first == AssistantTurnStatus.COMPLETED || it.second == AssistantTurnStatus.COMPLETED || it.third == AssistantTurnStatus.COMPLETED }
        releaseTerminal.complete(Unit)
        runCurrent()
        assertEquals(AssistantTurnStatus.RUNNING, observed)
        assertTrue(premature.isEmpty(), "No completed projection may escape before the terminal transaction commits")
        assertEquals(AssistantTurnStatus.COMPLETED, repo.state.value.selected.lastStatus)
        assertEquals(AssistantTurnStatus.COMPLETED, repo.state.value.summaries.single().status)
    }

    @Test
    fun editsOutsideSummaryPageMustNotInjectRowsIntoIt() = runTest {
        val memory = InMemoryConversationStore()
        val selected = AssistantSessionState(draft = "head", activityAt = 1000)
        memory.commit(metadata = selected, workspace = ConversationWorkspace(selectedId = selected.id))
        repeat(70) { memory.commit(metadata = AssistantSessionState(draft = "old $it", activityAt = it.toLong())) }
        val repo = AssistantConversationRepository(assistant = Runtime(), settings = conversationSettings(), store = memory, scope = backgroundScope)
        runCurrent()
        repo.loadSummaries(50)
        val expected = memory.summaries(offset = 50, limit = 50).map { it.id }
        repo.editDraft("updated head")
        assertEquals(expected, repo.state.value.summaries.map { it.id })
    }

    @Test
    fun failedReadOnSelectionMustPublishRecoverableStorageFailure() = runTest {
        val memory = InMemoryConversationStore()
        val selected = AssistantSessionState(draft = "selected")
        val target = AssistantSessionState(draft = "target")
        memory.commit(metadata = selected, workspace = ConversationWorkspace(selectedId = selected.id))
        memory.commit(metadata = target)
        var failRead = false
        val store = object : ConversationStore by memory {
            override suspend fun metadata(id: Uuid): AssistantSessionState? {
                check(!failRead)
                return memory.metadata(id)
            }
        }
        val repo = AssistantConversationRepository(assistant = Runtime(), settings = conversationSettings(), store = store, scope = backgroundScope)
        runCurrent()
        failRead = true
        val failure = assertFailsWith<AssistantException> { repo.select(target.id) }
        assertEquals(AssistantError.CONVERSATION_STORAGE, failure.error)
        assertNotEquals(ConversationStorageState.READY, repo.state.value.storage)
    }
}
