package com.skyd.podaura.podnya.model.repository.conversation

import com.skyd.podaura.podnya.core.*
import com.skyd.podaura.podnya.model.bean.*
import com.skyd.podaura.podnya.model.bean.conversation.*
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
        override suspend fun verify() = AssistantConnectionResult(true)
    }
    private data class Content(val size: Int, val payload: String = "checked") : AssistantContent {
        override val retainedBytes = size
        override fun historySnapshot() = AssistantHistorySnapshot("test", "summary", payload)
    }

    @Test fun threeConcurrentRepliesSurviveSwitchAndNewAndRejectFourthWithoutQueueing() = runTest {
        val finish = Channel<Unit>(Channel.UNLIMITED)
        val runtime = Runtime { _, _ -> emit(AssistantRunEvent.ReplyDelta("partial")); finish.receive(); emit(AssistantRunEvent.Completed) }
        val settings = conversationSettings()
        val store = InMemoryConversationStore()
        val repo = AssistantConversationRepository(runtime, settings, store, backgroundScope)
        val ids = (1..3).map {
            val id = repo.newConversation()
            repo.editDraft("question $it"); assertTrue(repo.sendDraft(settings)); runCurrent(); id
        }
        assertEquals(3, repo.state.value.runningIds.size)
        val fourth = repo.newConversation()
        repo.editDraft("keep fourth")
        assertFalse(repo.sendDraft(settings))
        assertEquals("keep fourth", repo.state.value.selected.draft)
        assertEquals(3, runtime.requests.size)
        repo.select(ids.first())
        assertEquals("partial", repo.state.value.selected.turns.single().reply)
        finish.send(Unit); runCurrent()
        assertEquals(2, repo.state.value.runningIds.size)
        repo.select(fourth)
        assertTrue(repo.sendDraft(settings)); runCurrent()
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
        val config = settings.snapshot()
        blockReads = true
        val ids = (1..3).map {
            val id = repo.newConversation(); repo.editDraft("q $it")
            val c = repo.state.value.selected
            assertTrue(repo.submit(id, c.editorId, c.draftRevision, config.configId, config.revision))
            runCurrent(); id
        }
        assertEquals(3, repo.state.value.runningIds.size)
        assertFalse(repo.delete(ids.first()))
        assertTrue(repo.delete(ids.first(), true))
        assertEquals(3, repo.state.value.runningIds.size)
        assertNull(store.get(ids.first()))
        val next = repo.newConversation(); repo.editDraft("next")
        val c = repo.state.value.selected
        assertFalse(repo.submit(next, c.editorId, c.draftRevision, config.configId, config.revision))
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
        repo.editDraft("A"); repo.sendDraft(settings); runCurrent()
        val a = repo.state.value.selected.id
        repo.stop(a)
        assertEquals(AssistantTurnStatus.STOPPED, repo.state.value.selected.turns.single().status)
        assertFalse(repo.retryLast(settings))
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
        repo.editDraft("retry me"); repo.sendDraft(settings); runCurrent()
        val id = repo.state.value.selected.turns.single().id
        assertTrue(repo.retryLast(settings)); runCurrent()
        assertEquals(id, repo.state.value.selected.turns.single().id)
        assertEquals("fixed", repo.state.value.selected.turns.single().reply)
        repo.editDraft("interrupt"); repo.sendDraft(settings); runCurrent()
        assertEquals(AssistantError.STREAM_INTERRUPTED, repo.state.value.selected.turns.last().error)
        repo.editDraft("oversized"); repo.sendDraft(settings); runCurrent()
        assertEquals(AssistantError.BUDGET_EXHAUSTED, repo.state.value.selected.turns.last().error)
        assertNull(repo.state.value.selected.turns.last().content)
    }

    @Test fun visibleHistoryIsNeverTrimmedWhileModelHistoryKeepsSixTurnsAndByteBudget() = runTest {
        val runtime = Runtime { _, _ -> emit(AssistantRunEvent.ContentReady(Content(30_000, "喵".repeat(10_000)))); emit(AssistantRunEvent.Completed) }
        val settings = conversationSettings(); val repo = AssistantConversationRepository(runtime, settings, scope = backgroundScope)
        repeat(60) { repo.editDraft("question $it"); assertTrue(repo.sendDraft(settings)); runCurrent() }
        assertEquals(40, repo.state.value.selected.turns.size)
        repo.loadWindow(id = repo.state.value.selected.id, anchor = 1)
        assertEquals("question 0", repo.state.value.selected.turns.first().question)
        assertEquals(listOf("question 57", "question 58"), runtime.requests.last().history.map { it.question })
        repo.editDraft("from an old reading window")
        assertTrue(repo.sendDraft(settings))
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
            assertTrue(repo.sendDraft(settings))
            runCurrent()
        }
        assertEquals(25, store.tail(id = repo.state.value.selected.id, limit = 100).size)
    }

    @Test fun titlesDraftsModelReferencesAndEmptyEditorReuseStayIsolated() = runTest {
        val runtime = Runtime { _, _ -> emit(AssistantRunEvent.Completed) }
        val settings = conversationSettings(); val store = InMemoryConversationStore()
        val repo = AssistantConversationRepository(runtime, settings, store, backgroundScope)
        val a = repo.newConversation(); assertEquals(a, repo.newConversation())
        repo.editDraft("first title"); repo.sendDraft(settings); runCurrent()
        assertEquals("first title", repo.state.value.selected.title)
        repo.rename(a, "manual"); repo.editDraft("saved A")
        val b = repo.newConversation(); repo.editDraft("saved B")
        repo.updateDraft(a, "late A", "test-editor", 99)
        assertEquals("saved B", repo.state.value.selected.draft)
        repo.select(a); assertEquals("manual", repo.state.value.selected.title)
        assertEquals("late A", repo.state.value.selected.draft)
        repo.sendDraft(settings); runCurrent(); assertEquals("manual", repo.state.value.selected.title)
        repo.select(b); assertEquals("saved B", repo.state.value.selected.draft)
        assertEquals(settings.snapshot().configId, repo.state.value.selected.modelConfigId)
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
        repo.editDraft("A"); repo.sendDraft(settings); runCurrent(); val a = repo.state.value.selected.id
        settings.save("https://second.example/prefix", "second-model", "second-key", false, true)
        repo.newConversation(); repo.editDraft("B"); repo.sendDraft(settings); runCurrent(); val b = repo.state.value.selected.id
        repo.editDraft("B draft")
        settings.clear(); runCurrent()
        assertEquals(listOf("https://first.example/v1/", "https://second.example/prefix/"), runtime.connections.map { it.baseUrl })
        assertEquals(listOf("first-model", "second-model"), runtime.connections.map { it.modelId })
        assertEquals(listOf("test-key", "second-key"), runtime.connections.map { it.apiKey })
        assertEquals(2, repo.state.value.runningIds.size)
        gate.send(Unit); gate.send(Unit); runCurrent()
        assertEquals("B draft", repo.state.value.selected.draft)
        assertEquals("first-model", store.get(a)!!.turns.single().source!!.modelId)
        assertEquals("second-model", store.get(b)!!.turns.single().source!!.modelId)
        assertFalse(repo.state.value.toString().contains("second-key"))
        settings.save("https://third.example", "new", "new-key", false, true)
        assertFalse(repo.sendDraft(settings)) // deleted config IDs must not silently rebind
        repo.selectModel(b, settings.snapshot().configId!!)
        assertEquals(settings.snapshot().configId, repo.state.value.selected.modelConfigId)
        assertEquals(1, repo.state.value.selected.turns.size)
    }

    @Test fun destinationConfirmationBindsConversationInputConfigRevisionAndFullServiceAddress() = runTest {
        val settings = conversationSettings(); val runtime = Runtime { _, _ -> emit(AssistantRunEvent.Completed) }
        val repo = AssistantConversationRepository(runtime, settings, scope = backgroundScope)
        val notices = mutableListOf<AssistantConversationRepository.Notice>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repo.notices.toList(notices) }
        repo.editDraft("old history"); repo.sendDraft(settings); runCurrent()
        val a = repo.state.value.selected.id
        settings.save("https://first.example/other", "new-model", "key", false, true)
        repo.editDraft("continue"); repo.sendDraft(settings); runCurrent()
        val ticket = (notices.last() as AssistantConversationRepository.Notice.ConfirmDestination).confirmation
        assertTrue(repo.state.value.runningIds.isEmpty()); assertEquals("continue", repo.state.value.selected.draft)
        repo.editDraft("changed")
        assertFalse(repo.submit(a, ticket.editorId, ticket.revision, ticket.configId, ticket.configRevision, confirmation = ticket))
        repo.sendDraft(settings); runCurrent()
        val fresh = (notices.last() as AssistantConversationRepository.Notice.ConfirmDestination).confirmation
        repo.newConversation(); repo.editDraft("B draft")
        assertTrue(repo.submit(a, fresh.editorId, fresh.revision, fresh.configId, fresh.configRevision, confirmation = fresh)); runCurrent()
        assertEquals("B draft", repo.state.value.selected.draft)
        assertEquals(2, runtime.requests.size)
        repo.select(a)
        settings.save("https://first.example/other/", "another-model", "rotated", false, true)
        repo.editDraft("same destination"); repo.sendDraft(settings); runCurrent()
        assertEquals(3, runtime.requests.size)
        val staleConfig = settings.snapshot()
        settings.save("https://first.example/third", "another-model", "third", false, true)
        repo.editDraft("stale config")
        val current = repo.state.value.selected
        repo.submit(a, current.editorId, current.draftRevision, staleConfig.configId, staleConfig.revision); runCurrent()
        assertEquals(3, runtime.requests.size)
        assertEquals(AssistantError.CONFIGURATION_CHANGED, (notices.last() as AssistantConversationRepository.Notice.Rejected).error)
        assertEquals("stale config", repo.state.value.selected.draft)
    }

    @Test fun editingWhileConfirmedConnectionIsResolvingInvalidatesTheConfirmation() = runTest {
        val release = CompletableDeferred<Unit>()
        var blocked = false
        val credentials = object : ConversationCredentials() {
            override suspend fun get(id: String): String? {
                if (blocked) release.await()
                return super.get(id)
            }
        }
        val settings = conversationSettings(credentials)
        val runtime = Runtime { _, _ -> emit(AssistantRunEvent.Completed) }
        val repo = AssistantConversationRepository(runtime, settings, scope = backgroundScope)
        val notices = mutableListOf<AssistantConversationRepository.Notice>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repo.notices.toList(notices) }
        repo.editDraft("history"); repo.sendDraft(settings); runCurrent()
        settings.save("https://other.example", "model", "key", false, true)
        repo.editDraft("confirmed input"); repo.sendDraft(settings); runCurrent()
        val ticket = (notices.last() as AssistantConversationRepository.Notice.ConfirmDestination).confirmation
        blocked = true
        repo.submit(ticket.conversationId, ticket.editorId, ticket.revision, ticket.configId, ticket.configRevision, confirmation = ticket)
        runCurrent()
        repo.editDraft("new input during resolution")
        release.complete(Unit); runCurrent()
        assertEquals(1, runtime.requests.size)
        assertEquals("new input during resolution", repo.state.value.selected.draft)
        assertEquals(AssistantError.STALE_INPUT, (notices.last() as AssistantConversationRepository.Notice.Rejected).error)
        assertTrue(repo.state.value.runningIds.isEmpty())
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
        assertTrue(repo.sendDraft(settings))
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
        assertTrue(repo.sendDraft(settings))
        runCurrent()
        assertTrue(runtime.requests.isEmpty())
        assertEquals("kept", repo.state.value.selected.draft)
        assertTrue(repo.state.value.runningIds.isEmpty())
        assertFalse(repo.sendDraft(settings))
        repo.retrySave()
        assertTrue(runtime.requests.isEmpty())
        assertTrue(repo.sendDraft(settings))
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
            repo.editDraft("question"); repo.sendDraft(settings); runCurrent()
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
