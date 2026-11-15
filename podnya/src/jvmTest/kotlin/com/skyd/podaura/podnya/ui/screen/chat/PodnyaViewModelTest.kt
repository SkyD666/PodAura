package com.skyd.podaura.podnya.ui.screen.chat

import com.skyd.podaura.podnya.model.bean.conversation.AssistantTurnStatus
import com.skyd.podaura.podnya.model.repository.conversation.AssistantConversationRepository
import com.skyd.podaura.podnya.model.repository.conversation.ConversationStore
import com.skyd.podaura.podnya.model.repository.conversation.InMemoryConversationStore
import com.skyd.podaura.podnya.model.bean.conversation.AssistantSessionState
import com.skyd.podaura.podnya.core.AssistantError
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.ViewModelStore
import com.skyd.podaura.podnya.core.AssistantAction
import com.skyd.podaura.podnya.core.AssistantRuntime
import com.skyd.podaura.podnya.core.AssistantConnectionResult
import com.skyd.podaura.podnya.core.AssistantRunEvent
import com.skyd.podaura.podnya.core.AssistantTurnRequest
import com.skyd.podaura.podnya.settings.AssistantSettingsRepository
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

class PodnyaViewModelTest {
    @Test
    fun navigationSwitchAndViewModelRecreationKeepGenerationAndIsolateDrafts() = runBlocking(Dispatchers.Main.immediate) {
        withTimeout(5_000) {
            Fixture().use { f ->
                f.settings.save("https://example.test/v1", "model", "synthetic-key", true, true)
                val model = f.model
                model.viewState.first { it.ready }
                val a = model.viewState.value.session.id
                val config = f.settings.snapshot()
                model.processIntent(PodnyaIntent.UpdateDraft(a, "p", "editor", 1))
                model.processIntent(PodnyaIntent.UpdateDraft(a, "private question", "editor", 2))
                model.processIntent(PodnyaIntent.Send(a, "editor", 2, config.configId, config.revision))
                f.started.await()
                assertEquals(PodnyaEvent.ClearDraft(a, "editor", 2), model.singleEvent.first())
                assertEquals("private question", f.lastQuestion)
                for ((intent, event) in listOf(PodnyaIntent.OpenSettings to PodnyaEvent.OpenSettings,
                    PodnyaIntent.OpenSearch to PodnyaEvent.OpenSearch, PodnyaIntent.Back to PodnyaEvent.Back)) {
                    model.processIntent(intent); assertEquals(event, model.singleEvent.first())
                    assertFalse(f.cancelled.isCompleted)
                }
                model.processIntent(PodnyaIntent.NewConversation)
                val b = model.viewState.first { it.session.id != a }.session.id
                model.processIntent(PodnyaIntent.UpdateDraft(b, "B draft", "editor-b", 1))
                model.processIntent(PodnyaIntent.UpdateDraft(a, "late A draft", "editor", 3))
                model.viewState.first { it.session.draft == "B draft" }
                model.processIntent(PodnyaIntent.OpenSearch)
                assertEquals(PodnyaEvent.OpenSearch, model.singleEvent.first())
                f.store.clear(); yield()
                assertFalse(f.cancelled.isCompleted)
                f.finish.complete(Unit)
                f.session.state.first { it.runningIds.isEmpty() }
                val reopened = PodnyaViewModel(f.session, f.settings)
                f.store.put("reopened", reopened)
                assertEquals("B draft", reopened.viewState.value.session.draft)
                reopened.processIntent(PodnyaIntent.Select(a))
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
                f.settings.save("https://example.test/v1", "model", "synthetic-key", true, true)
                val model = f.model
                model.viewState.first { it.ready }
                val a = model.viewState.value.session.id
                val config = f.settings.snapshot()
                model.processIntent(PodnyaIntent.UpdateDraft(a, "send this", "editor", 1))
                model.processIntent(PodnyaIntent.Send(a, "editor", 1, config.configId, config.revision))
                model.processIntent(PodnyaIntent.UpdateDraft(a, "next draft", "editor", 2))
                f.started.await()
                assertEquals(PodnyaEvent.ClearDraft(a, "editor", 1), model.singleEvent.first())
                model.viewState.first { it.session.draft == "next draft" }
                assertEquals("send this", f.lastQuestion)
                model.processIntent(PodnyaIntent.NewConversation)
                val b = model.viewState.first { it.session.id != a }.session.id
                model.processIntent(PodnyaIntent.Stop(a))
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
                f.settings.save("https://example.test/v1", "model", "synthetic-key", true, true)
                val model = f.model
                model.viewState.first { it.ready }
                val a = model.viewState.value.session.id; val config = f.settings.snapshot()
                model.processIntent(PodnyaIntent.UpdateDraft(a, "question", "editor", 1))
                model.processIntent(PodnyaIntent.Send(a, "editor", 1, config.configId, config.revision))
                f.started.await(); assertIs<PodnyaEvent.ClearDraft>(model.singleEvent.first())
                f.failReads.value = true
                model.viewState.first { it.settingsFailed }
                model.processIntent(PodnyaIntent.Stop(a)); model.processIntent(PodnyaIntent.Back)
                assertIs<PodnyaEvent.Back>(model.singleEvent.first())
                f.cancelled.await()
                assertEquals(AssistantTurnStatus.STOPPED, f.session.state.value.selected.turns.single().status)
            }
        }
    }

