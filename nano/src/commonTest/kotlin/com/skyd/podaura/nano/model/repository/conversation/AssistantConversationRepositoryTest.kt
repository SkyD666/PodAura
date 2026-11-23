package com.skyd.podaura.nano.model.repository.conversation

import com.skyd.podaura.nano.core.*
import com.skyd.podaura.nano.model.bean.*
import com.skyd.podaura.nano.model.bean.conversation.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.Json
import kotlin.test.*

class AssistantConversationRepositoryTest {
    private class Runtime(val block: suspend FlowCollector<AssistantRunEvent>.(AssistantTurnRequest, AssistantConnection) -> Unit) : AssistantRuntime {
        override val available = true
        val requests = mutableListOf<AssistantTurnRequest>()
        val connections = mutableListOf<AssistantConnection>()
        override fun run(request: AssistantTurnRequest, connection: AssistantConnection) = flow {
            requests += request; connections += connection; block(request, connection)
        }
        override suspend fun verify(connection: suspend () -> AssistantConnection) = AssistantConnectionResult(true)
    }
    private data class Content(val size: Int, val payload: String = "checked") : AssistantContent {
        override val retainedBytes = size
        override fun historySnapshot() = AssistantHistorySnapshot("test", "summary", payload)
    }

    @Test
    fun effortIsFixedForRunningRepliesAndRetryRecordsTheCurrentSelection() = runTest {
        val release = Channel<Unit>(Channel.UNLIMITED)
        val runtime = Runtime { _, _ ->
            release.receive()
            emit(AssistantRunEvent.Failed(AssistantError.NETWORK))
        }
        val settings = conversationSettings()
        suspend fun saveEffort(effort: AssistantReasoningEffort) {
            settings.save(
                baseUrl = "https://api.openai.com/v1/",
                modelId = "gpt-6.1-terra",
                apiKey = "test-key",
                editing = settings.snapshot(),
                reasoningEffort = effort,
            )
        }
        saveEffort(AssistantReasoningEffort.MEDIUM)
        val repo = AssistantConversationRepository(assistant = runtime, settings = settings, scope = backgroundScope)
        repo.editDraft("Question")
        assertTrue(repo.sendDraft())
        runCurrent()
        val turnId = repo.state.value.selected.turns.single().id
        saveEffort(AssistantReasoningEffort.MAX)
        assertEquals(AssistantReasoningEffort.MEDIUM, runtime.connections.single().reasoningEffort)
        assertEquals("medium", repo.state.value.selected.turns.single().source!!.reasoningEffort)
        release.send(Unit)
        runCurrent()
        assertTrue(repo.retryLast())
        runCurrent()
        assertEquals(turnId, repo.state.value.selected.turns.single().id)
        assertEquals(AssistantReasoningEffort.MAX, runtime.connections.last().reasoningEffort)
        assertEquals("max", repo.state.value.selected.turns.single().source!!.reasoningEffort)
        release.send(Unit)
        runCurrent()
        repo.editDraft("Next question")
        assertTrue(repo.sendDraft())
        runCurrent()
        assertEquals("max", repo.state.value.selected.turns.last().source!!.reasoningEffort)
    }

    @Test fun threeConcurrentRepliesSurviveSwitchAndNewAndRejectFourthWithoutQueueing() = runTest {
        val finish = Channel<Unit>(Channel.UNLIMITED)
        val runtime = Runtime { _, _ -> emit(AssistantRunEvent.ReplyDelta("partial")); finish.receive(); emit(AssistantRunEvent.Completed) }
        val settings = conversationSettings()
        val store = InMemoryConversationStore()
        val repo = AssistantConversationRepository(runtime, settings, store, backgroundScope)
        val ids = (1..3).map {
            val id = repo.newConversation()
            repo.editDraft("question $it")
            assertTrue(repo.sendDraft())
            runCurrent()
            id
        }
        assertEquals(3, repo.state.value.runningIds.size)
        val fourth = repo.newConversation()
        repo.editDraft("keep fourth")
        assertFalse(repo.sendDraft())
        assertEquals("keep fourth", repo.state.value.selected.draft)
        assertEquals(3, runtime.requests.size)
        repo.select(ids.first())
        assertEquals("partial", repo.state.value.selected.turns.single().reply)
        finish.send(Unit); runCurrent()
        assertEquals(2, repo.state.value.runningIds.size)
        repo.select(fourth)
        assertTrue(repo.sendDraft())
        runCurrent()
        assertEquals(4, runtime.requests.size)
        assertEquals(3, repo.state.value.runningIds.size)
        assertTrue(ids.all { store.get(it)?.turns?.size == 1 })
    }

