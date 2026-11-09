package com.skyd.podaura.podnya.ui.screen.chat

import com.skyd.podaura.podnya.model.bean.AssistantTurnStatus
import com.skyd.podaura.podnya.model.repository.AssistantConversationRepository
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class PodnyaViewModelTest {
    @Test
    fun navigationAndViewModelRecreationKeepTheSameGenerationRunning() = runBlocking(Dispatchers.Main.immediate) {
        withTimeout(5_000) {
            Fixture().use { fixture ->
                fixture.settings.save("https://example.test/v1", "model", "synthetic-key", true, true)
                val model = fixture.model
                model.viewState.first { it.ready }
                model.processIntent(PodnyaIntent.UpdateDraft("p"))
                model.processIntent(PodnyaIntent.UpdateDraft("private"))
                model.processIntent(PodnyaIntent.UpdateDraft("private question"))
                model.processIntent(PodnyaIntent.Send("editor", 1))
                model.processIntent(PodnyaIntent.Send("editor", 1))
                model.viewState.first { it.session.isRunning }
                fixture.started.await()
                assertEquals(1, fixture.requests)
                assertEquals("private question", fixture.lastQuestion)
                assertEquals("", model.viewState.value.session.draft)

                assertEquals(PodnyaEvent.ClearDraft("editor", 1), model.singleEvent.first())
                for ((intent, event) in listOf(
                    PodnyaIntent.OpenSettings to PodnyaEvent.OpenSettings,
                    PodnyaIntent.OpenSearch to PodnyaEvent.OpenSearch,
                    PodnyaIntent.Back to PodnyaEvent.Back,
                )) {
                    model.processIntent(intent)
                    assertEquals(event, model.singleEvent.first())
                    assertEquals(AssistantTurnStatus.RUNNING, fixture.session.state.value.turns.single().status)
                    assertFalse(fixture.cancelled.isCompleted)
                }
                assertNull(withTimeoutOrNull(50) { model.singleEvent.first() })
                fixture.store.clear()
                yield()
                assertFalse(fixture.cancelled.isCompleted)
                fixture.finish.complete(Unit)
                fixture.session.state.first { it.turns.single().status == AssistantTurnStatus.COMPLETED }
                val reopened = PodnyaViewModel(fixture.session, fixture.settings)
                fixture.store.put("reopened", reopened)
                assertEquals("partial complete", reopened.viewState.value.session.turns.single().reply)
                assertEquals(1, fixture.requests)

                reopened.processIntent(PodnyaIntent.UpdateDraft("next question"))
                reopened.viewState.first { it.session.draft == "next question" }
                reopened.processIntent(PodnyaIntent.NewConversation("editor", 2))
                reopened.viewState.first { it.session.turns.isEmpty() && it.session.draft.isEmpty() }
                assertEquals(1, fixture.requests)
            }
        }
    }

    @Test
    fun mixedIntentBurstKeepsResetDraftSendAndRetryInOrder() = runBlocking(Dispatchers.Main.immediate) {
        withTimeout(5_000) {
            Fixture().use { fixture ->
                fixture.settings.save("https://example.test/v1", "model", "synthetic-key", true, true)
                val model = fixture.model
                model.viewState.first { it.ready }

                // No waits between commands: separate subscribers must not drain all draft edits
                // before Send, or process NewConversation after the new request has started.
                model.processIntent(PodnyaIntent.UpdateDraft("discard this"))
                model.processIntent(PodnyaIntent.NewConversation("editor", 1))
                model.processIntent(PodnyaIntent.UpdateDraft("send this"))
                model.processIntent(PodnyaIntent.Send("editor", 2))
                model.processIntent(PodnyaIntent.UpdateDraft("next draft"))
                model.processIntent(PodnyaIntent.OpenSettings)

                assertEquals(PodnyaEvent.ClearDraft("editor", 1), model.singleEvent.first())
                assertEquals(PodnyaEvent.ClearDraft("editor", 2), model.singleEvent.first())
                assertEquals(PodnyaEvent.OpenSettings, model.singleEvent.first())
                fixture.started.await()
                assertEquals("send this", fixture.lastQuestion)
                assertEquals("next draft", fixture.session.state.value.draft)
                assertEquals(1, fixture.requests)
                assertFalse(fixture.cancelled.isCompleted)

                model.processIntent(PodnyaIntent.Stop)
                model.processIntent(PodnyaIntent.Retry)
                model.processIntent(PodnyaIntent.Back)
                assertEquals(PodnyaEvent.Back, model.singleEvent.first())
                fixture.cancelled.await()
                assertEquals(2, fixture.requests)
                assertEquals("send this", fixture.lastQuestion)
                assertEquals("next draft", fixture.session.state.value.draft)
                fixture.finish.complete(Unit)
                model.viewState.first { it.session.turns.single().status == AssistantTurnStatus.COMPLETED }
                assertEquals("partial complete", model.viewState.value.session.turns.single().reply)
            }
        }
    }

    @Test
    fun unconfiguredSendIsIgnoredAndRecreatedViewModelKeepsProcessSession() = runBlocking(Dispatchers.Main.immediate) {
        withTimeout(5_000) {
            Fixture().use { fixture ->
                val model = fixture.model
                model.viewState.first { it.settings != null }
                model.processIntent(PodnyaIntent.UpdateDraft("kept draft"))
                model.viewState.first { it.session.draft == "kept draft" }
                model.processIntent(PodnyaIntent.Send("editor", 1))
                // A subsequent navigation event proves the preceding Send intent was processed.
                model.processIntent(PodnyaIntent.OpenSearch)
                assertIs<PodnyaEvent.OpenSearch>(model.singleEvent.first())
                assertEquals(0, fixture.requests)
                assertFalse(model.viewState.value.session.isRunning)
                fixture.store.clear()
                val reopened = PodnyaViewModel(fixture.session, fixture.settings)
                fixture.store.put("reopened", reopened)
                assertEquals("kept draft", reopened.viewState.value.session.draft)
                yield()
            }
        }
    }

    @Test
    fun settingsReadFailureDoesNotKillStopOrNavigationIntents() = runBlocking(Dispatchers.Main.immediate) {
        withTimeout(5_000) {
            Fixture().use { fixture ->
                fixture.settings.save("https://example.test/v1", "model", "synthetic-key", true, true)
                val model = fixture.model
                model.viewState.first { it.ready }
                model.processIntent(PodnyaIntent.UpdateDraft("question"))
                model.processIntent(PodnyaIntent.Send("editor", 1))
                fixture.started.await()
                assertIs<PodnyaEvent.ClearDraft>(model.singleEvent.first())
                fixture.failReads.value = true
                model.viewState.first { it.settingsFailed }
                assertFalse(model.viewState.value.ready)
                model.processIntent(PodnyaIntent.Stop)
                model.processIntent(PodnyaIntent.Back)
                assertIs<PodnyaEvent.Back>(model.singleEvent.first())
                fixture.cancelled.await()
                assertEquals(AssistantTurnStatus.STOPPED, fixture.session.state.value.turns.single().status)
            }
        }
    }

    @Test
    fun capabilityActionKeepsTheRunAndExplicitStopStillCancels() = runBlocking(Dispatchers.Main.immediate) {
        withTimeout(5_000) {
            Fixture().use { fixture ->
                fixture.settings.save("https://example.test/v1", "model", "synthetic-key", true, true)
                val model = fixture.model
                model.viewState.first { it.ready }
                model.processIntent(PodnyaIntent.UpdateDraft("private question"))
                model.processIntent(PodnyaIntent.Send("editor", 1))
                fixture.started.await()
                assertIs<PodnyaEvent.ClearDraft>(model.singleEvent.first())
                val action = SampleAction("private-target")
                model.processIntent(PodnyaIntent.PerformAction(action))
                assertEquals(PodnyaEvent.Action(action), model.singleEvent.first())
                assertEquals(AssistantTurnStatus.RUNNING, fixture.session.state.value.turns.single().status)
                assertFalse(fixture.cancelled.isCompleted)
                model.processIntent(PodnyaIntent.Stop)
                fixture.cancelled.await()
                assertEquals(AssistantTurnStatus.STOPPED, fixture.session.state.value.turns.single().status)
                assertNull(withTimeoutOrNull(50) { model.singleEvent.first() })
            }
        }
    }

    @Test
    fun loggedPayloadsDoNotContainQuestionOrCapabilityAction() {
        assertFalse(PodnyaIntent.UpdateDraft("private question").toString().contains("private question"))
        assertFalse(PodnyaIntent.PerformAction(SampleAction("private-id")).toString().contains("private-id"))
        assertFalse(PodnyaEvent.Action(SampleAction("private-id")).toString().contains("private-id"))
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
        val session = AssistantConversationRepository(object : AssistantRuntime {
            override val available = true
            override suspend fun verify() = AssistantConnectionResult(true)
            override fun run(request: AssistantTurnRequest) = flow<AssistantRunEvent> {
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
        }, CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate))
        val model = PodnyaViewModel(session, settings).also { store.put("model", it) }
        override fun close() { store.clear(); session.close() }
    }
}