    @Test
    fun failedStoreCommandReportsItsTargetAndDoesNotTerminateTheIntentPipeline() = runBlocking(Dispatchers.Main.immediate) {
        withTimeout(5_000) {
            Fixture().use { f ->
                f.settings.save("https://example.test/v1", "model", "key", true, true)
                val model = f.model
                model.viewState.first { it.ready }
                val id = model.viewState.value.session.id
                model.processIntent(PodnyaIntent.UpdateDraft(id, "kept", "editor", 1))
                model.viewState.first { it.session.draft == "kept" }
                val config = f.settings.snapshot()
                f.failConversationWrites = true
                model.processIntent(PodnyaIntent.Send(id, "editor", 1, config.configId, config.revision))
                model.processIntent(PodnyaIntent.OpenSearch)
                val events = listOf(model.singleEvent.first(), model.singleEvent.first())
                val error = events.filterIsInstance<PodnyaEvent.Rejected>().single()
                assertEquals(id, error.conversationId)
                assertEquals(AssistantError.CONVERSATION_STORAGE, error.error)
                assertTrue(events.contains(PodnyaEvent.OpenSearch))
                f.failConversationWrites = false
                model.processIntent(PodnyaIntent.RetrySave)
                model.viewState.first { it.ready }
                model.processIntent(PodnyaIntent.Rename(id, "still working"))
                model.viewState.first { it.session.title == "still working" }
                assertEquals("kept", model.viewState.value.session.draft)
            }
        }
    }

    @Test
    fun loggedPayloadsDoNotContainQuestionTitleOrCapabilityAction() {
        val id = kotlin.uuid.Uuid.random()
        assertFalse(PodnyaIntent.UpdateDraft(id, "private question", "editor", 1).toString().contains("private question"))
        assertFalse(PodnyaIntent.Rename(id, "private title").toString().contains("private title"))
        assertFalse(PodnyaIntent.PerformAction(id, SampleAction("private-id")).toString().contains("private-id"))
        assertFalse(PodnyaEvent.Action(id, SampleAction("private-id")).toString().contains("private-id"))
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
        val settings = AssistantSettingsRepository(preferences, object : CredentialStore {
            override suspend fun put(id: String, secret: String) = error("Session-only fixture")
            override suspend fun get(id: String): String? = error("Session-only fixture")
            override suspend fun delete(id: String) = error("Session-only fixture")
        })
        var failConversationWrites = false
        private val memory = InMemoryConversationStore()
        private val conversationStore = object : ConversationStore by memory {
            override suspend fun commit(metadata: AssistantSessionState?, turn: com.skyd.podaura.podnya.model.bean.conversation.AssistantSessionTurn?, workspace: com.skyd.podaura.podnya.model.bean.conversation.ConversationWorkspace?) {
                check(!failConversationWrites) { "synthetic store failure" }
                memory.commit(metadata = metadata, turn = turn, workspace = workspace)
            }
        }
        val session = AssistantConversationRepository(object : AssistantRuntime {
            override val available = true
            override suspend fun verify() = AssistantConnectionResult(true)
            override fun run(request: AssistantTurnRequest, connection: com.skyd.podaura.podnya.core.AssistantConnection) = flow<AssistantRunEvent> {
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
        val model = PodnyaViewModel(session, settings).also { store.put("model", it) }
        override fun close() { store.clear(); session.close() }
    }
}