    @Test fun preparingCountsAndCancellationHoldsSlotUntilCleanupAndNeverResurrectsDeletedConversation() = runTest {
        val releaseRead = CompletableDeferred<Unit>()
        var blockReads = false
        val credentials = object : ConversationCredentials() {
            override suspend fun get(id: String): String? {
                if (blockReads) withContext(NonCancellable) { releaseRead.await() }
                return super.get(id)
            }
        }
        val settings = conversationSettings(credentials)
        val runtime = Runtime { _, _ -> emit(AssistantRunEvent.Completed) }
        val store = InMemoryConversationStore()
        val repo = AssistantConversationRepository(runtime, settings, store, backgroundScope)
        blockReads = true
        val ids = (1..3).map {
            val id = repo.newConversation(); repo.editDraft("q $it")
            val c = repo.state.value.selected
            assertTrue(repo.submit(id = id, editorId = c.editorId, revision = c.draftRevision))
            runCurrent(); id
        }
        assertEquals(3, repo.state.value.runningIds.size)
        assertFalse(repo.delete(ids.first()))
        assertTrue(repo.delete(ids.first(), true))
        assertEquals(3, repo.state.value.runningIds.size)
        assertNull(store.get(ids.first()))
        val next = repo.newConversation(); repo.editDraft("next")
        val c = repo.state.value.selected
        assertFalse(repo.submit(id = next, editorId = c.editorId, revision = c.draftRevision))
        releaseRead.complete(Unit); runCurrent()
        assertNull(store.get(ids.first()))
        assertEquals(2, runtime.requests.size)
        assertTrue(repo.state.value.runningIds.isEmpty())
    }

    @Test fun deletionDropsLateEventsAndStopKeepsPartialReplyAndRetryUsesSameTurn() = runTest {
        val cleanup = CompletableDeferred<Unit>()
        val runtime = Runtime { _, _ ->
            emit(AssistantRunEvent.ReplyDelta("partial"))
            try { awaitCancellation() } finally { withContext(NonCancellable) { cleanup.await() } }
        }
        val settings = conversationSettings(); val store = InMemoryConversationStore()
        val repo = AssistantConversationRepository(runtime, settings, store, backgroundScope)
        repo.editDraft("A")
        repo.sendDraft()
        runCurrent()
        val a = repo.state.value.selected.id
        repo.stop(a)
        assertEquals(AssistantTurnStatus.STOPPED, repo.state.value.selected.turns.single().status)
        assertFalse(repo.retryLast())
        assertEquals("partial", repo.state.value.selected.turns.single().reply)
        repo.delete(a, true)
        repo.editDraft("B")
        cleanup.complete(Unit); runCurrent()
        assertNull(store.get(a))
        assertEquals("B", repo.state.value.selected.draft)
        assertTrue(repo.state.value.runningIds.isEmpty())
    }

    @Test fun failuresRetryInPlaceAndUnterminatedFlowsFailAndOversizedCardsAreRejected() = runTest {
        var attempts = 0
        val runtime = Runtime { _, _ -> when (attempts++) {
            0 -> { emit(AssistantRunEvent.ReplyDelta("partial")); emit(AssistantRunEvent.Failed(AssistantError.NETWORK)) }
            1 -> { emit(AssistantRunEvent.ReplyDelta("fixed")); emit(AssistantRunEvent.Completed) }
            2 -> emit(AssistantRunEvent.ReplyDelta("interrupted"))
            else -> emit(AssistantRunEvent.ContentReady(Content(AssistantLimits.CONTENT_BYTES + 1)))
        } }
        val settings = conversationSettings(); val repo = AssistantConversationRepository(runtime, settings, scope = backgroundScope)
        repo.editDraft("retry me")
        repo.sendDraft()
        runCurrent()
        val id = repo.state.value.selected.turns.single().id
        assertTrue(repo.retryLast())
        runCurrent()
        assertEquals(id, repo.state.value.selected.turns.single().id)
        assertEquals("fixed", repo.state.value.selected.turns.single().reply)
        repo.editDraft("interrupt")
        repo.sendDraft()
        runCurrent()
        assertEquals(AssistantError.STREAM_INTERRUPTED, repo.state.value.selected.turns.last().error)
        repo.editDraft("oversized")
        repo.sendDraft()
        runCurrent()
        assertEquals(AssistantError.BUDGET_EXHAUSTED, repo.state.value.selected.turns.last().error)
        assertNull(repo.state.value.selected.turns.last().content)
    }

