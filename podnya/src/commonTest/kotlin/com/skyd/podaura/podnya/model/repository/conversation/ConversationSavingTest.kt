package com.skyd.podaura.podnya.model.repository.conversation

import com.skyd.podaura.podnya.core.*
import com.skyd.podaura.podnya.model.bean.conversation.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlin.test.*

class ConversationSavingTest {
    private class Runtime : AssistantRuntime {
        override val available = true
        var calls = 0
        val events = Channel<AssistantRunEvent>(Channel.UNLIMITED)
        override fun run(request: AssistantTurnRequest, connection: AssistantConnection) = flow {
            calls++
            for (event in events) emit(event)
        }
        override suspend fun verify(connection: suspend () -> AssistantConnection) = AssistantConnectionResult(true)
    }

    @Test
    fun continuousDraftsAndDeltasHaveSaveDeadlinesAndDoNotRewriteEveryDelta() = runTest {
        val memory = InMemoryConversationStore()
        var turnWrites = 0
        val store = object : ConversationStore by memory {
            override suspend fun commit(metadata: AssistantSessionState?, turn: AssistantSessionTurn?, workspace: ConversationWorkspace?) {
                if (turn != null) turnWrites++
                memory.commit(metadata = metadata, turn = turn, workspace = workspace)
            }
        }
        val runtime = Runtime()
        val settings = conversationSettings()
        val repo = AssistantConversationRepository(assistant = runtime, settings = settings, store = store, scope = backgroundScope, saveClock = { testScheduler.currentTime })
        repeat(30) { index ->
            repo.editDraft("draft $index")
            advanceTimeBy(100)
            runCurrent()
            if (index >= 10) assertTrue(memory.metadata(repo.state.value.selected.id)!!.draft.isNotEmpty())
        }
        val id = repo.state.value.selected.id
        assertEquals("draft 26", memory.metadata(id)!!.draft)
        assertTrue(repo.sendDraft())
        runCurrent()
        val acceptedWrites = turnWrites
        repeat(40) {
            runtime.events.send(AssistantRunEvent.ReplyDelta("x"))
            runCurrent()
            advanceTimeBy(100)
            runCurrent()
            if (it == 20) assertTrue(memory.tail(id = id, limit = 1).single().reply.isNotEmpty())
        }
        assertTrue(turnWrites - acceptedWrites in 1..3)
        runtime.events.send(AssistantRunEvent.Completed)
        runCurrent()
        assertEquals("x".repeat(40), memory.tail(id = id, limit = 1).single().reply)
        assertEquals(AssistantTurnStatus.COMPLETED, memory.summaries(offset = 0, limit = 1).single().status)
    }

