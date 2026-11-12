package com.skyd.podaura.podnya.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.ViewModelStore
import com.skyd.mvi.mviConfig
import com.skyd.podaura.podnya.model.repository.*
import com.skyd.podaura.podnya.core.AssistantConnectionResult
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantFailure
import com.skyd.podaura.podnya.core.AssistantRunEvent
import com.skyd.podaura.podnya.core.AssistantRuntime
import com.skyd.podaura.podnya.core.AssistantTurnRequest
import com.skyd.fundation.credential.CredentialStorageException
import com.skyd.fundation.credential.CredentialStore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class AssistantSettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val viewModels = ViewModelStore()

    @BeforeTest
    fun setUp() {
        mviConfig { printLog = false }
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        viewModels.clear()
        Dispatchers.resetMain()
        mviConfig { printLog = true }
    }

    @Test
    fun changedRecipientRequiresFreshConsentAndKeyWithoutDiscardingSavedConfiguration() = runTest {
        val fixture = createFixture()
        val model = fixture.model
        runCurrent()
        fixture.session.editDraft("Keep this draft")
        dispatch(model, AssistantSettingsIntent.SetHasNewKey(true))
        dispatch(model, AssistantSettingsIntent.SetBaseUrl("https://second.example/v1"))
        assertFalse(model.viewState.value.consentAccepted)
        assertFalse(model.viewState.value.hasNewKey)

        dispatch(model, AssistantSettingsIntent.Save("old-input-key"))
        runCurrent()
        assertEquals(AssistantError.DISCLOSURE_REQUIRED, failure(fixture))
        dispatch(model, AssistantSettingsIntent.SetConsentAccepted(true))
        dispatch(model, AssistantSettingsIntent.Save("old-input-key"))
        runCurrent()
        assertEquals(AssistantError.CONFIGURATION_REQUIRED, failure(fixture))
        assertEquals("https://first.example/v1/", fixture.repository.snapshot().baseUrl)
        assertEquals("Keep this draft", fixture.session.state.value.selected.draft)

        dispatch(model, AssistantSettingsIntent.SetHasNewKey(true))
        dispatch(model, AssistantSettingsIntent.Save("new-service-key"))
        runCurrent()
        assertEquals("https://second.example/v1/", fixture.repository.snapshot().baseUrl)
        assertEquals("Keep this draft", fixture.session.state.value.selected.draft)
        assertFalse(model.viewState.value.toString().contains("new-service-key"))
        assertEquals(0, fixture.assistant.runCalls)
        assertEquals(0, fixture.assistant.verifyCalls)
    }

    @Test
    fun failedSecureSaveKeepsConversationAndAllowsExplicitSessionOnlyRetry() = runTest {
        val fixture = createFixture()
        runCurrent()
        fixture.session.editDraft("Do not lose me")
        fixture.credentials.failWrites = true
        dispatch(fixture.model, AssistantSettingsIntent.SetHasNewKey(true))
        dispatch(fixture.model, AssistantSettingsIntent.Save("rotated-secret"))
        runCurrent()
        assertEquals(AssistantError.CREDENTIAL_STORAGE_UNAVAILABLE, failure(fixture))
        assertEquals("Do not lose me", fixture.session.state.value.selected.draft)
        assertTrue(fixture.repository.snapshot().hasApiKey)
        assertFalse(fixture.repository.snapshot().sessionOnly)

        dispatch(fixture.model, AssistantSettingsIntent.SetSessionOnly(true))
        dispatch(fixture.model, AssistantSettingsIntent.Save("rotated-secret"))
        runCurrent()
        assertTrue(fixture.repository.snapshot().sessionOnly)
        assertEquals("Do not lose me", fixture.session.state.value.selected.draft)
        assertFalse(fixture.model.viewState.value.hasNewKey)
        assertIs<AssistantSettingsEvent.Saved>(fixture.events.last())
    }

    @Test
    fun testUsesOnlySavedConfigurationAndCancellationStopsActualVerification() = runTest {
        val fixture = createFixture()
        runCurrent()
        dispatch(fixture.model, AssistantSettingsIntent.SetModelId("unsaved-model"))
        dispatch(fixture.model, AssistantSettingsIntent.Verify)
        runCurrent()
        assertEquals(0, fixture.assistant.verifyCalls)

        dispatch(fixture.model, AssistantSettingsIntent.SetModelId("saved-model"))
        fixture.assistant.waitForCancellation = true
        dispatch(fixture.model, AssistantSettingsIntent.Verify)
        runCurrent()
        assertEquals(1, fixture.assistant.verifyCalls)
        assertEquals(AssistantSettingsOperation.TESTING, fixture.model.viewState.value.operation)
        assertEquals(0, fixture.assistant.runCalls)
        dispatch(fixture.model, AssistantSettingsIntent.CancelVerification)
        runCurrent()
        assertTrue(fixture.assistant.verificationCancelled)
        assertEquals(null, fixture.model.viewState.value.operation)
        assertTrue(fixture.events.isEmpty())
        assertTrue(fixture.model.viewState.value.canVerify)
    }

    @Test
    fun canonicalEndpointEditsPreserveRecipientWhileInvalidEditsRevokeConsent() = runTest {
        val fixture = createFixture()
        runCurrent()
        dispatch(fixture.model, AssistantSettingsIntent.SetBaseUrl("https://first.example/v1/chat/completions"))
        assertTrue(fixture.model.viewState.value.consentAccepted)
        assertFalse(fixture.model.viewState.value.dirty)
        dispatch(fixture.model, AssistantSettingsIntent.SetHasNewKey(true))
        dispatch(fixture.model, AssistantSettingsIntent.SetBaseUrl("http://first.example/v1"))
        assertFalse(fixture.model.viewState.value.consentAccepted)
        assertFalse(fixture.model.viewState.value.hasNewKey)
        assertEquals(null, fixture.model.viewState.value.normalizedBaseUrl)
        dispatch(fixture.model, AssistantSettingsIntent.Save(null))
        runCurrent()
        assertEquals(AssistantError.INVALID_ADDRESS, failure(fixture))
    }

    @Test
    fun firstConfigurationPreservesTheQuestionDraftWithoutSendingIt() = runTest {
        val fixture = createFixture(configured = false)
        runCurrent()
        fixture.session.editDraft("Find unread articles about Kotlin")
        dispatch(fixture.model, AssistantSettingsIntent.SetBaseUrl("https://first.example/v1"))
        dispatch(fixture.model, AssistantSettingsIntent.SetModelId("saved-model"))
        dispatch(fixture.model, AssistantSettingsIntent.SetConsentAccepted(true))
        dispatch(fixture.model, AssistantSettingsIntent.SetHasNewKey(true))

        dispatch(fixture.model, AssistantSettingsIntent.Save("initial-secret"))
        runCurrent()

        assertTrue(fixture.repository.snapshot().configured)
        assertEquals("Find unread articles about Kotlin", fixture.session.state.value.selected.draft)
        assertTrue(fixture.session.state.value.selected.turns.isEmpty())
        assertIs<AssistantSettingsEvent.Saved>(fixture.events.last())
        assertEquals(0, fixture.assistant.runCalls)
        assertEquals(0, fixture.assistant.verifyCalls)
    }

    @Test
    fun keyUpdatePreservesCompletedHistoryAndUnsentQuestionWithoutResubmitting() = runTest {
        val fixture = createFixture()
        runCurrent()
        fixture.session.editDraft("Previously sent question")
        assertTrue(fixture.session.sendDraft(fixture.repository))
        runCurrent()
        assertEquals(1, fixture.session.state.value.selected.turns.size)
        fixture.session.editDraft("My next question")

        dispatch(fixture.model, AssistantSettingsIntent.SetHasNewKey(true))
        dispatch(fixture.model, AssistantSettingsIntent.Save("rotated-secret"))
        runCurrent()

        assertEquals(1, fixture.session.state.value.selected.turns.size)
        assertEquals("My next question", fixture.session.state.value.selected.draft)
        assertFalse(fixture.session.state.value.selected.isRunning)
        assertEquals(1, fixture.assistant.runCalls)
        assertEquals(0, fixture.assistant.verifyCalls)
    }

    @Test
    fun consecutiveEditsAreReducedBeforeSaveAndDuplicateSaveCannotRaceTheCommit() = runTest {
        val fixture = createFixture(configured = false)
        runCurrent()
        fixture.session.editDraft("Keep this question local")
        // No scheduler advancement between intents: the reducer must preserve their order.
        dispatch(fixture.model,
            AssistantSettingsIntent.SetBaseUrl("https://new.example/v1"),
            AssistantSettingsIntent.SetModelId("new-model"),
            AssistantSettingsIntent.SetConsentAccepted(true),
            AssistantSettingsIntent.SetHasNewKey(true),
            AssistantSettingsIntent.Save("first-key"),
            AssistantSettingsIntent.Save("must-not-overwrite"),
        )
        val saved = fixture.repository.connection()
        assertEquals("https://new.example/v1/", saved.baseUrl)
        assertEquals("new-model", saved.modelId)
        assertEquals("first-key", saved.apiKey)
        assertEquals(1, fixture.credentials.writes)
        assertEquals(listOf<AssistantSettingsEvent>(AssistantSettingsEvent.Saved), fixture.events)
        assertEquals(1, fixture.model.viewState.value.savedRevision)
        assertEquals("Keep this question local", fixture.session.state.value.selected.draft)
        assertFalse(fixture.model.viewState.value.dirty)
    }

    @Test
    fun cancellingVerificationBeforeItsCoroutineStartsStillClearsTheBusyState() = runTest {
        val fixture = createFixture()
        runCurrent()
        dispatch(fixture.model, AssistantSettingsIntent.Verify, AssistantSettingsIntent.CancelVerification)
        assertEquals(null, fixture.model.viewState.value.operation)
        assertTrue(fixture.model.viewState.value.canVerify)
        assertTrue(fixture.events.isEmpty())
        dispatch(fixture.model, AssistantSettingsIntent.Verify)
        assertEquals(listOf<AssistantSettingsEvent>(AssistantSettingsEvent.ConnectionSucceeded), fixture.events)
    }

    @Test
    fun verificationAndClearEmitSingleEventsWithoutRetainingANoticeInState() = runTest {
        val fixture = createFixture()
        runCurrent()
        dispatch(fixture.model, AssistantSettingsIntent.Verify)
        assertEquals(listOf<AssistantSettingsEvent>(AssistantSettingsEvent.ConnectionSucceeded), fixture.events)
        dispatch(fixture.model, AssistantSettingsIntent.SetModelId("saved-model"))
        assertEquals(1, fixture.events.size)
        fixture.session.editDraft("Clear this with the configuration")
        dispatch(fixture.model, AssistantSettingsIntent.Clear, AssistantSettingsIntent.Clear)
        assertEquals(listOf<AssistantSettingsEvent>(AssistantSettingsEvent.ConnectionSucceeded, AssistantSettingsEvent.Cleared), fixture.events)
        assertFalse(fixture.repository.snapshot().configured)
        assertEquals("Clear this with the configuration", fixture.session.state.value.selected.draft)
        assertEquals(null, fixture.model.viewState.value.operation)
    }

    @Test
    fun clearingViewModelDuringSaveFinishesTheLocalCommitAndPreservesUnsentDraft() = runTest {
        val fixture = createFixture()
        runCurrent()
        fixture.session.editDraft("Draft survives navigation")
        val finishWrite = CompletableDeferred<Unit>()
        fixture.credentials.blockWrites = finishWrite
        dispatch(fixture.model, AssistantSettingsIntent.SetHasNewKey(true), AssistantSettingsIntent.Save("replacement-key"))
        assertEquals(AssistantSettingsOperation.SAVING, fixture.model.viewState.value.operation)
        viewModels.clear()
        runCurrent()
        finishWrite.complete(Unit)
        runCurrent()
        assertEquals("replacement-key", fixture.repository.connection().apiKey)
        assertEquals("Draft survives navigation", fixture.session.state.value.selected.draft)
        assertTrue(fixture.events.isEmpty())
    }

    @Test
    fun loggedIntentAndStateDescriptionsNeverContainCredentialsOrServiceDetails() {
        val address = "https://private-service.example/v1"
        val model = "private-model-name"
        val secret = "synthetic-private-api-key"
        val descriptions = listOf(
            AssistantSettingsIntent.SetBaseUrl(address), AssistantSettingsIntent.SetModelId(model),
            AssistantSettingsIntent.Save(secret),
            AssistantSettingsState(saved = AssistantSettings(baseUrl = address, modelId = model), baseUrl = address, modelId = model),
        ).joinToString()
        for (value in listOf(address, model, secret)) assertFalse(descriptions.contains(value))
        assertTrue(AssistantSettingsIntent.Save(secret).toString().contains("redacted"))
    }

    private suspend fun TestScope.dispatch(model: AssistantSettingsViewModel, vararg intents: AssistantSettingsIntent) {
        withContext(Dispatchers.Main.immediate) {
            intents.forEach { model.processIntent(it) }
        }
        runCurrent()
    }

    private fun failure(fixture: Fixture): AssistantFailure? =
        assertIs<AssistantSettingsEvent.Failed>(fixture.events.last()).error

    private suspend fun TestScope.createFixture(configured: Boolean = true): Fixture {
        val credentials = MemoryCredentials()
        val repository = AssistantSettingsRepository(MemoryPreferences(), credentials)
        if (configured) {
            repository.save("https://first.example/v1", "saved-model", "initial-secret", false, true)
        }
        val assistant = RecordingAssistant()
        val session = AssistantConversationRepository(assistant, repository, scope = backgroundScope)
        val model = AssistantSettingsViewModel(repository, assistant)
        viewModels.put("settings", model)
        val fixture = Fixture(repository, credentials, assistant, session, model)
        backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            model.singleEvent.collect { fixture.events += it }
        }
        return fixture
    }

    private class Fixture(
        val repository: AssistantSettingsRepository,
        val credentials: MemoryCredentials,
        val assistant: RecordingAssistant,
        val session: AssistantConversationRepository,
        val model: AssistantSettingsViewModel,
    ) {
        val events = mutableListOf<AssistantSettingsEvent>()
    }

    private class MemoryPreferences : DataStore<Preferences> {
        private val values = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = values
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            transform(values.value).also { values.value = it }
    }

    private class MemoryCredentials : CredentialStore {
        private val keys = mutableMapOf<String, String>()
        var failWrites = false
        var writes = 0
        var blockWrites: CompletableDeferred<Unit>? = null
        override suspend fun put(id: String, secret: String) {
            if (failWrites) throw CredentialStorageException()
            blockWrites?.await()
            keys[id] = secret
            writes++
        }
        override suspend fun get(id: String): String? = keys[id]
        override suspend fun delete(id: String) { keys.remove(id) }
    }

    private class RecordingAssistant : AssistantRuntime {
        override val available = true
        var verifyCalls = 0
        var runCalls = 0
        var waitForCancellation = false
        var verificationCancelled = false
        override fun run(request: AssistantTurnRequest, connection: com.skyd.podaura.podnya.core.AssistantConnection): Flow<AssistantRunEvent> {
            runCalls++
            return flowOf(AssistantRunEvent.ReplyDelta("Previous reply"), AssistantRunEvent.Completed)
        }
        override suspend fun verify(): AssistantConnectionResult {
            verifyCalls++
            if (waitForCancellation) {
                try { awaitCancellation() }
                finally { verificationCancelled = true }
            }
            return AssistantConnectionResult(success = true)
        }
    }
}