    @Test fun visibleHistoryIsNeverTrimmedWhileModelHistoryKeepsSixTurnsAndByteBudget() = runTest {
        val runtime = Runtime { _, _ -> emit(AssistantRunEvent.ContentReady(Content(30_000, "喵".repeat(10_000)))); emit(AssistantRunEvent.Completed) }
        val settings = conversationSettings(); val repo = AssistantConversationRepository(runtime, settings, scope = backgroundScope)
        repeat(60) {
            repo.editDraft("question $it")
            assertTrue(repo.sendDraft())
            runCurrent()
        }
        assertEquals(40, repo.state.value.selected.turns.size)
        repo.loadWindow(id = repo.state.value.selected.id, anchor = 1)
        assertEquals("question 0", repo.state.value.selected.turns.first().question)
        assertEquals(listOf("question 57", "question 58"), runtime.requests.last().history.map { it.question })
        repo.editDraft("from an old reading window")
        assertTrue(repo.sendDraft())
        runCurrent()
        assertEquals(listOf("question 58", "question 59"), runtime.requests.last().history.map { it.question })
        assertTrue(Json.encodeToString(runtime.requests.last().history).encodeToByteArray().size < AssistantLimits.PROMPT_BYTES)
    }

    @Test fun historyCanExceedTheFormerByteBudgetWithoutEviction() = runTest {
        val runtime = Runtime { _, _ ->
            emit(AssistantRunEvent.ContentReady(Content(AssistantLimits.CONTENT_BYTES)))
            emit(AssistantRunEvent.Completed)
        }
        val settings = conversationSettings()
        val store = InMemoryConversationStore()
        val repo = AssistantConversationRepository(assistant = runtime, settings = settings, store = store, scope = backgroundScope)
        repeat(25) {
            repo.editDraft("question $it")
            assertTrue(repo.sendDraft())
            runCurrent()
        }
        assertEquals(25, store.tail(id = repo.state.value.selected.id, limit = 100).size)
    }

    @Test fun titlesDraftsAndEmptyEditorReuseStayIsolated() = runTest {
        val runtime = Runtime { _, _ -> emit(AssistantRunEvent.Completed) }
        val settings = conversationSettings(); val store = InMemoryConversationStore()
        val repo = AssistantConversationRepository(runtime, settings, store, backgroundScope)
        val a = repo.newConversation(); assertEquals(a, repo.newConversation())
        repo.editDraft("first title")
        repo.sendDraft()
        runCurrent()
        assertEquals("first title", repo.state.value.selected.title)
        repo.rename(a, "manual"); repo.editDraft("saved A")
        val b = repo.newConversation(); repo.editDraft("saved B")
        repo.updateDraft(a, "late A", "test-editor", 99)
        assertEquals("saved B", repo.state.value.selected.draft)
        repo.select(a); assertEquals("manual", repo.state.value.selected.title)
        assertEquals("late A", repo.state.value.selected.draft)
        repo.sendDraft()
        runCurrent()
        assertEquals("manual", repo.state.value.selected.title)
        repo.select(b); assertEquals("saved B", repo.state.value.selected.draft)
        assertNull(repo.state.value.selected.modelConfigId)
        repo.delete(a); assertFalse(repo.updateDraft(a, "resurrection", "old", 100))
        assertNull(store.get(a))
    }

    @Test fun summariesArePagedBeyondTheFormerCountLimitAndReuseOneBlank() = runTest {
        val settings = conversationSettings()
        val store = InMemoryConversationStore()
        val repo = AssistantConversationRepository(assistant = Runtime { _, _ -> }, settings = settings, store = store, scope = backgroundScope)
        repeat(120) {
            repo.newConversation()
            repo.editDraft("draft $it")
        }
        val blank = repo.newConversation()
        assertEquals(blank, repo.newConversation())
        assertEquals(120, store.summaries(offset = 0, limit = 200).size)
        assertTrue(repo.state.value.summaries.size <= 50)
        repo.loadSummaries(100)
        assertEquals(20, repo.state.value.summaries.size)
        assertFalse(repo.state.value.hasMoreSummaries)
    }