    @Test
    fun acceptanceWaitsForCommitAndNewEditorVersionSurvivesBothMemoryAndDisk() = runTest {
        val memory = InMemoryConversationStore()
        val writing = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var pauseAdmission = true
        val store = object : ConversationStore by memory {
            override suspend fun commit(metadata: AssistantSessionState?, turn: AssistantSessionTurn?, workspace: ConversationWorkspace?) {
                if (turn != null && pauseAdmission) {
                    writing.complete(Unit)
                    release.await()
                }
                memory.commit(metadata = metadata, turn = turn, workspace = workspace)
            }
        }
        val runtime = Runtime()
        val settings = conversationSettings()
        val repo = AssistantConversationRepository(assistant = runtime, settings = settings, store = store, scope = backgroundScope)
        val notices = mutableListOf<AssistantConversationRepository.Notice>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repo.notices.toList(notices) }
        repo.editDraft("original")
        repo.sendDraft()
        runCurrent()
        assertTrue(writing.isCompleted)
        assertEquals(0, runtime.calls)
        assertFalse(notices.any { it is AssistantConversationRepository.Notice.Accepted })
        repo.editDraft("new input")
        release.complete(Unit)
        pauseAdmission = false
        runCurrent()
        assertEquals(1, runtime.calls)
        assertEquals("new input", repo.state.value.selected.draft)
        val accepted = notices.filterIsInstance<AssistantConversationRepository.Notice.Accepted>().single()
        assertTrue(accepted.revision < repo.state.value.selected.draftRevision)
        repo.flushPending()
        assertEquals("new input", memory.metadata(repo.state.value.selected.id)!!.draft)
        assertEquals("original", memory.tail(id = repo.state.value.selected.id, limit = 1).single().question)
    }

    @Test
    fun failedTimedSaveRetainsTextStopsGenerationAndRetryNeverCallsModel() = runTest {
        val memory = InMemoryConversationStore()
        var fail = false
        val store = object : ConversationStore by memory {
            override suspend fun commit(metadata: AssistantSessionState?, turn: AssistantSessionTurn?, workspace: ConversationWorkspace?) {
                check(!fail)
                memory.commit(metadata = metadata, turn = turn, workspace = workspace)
            }
        }
        val runtime = Runtime()
        val settings = conversationSettings()
        val repo = AssistantConversationRepository(assistant = runtime, settings = settings, store = store, scope = backgroundScope, saveClock = { testScheduler.currentTime })
        repo.editDraft("question")
        repo.sendDraft()
        runCurrent()
        runtime.events.send(AssistantRunEvent.ReplyDelta("unsaved partial"))
        runCurrent()
        repo.editDraft("unsaved draft")
        fail = true
        advanceTimeBy(1000)
        runCurrent()
        assertEquals(ConversationStorageState.SAVE_FAILED, repo.state.value.storage)
        assertTrue(repo.state.value.runningIds.isEmpty())
        assertEquals("unsaved draft", repo.state.value.selected.draft)
        assertEquals("unsaved partial", repo.state.value.selected.turns.single().reply)
        assertFalse(repo.sendDraft())
        repo.retrySave()
        assertEquals(ConversationStorageState.SAVE_FAILED, repo.state.value.storage)
        fail = false
        repo.retrySave()
        assertEquals(1, runtime.calls)
        assertEquals(ConversationStorageState.READY, repo.state.value.storage)
        assertEquals("unsaved draft", memory.metadata(repo.state.value.selected.id)!!.draft)
        assertEquals("unsaved partial", memory.tail(id = repo.state.value.selected.id, limit = 1).single().reply)
    }
    @Test
    fun anOlderStreamWriteCannotClearNewerDeltasOrTerminalState() = runTest {
        val memory = InMemoryConversationStore()
        var pause = false
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val store = object : ConversationStore by memory {
            override suspend fun commit(metadata: AssistantSessionState?, turn: AssistantSessionTurn?, workspace: ConversationWorkspace?) {
                if (pause) {
                    pause = false
                    started.complete(Unit)
                    release.await()
                }
                memory.commit(metadata = metadata, turn = turn, workspace = workspace)
            }
        }
        val runtime = Runtime()
        val settings = conversationSettings()
        val repo = AssistantConversationRepository(assistant = runtime, settings = settings, store = store, scope = backgroundScope)
        repo.editDraft("question")
        repo.sendDraft()
        runCurrent()
        runtime.events.send(AssistantRunEvent.ReplyDelta("one"))
        runCurrent()
        pause = true
        val flush = backgroundScope.launch { repo.flushPending() }
        runCurrent()
        assertTrue(started.isCompleted)
        runtime.events.send(AssistantRunEvent.ReplyDelta("two"))
        runtime.events.send(AssistantRunEvent.Completed)
        runCurrent()
        assertEquals(AssistantTurnStatus.RUNNING, repo.state.value.selected.turns.single().status)
        release.complete(Unit)
        runCurrent()
        flush.join()
        val saved = memory.tail(id = repo.state.value.selected.id, limit = 1).single()
        assertEquals("onetwo", saved.reply)
        assertEquals(AssistantTurnStatus.COMPLETED, saved.status)
        assertEquals(saved.status, memory.summaries(offset = 0, limit = 1).single().status)
        assertEquals(saved.status, repo.state.value.selected.turns.single().status)
    }

    @Test
    fun failedDeleteAndRenameNeverPublishSuccessAndInitializationCanBeRetried() = runTest {
        val memory = InMemoryConversationStore()
        var failOpen = true
        var failWrite = false
        val store = object : ConversationStore by memory {
            override suspend fun open() {
                check(!failOpen)
                memory.open()
            }
            override suspend fun commit(metadata: AssistantSessionState?, turn: AssistantSessionTurn?, workspace: ConversationWorkspace?) {
                check(!failWrite)
                memory.commit(metadata = metadata, turn = turn, workspace = workspace)
            }
            override suspend fun delete(id: kotlin.uuid.Uuid, workspace: ConversationWorkspace, replacement: AssistantSessionState?) {
                check(!failWrite)
                memory.delete(id = id, workspace = workspace, replacement = replacement)
            }
        }
        val runtime = Runtime()
        val repo = AssistantConversationRepository(assistant = runtime, settings = conversationSettings(), store = store, scope = backgroundScope)
        runCurrent()
        assertEquals(ConversationStorageState.OPEN_FAILED, repo.state.value.storage)
        repo.retrySave()
        assertEquals(ConversationStorageState.OPEN_FAILED, repo.state.value.storage)
        failOpen = false
        repo.retrySave()
        repo.editDraft("keep draft")
        repo.flushPending()
        val id = repo.state.value.selected.id
        failWrite = true
        repo.rename(id = id, title = "uncommitted title")
        assertEquals("", repo.state.value.selected.title)
        failWrite = false
        repo.retrySave()
        failWrite = true
        assertFalse(repo.delete(id))
        assertEquals(id, repo.state.value.selected.id)
        assertNotNull(memory.metadata(id))
        failWrite = false
        repo.retrySave()
        assertTrue(repo.delete(id))
        assertNull(memory.metadata(id))
        assertEquals(0, runtime.calls)
    }

}
