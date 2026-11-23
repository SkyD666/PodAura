package com.skyd.podaura.nano.ui.screen.chat

import com.skyd.podaura.nano.model.repository.conversation.ConversationCredentials
import com.skyd.podaura.nano.core.AssistantConnection
import com.skyd.podaura.nano.model.bean.conversation.AssistantTurnStatus
import com.skyd.podaura.nano.model.repository.conversation.AssistantConversationRepository
import com.skyd.podaura.nano.model.repository.conversation.ConversationStore
import com.skyd.podaura.nano.model.repository.conversation.InMemoryConversationStore
import com.skyd.podaura.nano.model.bean.conversation.AssistantSessionState
import com.skyd.podaura.nano.core.AssistantError
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.ViewModelStore
import com.skyd.podaura.nano.core.AssistantAction
import com.skyd.podaura.nano.core.AssistantRuntime
import com.skyd.podaura.nano.core.AssistantConnectionResult
import com.skyd.podaura.nano.core.AssistantRunEvent
import com.skyd.podaura.nano.core.AssistantTurnRequest
import com.skyd.podaura.nano.settings.AssistantSettingsRepository
import com.skyd.fundation.credential.CredentialStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class NanoViewModelTest {
    @Test
    fun navigationSwitchAndViewModelRecreationKeepGenerationAndIsolateDrafts() = runBlocking(Dispatchers.Main.immediate) {
        withTimeout(5_000) {
            Fixture().use { f ->
                f.settings.save(baseUrl = "https://example.test/v1", modelId = "model", apiKey = "synthetic-key")
                val model = f.model
                model.viewState.first { it.ready }
                val a = model.viewState.value.session.id
                model.processIntent(NanoIntent.UpdateDraft(a, "p", "editor", 1))
                model.processIntent(NanoIntent.UpdateDraft(a, "private question", "editor", 2))
                model.processIntent(NanoIntent.Send(conversationId = a, editorId = "editor", revision = 2))
                f.started.await()
                assertEquals(NanoEvent.ClearDraft(a, "editor", 2), model.singleEvent.first())
                assertEquals("private question", f.lastQuestion)
                for ((intent, event) in listOf(NanoIntent.OpenSettings to NanoEvent.OpenSettings,
                    NanoIntent.OpenSearch to NanoEvent.OpenSearch, NanoIntent.Back to NanoEvent.Back)) {
                    model.processIntent(intent); assertEquals(event, model.singleEvent.first())
                    assertFalse(f.cancelled.isCompleted)
                }
                model.processIntent(NanoIntent.NewConversation)
                val b = model.viewState.first { it.session.id != a }.session.id
                model.processIntent(NanoIntent.UpdateDraft(b, "B draft", "editor-b", 1))
                model.processIntent(NanoIntent.UpdateDraft(a, "late A draft", "editor", 3))
                model.viewState.first { it.session.draft == "B draft" }
                model.processIntent(NanoIntent.OpenSearch)
                assertEquals(NanoEvent.OpenSearch, model.singleEvent.first())
                f.store.clear(); yield()
                assertFalse(f.cancelled.isCompleted)
                f.finish.complete(Unit)
                f.session.state.first { it.runningIds.isEmpty() }
                val reopened = NanoViewModel(f.session, f.settings)
                f.store.put("reopened", reopened)
                assertEquals("B draft", reopened.viewState.value.session.draft)
                reopened.processIntent(NanoIntent.Select(a))
                val restored = reopened.viewState.first { it.session.id == a }.session
                assertEquals("late A draft", restored.draft)
                assertEquals("partial complete", restored.turns.single().reply)
                assertEquals(1, f.requests)
            }
        }
    }

    @Test
    fun laterDraftSurvivesSendCompletionAndExplicitStopTargetsOriginalConversation() = runBlocking(Dispatchers.Main.immediate) {
        withTimeout(5_000) {
            Fixture().use { f ->
                f.settings.save(baseUrl = "https://example.test/v1", modelId = "model", apiKey = "synthetic-key")
                val model = f.model
                model.viewState.first { it.ready }
                val a = model.viewState.value.session.id
                model.processIntent(NanoIntent.UpdateDraft(a, "send this", "editor", 1))
                model.processIntent(NanoIntent.Send(conversationId = a, editorId = "editor", revision = 1))
                model.processIntent(NanoIntent.UpdateDraft(a, "next draft", "editor", 2))
                f.started.await()
                assertEquals(NanoEvent.ClearDraft(a, "editor", 1), model.singleEvent.first())
                model.viewState.first { it.session.draft == "next draft" }
                assertEquals("send this", f.lastQuestion)
                model.processIntent(NanoIntent.NewConversation)
                val b = model.viewState.first { it.session.id != a }.session.id
                model.processIntent(NanoIntent.Stop(a))
                f.cancelled.await()
                assertEquals(b, model.viewState.value.session.id)
                assertFalse(model.viewState.value.session.isRunning)
            }
        }
    }

    @Test
    fun settingsReadFailureDoesNotKillStopOrNavigation() = runBlocking(Dispatchers.Main.immediate) {
        withTimeout(5_000) {
            Fixture().use { f ->
                f.settings.save(baseUrl = "https://example.test/v1", modelId = "model", apiKey = "synthetic-key")
                val model = f.model
                model.viewState.first { it.ready }
                val a = model.viewState.value.session.id
                model.processIntent(NanoIntent.UpdateDraft(a, "question", "editor", 1))
                model.processIntent(NanoIntent.Send(conversationId = a, editorId = "editor", revision = 1))
                f.started.await(); assertIs<NanoEvent.ClearDraft>(model.singleEvent.first())
                f.failReads.value = true
                model.viewState.first { it.settingsFailed }
                model.processIntent(NanoIntent.Stop(a)); model.processIntent(NanoIntent.Back)
                assertIs<NanoEvent.Back>(model.singleEvent.first())
                f.cancelled.await()
                assertEquals(AssistantTurnStatus.STOPPED, f.session.state.value.selected.turns.single().status)
            }
        }
    }

    @Test
    fun failedStoreCommandReportsItsTargetAndDoesNotTerminateTheIntentPipeline() = runBlocking(Dispatchers.Main.immediate) {
        withTimeout(5_000) {
            Fixture().use { f ->
                f.settings.save(baseUrl = "https://example.test/v1", modelId = "model", apiKey = "key")
                val model = f.model
                model.viewState.first { it.ready }
                val id = model.viewState.value.session.id
                model.processIntent(NanoIntent.UpdateDraft(id, "kept", "editor", 1))
                model.viewState.first { it.session.draft == "kept" }
                f.failConversationWrites = true
                model.processIntent(NanoIntent.Send(conversationId = id, editorId = "editor", revision = 1))
                model.processIntent(NanoIntent.OpenSearch)
                val events = listOf(model.singleEvent.first(), model.singleEvent.first())
                val error = events.filterIsInstance<NanoEvent.Rejected>().single()
                assertEquals(id, error.conversationId)
                assertEquals(AssistantError.CONVERSATION_STORAGE, error.error)
                assertTrue(events.contains(NanoEvent.OpenSearch))
                f.failConversationWrites = false
                model.processIntent(NanoIntent.RetrySave)
                model.viewState.first { it.ready }
                model.processIntent(NanoIntent.Rename(id, "still working"))
                model.viewState.first { it.session.title == "still working" }
                assertEquals("kept", model.viewState.value.session.draft)
            }
        }
    }

    @Test
    fun loggedPayloadsDoNotContainQuestionTitleOrCapabilityAction() {
        val id = kotlin.uuid.Uuid.random()
        assertFalse(NanoIntent.UpdateDraft(id, "private question", "editor", 1).toString().contains("private question"))
        assertFalse(NanoIntent.Rename(id, "private title").toString().contains("private title"))
        assertFalse(NanoIntent.PerformAction(id, SampleAction("private-id")).toString().contains("private-id"))
        assertFalse(NanoEvent.Action(id, SampleAction("private-id")).toString().contains("private-id"))
    }

    private data class SampleAction(val target: String) : AssistantAction

    private class Fixture : AutoCloseable {
        val store = ViewModelStore()
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        var requests = 0
        var lastQuestion: String? = null
        val failReads = MutableStateFlow(false)
        private val stored = MutableStateFlow(emptyPreferences())
        private val preferences = object : DataStore<Preferences> {
            override val data = combine(stored, failReads) { values, fail ->
                check(!fail) { "Synthetic local read failure" }
                values
            }
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences) =
                transform(stored.value).also { stored.value = it }
        }
        val settings = AssistantSettingsRepository(preferences, ConversationCredentials())
        var failConversationWrites = false
        private val memory = InMemoryConversationStore()
        private val conversationStore = object : ConversationStore by memory {
            override suspend fun commit(metadata: AssistantSessionState?, turn: com.skyd.podaura.nano.model.bean.conversation.AssistantSessionTurn?, workspace: com.skyd.podaura.nano.model.bean.conversation.ConversationWorkspace?) {
                check(!failConversationWrites) { "synthetic store failure" }
                memory.commit(metadata = metadata, turn = turn, workspace = workspace)
            }
        }
        val session = AssistantConversationRepository(object : AssistantRuntime {
            override val available = true
            override suspend fun verify(connection: suspend () -> AssistantConnection) = AssistantConnectionResult(true)
            override fun run(request: AssistantTurnRequest, connection: com.skyd.podaura.nano.core.AssistantConnection) = flow<AssistantRunEvent> {
                requests++
                lastQuestion = request.question
                emit(AssistantRunEvent.ReplyDelta("partial"))
                started.complete(Unit)
                try {
                    finish.await()
                    emit(AssistantRunEvent.ReplyDelta(" complete"))
                    emit(AssistantRunEvent.Completed)
                } catch (cancelledRun: CancellationException) {
                    cancelled.complete(Unit)
                    throw cancelledRun
                }
            }
        }, settings, store = conversationStore, scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate))
        val model = NanoViewModel(session, settings).also { store.put("model", it) }
        override fun close() { store.clear(); session.close() }
    }
}