    @Test fun connectionSnapshotsAreIndependentAndConfigurationClearDoesNotEraseHistoryOrCancelRuns() = runTest {
        val gate = Channel<Unit>(Channel.UNLIMITED)
        val runtime = Runtime { _, _ -> gate.receive(); emit(AssistantRunEvent.Completed) }
        val settings = conversationSettings(); val store = InMemoryConversationStore()
        val repo = AssistantConversationRepository(runtime, settings, store, backgroundScope)
        repo.editDraft("A")
        repo.sendDraft()
        runCurrent()
        val a = repo.state.value.selected.id
        settings.save(baseUrl = "https://second.example/prefix", modelId = "second-model", apiKey = "second-key", editing = settings.snapshot())
        repo.newConversation()
        repo.editDraft("B")
        repo.sendDraft()
        runCurrent()
        val b = repo.state.value.selected.id
        repo.editDraft("B draft")
        settings.delete(id = settings.snapshot().configId!!, replacementId = null)
        runCurrent()
        assertEquals(listOf("https://first.example/v1/", "https://second.example/prefix/"), runtime.connections.map { it.baseUrl })
        assertEquals(listOf("first-model", "second-model"), runtime.connections.map { it.modelId })
        assertEquals(listOf("test-key", "second-key"), runtime.connections.map { it.apiKey })
        assertEquals(2, repo.state.value.runningIds.size)
        gate.send(Unit); gate.send(Unit); runCurrent()
        assertEquals("B draft", repo.state.value.selected.draft)
        assertEquals("first-model", store.get(a)!!.turns.single().source!!.modelId)
        assertEquals("second-model", store.get(b)!!.turns.single().source!!.modelId)
        assertFalse(repo.state.value.toString().contains("second-key"))
        settings.save(baseUrl = "https://third.example", modelId = "new", apiKey = "new-key")
        assertTrue(repo.sendDraft())
        runCurrent()
        assertEquals("new", runtime.connections.last().modelId)
        assertEquals("second-model", store.get(b)!!.turns.first().source!!.profileName)
    }

    @Test
    fun retryUsesTheActiveProfileAndKeepsTheOriginalQuestion() = runTest {
        val settings = conversationSettings()
        val runtime = Runtime { _, _ -> emit(AssistantRunEvent.Failed(AssistantError.NETWORK)) }
        val repo = AssistantConversationRepository(assistant = runtime, settings = settings, scope = backgroundScope)
        repo.editDraft("original question")
        repo.sendDraft()
        runCurrent()
        settings.save(
            baseUrl = "https://second.example",
            modelId = "backup-model",
            apiKey = "backup-key",
            name = "Backup",
            activate = true,
        )
        repo.retryLast()
        runCurrent()
        val turn = repo.state.value.selected.turns.single()
        assertEquals("original question", turn.question)
        assertEquals("Backup", turn.source?.profileName)
        assertEquals("backup-key", runtime.connections.last().apiKey)
    }

    @Test fun draftInputLimitRetainsThePreviousDraft() = runTest {
        val settings = conversationSettings()
        val repo = AssistantConversationRepository(assistant = Runtime { _, _ -> }, settings = settings, scope = backgroundScope)
        repo.editDraft("kept")
        val current = repo.state.value.selected
        assertFalse(repo.updateDraft(id = current.id, text = "x".repeat(AssistantLimits.INPUT_CHARS + 1), editorId = current.editorId, revision = current.draftRevision + 1))
        assertEquals("kept", repo.state.value.selected.draft)
    }

    @Test fun preparingConversationIsNotReusedWhenDraftIsCleared() = runTest {
        val release = CompletableDeferred<Unit>()
        var block = false
        val credentials = object : ConversationCredentials() {
            override suspend fun get(id: String): String? {
                if (block) release.await()
                return super.get(id)
            }
        }
        val settings = conversationSettings(credentials)
        val repo = AssistantConversationRepository(assistant = Runtime { _, _ -> emit(AssistantRunEvent.Completed) }, settings = settings, scope = backgroundScope)
        repo.editDraft("pending")
        val id = repo.state.value.selected.id
        block = true
        assertTrue(repo.sendDraft())
        runCurrent()
        repo.editDraft("")
        assertNotEquals(id, repo.newConversation())
        release.complete(Unit)
        runCurrent()
        repo.select(id)
        assertEquals("pending", repo.state.value.selected.turns.single().question)
    }

    @Test fun admissionFailurePreservesDraftAndBlocksSendsUntilSaveRetry() = runTest {
        val memory = InMemoryConversationStore()
        var failNext = false
        val store = object : ConversationStore by memory {
            override suspend fun commit(metadata: AssistantSessionState?, turn: AssistantSessionTurn?, workspace: ConversationWorkspace?) {
                if (failNext) {
                    failNext = false
                    error("synthetic write failure")
                }
                memory.commit(metadata = metadata, turn = turn, workspace = workspace)
            }
        }
        val runtime = Runtime { _, _ -> emit(AssistantRunEvent.Completed) }
        val settings = conversationSettings()
        val repo = AssistantConversationRepository(assistant = runtime, settings = settings, store = store, scope = backgroundScope)
        repo.editDraft("kept")
        failNext = true
        assertTrue(repo.sendDraft())
        runCurrent()
        assertTrue(runtime.requests.isEmpty())
        assertEquals("kept", repo.state.value.selected.draft)
        assertTrue(repo.state.value.runningIds.isEmpty())
        assertFalse(repo.sendDraft())
        repo.retrySave()
        assertTrue(runtime.requests.isEmpty())
        assertTrue(repo.sendDraft())
        runCurrent()
        assertEquals(1, runtime.requests.size)
    }

    @Test fun continuousStoreFailuresCannotPreventStopOrFinallyFromReleasingResources() = runTest {
        for (stop in listOf(false, true)) {
            var failing = false
            val memory = InMemoryConversationStore()
            val store = object : ConversationStore by memory {
                override suspend fun commit(metadata: AssistantSessionState?, turn: AssistantSessionTurn?, workspace: ConversationWorkspace?) {
                    check(!failing) { "synthetic persistent write failure" }
                    memory.commit(metadata = metadata, turn = turn, workspace = workspace)
                }
            }
            val finish = CompletableDeferred<Unit>()
            val cancelled = CompletableDeferred<Unit>()
            val settings = conversationSettings()
            val repo = AssistantConversationRepository(Runtime { _, _ ->
                emit(AssistantRunEvent.ReplyDelta("saved partial"))
                try { finish.await(); emit(AssistantRunEvent.Completed) }
                finally { cancelled.complete(Unit) }
            }, settings, store, backgroundScope)
            val notices = mutableListOf<AssistantConversationRepository.Notice>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repo.notices.toList(notices) }
            repo.editDraft("question")
            repo.sendDraft()
            runCurrent()
            val id = repo.state.value.selected.id
            failing = true
            if (stop) repo.stop(id) else finish.complete(Unit)
            runCurrent()
            assertTrue(cancelled.isCompleted)
            assertTrue(repo.state.value.runningIds.isEmpty())
            assertEquals("saved partial", repo.state.value.selected.turns.single().reply)
            assertEquals(ConversationStorageState.SAVE_FAILED, repo.state.value.storage)
            failing = false
            repo.retrySave()
            assertEquals("saved partial", memory.get(id)!!.turns.single().reply)
            assertTrue(notices.filterIsInstance<AssistantConversationRepository.Notice.Rejected>()
                .any { it.error == AssistantError.CONVERSATION_STORAGE })
        }
    }

    @Test fun storeSummaryAndTurnCommitAndDeleteTogether() = runTest {
        val store = InMemoryConversationStore()
        val turn = AssistantSessionTurn(id = 1, question = "question")
        val metadata = AssistantSessionState(title = "title", lastTurnId = 1, lastStatus = turn.status)
        store.commit(metadata = metadata, turn = turn)
        assertEquals("title", store.summaries(offset = 0, limit = 10).single().title)
        assertEquals(AssistantTurnStatus.RUNNING, store.summaries(offset = 0, limit = 10).single().status)
        assertEquals(turn, store.tail(id = metadata.id, limit = 1).single())
        store.delete(id = metadata.id, workspace = ConversationWorkspace())
        assertNull(store.metadata(metadata.id))
        assertTrue(store.tail(id = metadata.id, limit = 1).isEmpty())
    }


}
