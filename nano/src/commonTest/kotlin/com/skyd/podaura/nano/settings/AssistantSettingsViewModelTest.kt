package com.skyd.podaura.nano.settings

import com.skyd.podaura.nano.core.AssistantConnection
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.ViewModelStore
import com.skyd.mvi.mviConfig
import com.skyd.podaura.nano.model.repository.conversation.*
import com.skyd.podaura.nano.core.AssistantReasoningEffort
import com.skyd.podaura.nano.core.AssistantProtocol
import com.skyd.podaura.nano.core.AssistantConnectionResult
import com.skyd.podaura.nano.core.AssistantError
import com.skyd.podaura.nano.core.AssistantFailure
import com.skyd.podaura.nano.core.AssistantRunEvent
import com.skyd.podaura.nano.core.AssistantRuntime
import com.skyd.podaura.nano.core.AssistantTurnRequest
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
    fun changedRecipientRequiresFreshKeyWithoutDiscardingSavedConfiguration() = runTest {
        val fixture = createFixture()
        val model = fixture.model
        runCurrent()
        fixture.session.editDraft("Keep this draft")
        dispatch(model, newKey(model))
        dispatch(model, AssistantSettingsIntent.SetBaseUrl("https://second.example/v1"))
        assertFalse(model.viewState.value.hasNewKey)

        dispatch(model, save(model, "old-input-key"))
        runCurrent()
        assertEquals(AssistantError.CONFIGURATION_REQUIRED, failure(fixture))
        assertEquals("https://first.example/v1/", fixture.repository.snapshot().baseUrl)
        assertEquals("Keep this draft", fixture.session.state.value.selected.draft)

        dispatch(model, newKey(model))
        dispatch(model, save(model, "new-service-key"))
        runCurrent()
        assertEquals("https://second.example/v1/", fixture.repository.snapshot().baseUrl)
        assertEquals("Keep this draft", fixture.session.state.value.selected.draft)
        assertFalse(model.viewState.value.toString().contains("new-service-key"))
        assertEquals(0, fixture.assistant.runCalls)
        assertEquals(0, fixture.assistant.verifyCalls)
    }

    @Test
    fun failedSecureSaveKeepsConversationAndAllowsRetry() = runTest {
        val fixture = createFixture()
        runCurrent()
        fixture.session.editDraft("Do not lose me")
        fixture.credentials.failWrites = true
        dispatch(fixture.model, newKey(fixture.model))
        dispatch(fixture.model, save(fixture.model, "rotated-secret"))
        runCurrent()
        assertEquals(AssistantError.CREDENTIAL_STORAGE_UNAVAILABLE, failure(fixture))
        assertEquals("Do not lose me", fixture.session.state.value.selected.draft)
        assertTrue(fixture.repository.snapshot().hasApiKey)

        fixture.credentials.failWrites = false
        dispatch(fixture.model, save(fixture.model, "rotated-secret"))
        runCurrent()
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
    fun canonicalEndpointEditsPreserveRecipientWhileInvalidEditsDropTheDraftKey() = runTest {
        val fixture = createFixture()
        runCurrent()
        dispatch(fixture.model, AssistantSettingsIntent.SetBaseUrl("https://first.example/v1/chat/completions"))
        assertFalse(fixture.model.viewState.value.dirty)
        dispatch(fixture.model, newKey(fixture.model))
        dispatch(fixture.model, AssistantSettingsIntent.SetBaseUrl("http://first.example/v1"))
        assertFalse(fixture.model.viewState.value.hasNewKey)
        assertEquals(null, fixture.model.viewState.value.normalizedBaseUrl)
        dispatch(fixture.model, save(fixture.model, null))
        runCurrent()
        assertEquals(AssistantError.INVALID_ADDRESS, failure(fixture))
    }

    @Test
    fun firstConfigurationPreservesTheQuestionDraftWithoutSendingIt() = runTest {
        val fixture = createFixture(configured = false)
        runCurrent()
        dispatch(fixture.model, AssistantSettingsIntent.SelectProvider(AssistantProviderPresets.CUSTOM))
        fixture.session.editDraft("Find unread articles about Kotlin")
        dispatch(fixture.model, AssistantSettingsIntent.SetBaseUrl("https://first.example/v1"))
        dispatch(fixture.model, AssistantSettingsIntent.SetModelId("saved-model"))
        dispatch(fixture.model, newKey(fixture.model))

        dispatch(fixture.model, save(fixture.model, "initial-secret"))
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
        assertTrue(fixture.session.sendDraft())
        runCurrent()
        assertEquals(1, fixture.session.state.value.selected.turns.size)
        fixture.session.editDraft("My next question")

        dispatch(fixture.model, newKey(fixture.model))
        dispatch(fixture.model, save(fixture.model, "rotated-secret"))
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
        dispatch(fixture.model, AssistantSettingsIntent.SelectProvider(AssistantProviderPresets.CUSTOM))
        val initial = fixture.model.viewState.value
        fixture.session.editDraft("Keep this question local")
        // No scheduler advancement between intents: the reducer must preserve their order.
        dispatch(fixture.model,
            AssistantSettingsIntent.SetBaseUrl("https://new.example/v1"),
            AssistantSettingsIntent.SetModelId("new-model"),
            AssistantSettingsIntent.SetHasNewKey(value = true, keyRevision = initial.keyRevision + 1),
            AssistantSettingsIntent.Save(apiKey = "first-key", editRevision = initial.editRevision + 3, keyRevision = initial.keyRevision + 1),
            AssistantSettingsIntent.Save(apiKey = "must-not-overwrite", editRevision = initial.editRevision + 3, keyRevision = initial.keyRevision + 1),
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
        val clear = AssistantSettingsIntent.Clear(profileId = fixture.repository.snapshot().configId)
        dispatch(fixture.model, clear, clear)
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
        val initial = fixture.model.viewState.value
        dispatch(
            fixture.model,
            newKey(fixture.model),
            AssistantSettingsIntent.Save(apiKey = "replacement-key", editRevision = initial.editRevision + 1, keyRevision = initial.keyRevision),
        )
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
            AssistantSettingsIntent.SetName(model), AssistantSettingsIntent.SetBaseUrl(address), AssistantSettingsIntent.SetModelId(model),
            AssistantSettingsIntent.Save(apiKey = secret, editRevision = 0, keyRevision = 0),
            AssistantSettingsState(saved = AssistantSettings(baseUrl = address, modelId = model), baseUrl = address, modelId = model),
        ).joinToString()
        for (value in listOf(address, model, secret)) assertFalse(descriptions.contains(value))
        assertTrue(AssistantSettingsIntent.Save(apiKey = secret, editRevision = 0, keyRevision = 0).toString().contains("redacted"))
    }

    @Test
    fun switchingIsVersionedAndLateKeyOrSaveCannotReachTheNewRecipient() = runTest {
        val fixture = createFixture()
        runCurrent()
        val model = fixture.model
        val saved = fixture.repository.snapshot()
        dispatch(model, AssistantSettingsIntent.SelectProvider("openai"))
        assertEquals("openai", model.viewState.value.providerId)
        assertEquals(saved, fixture.repository.snapshot())
        dispatch(model, newKey(model))
        val before = model.viewState.value
        val lateSave = save(model, "old-key")
        dispatch(model, AssistantSettingsIntent.SelectProvider("claude"))
        assertEquals(AssistantProtocol.ANTHROPIC_MESSAGES, model.viewState.value.protocol)
        assertFalse(model.viewState.value.hasNewKey)
        dispatch(model, AssistantSettingsIntent.SetHasNewKey(value = true, keyRevision = before.keyRevision), lateSave)
        assertFalse(model.viewState.value.hasNewKey)
        assertEquals(saved, fixture.repository.snapshot())
        dispatch(model, newKey(model))
        dispatch(model, save(model, "new-key"))
        assertEquals("new-key", fixture.repository.connection().apiKey)
        assertEquals(AssistantProtocol.ANTHROPIC_MESSAGES, fixture.repository.connection().protocol)
        assertEquals(saved.configId, fixture.repository.snapshot().configId)
    }

    @Test
    fun customKeepsTheEditingStartAndReselectingOfficialAppliesNativePreset() = runTest {
        val fixture = createFixture(configured = false)
        runCurrent()
        val model = fixture.model
        assertEquals(null, model.viewState.value.providerId)
        assertFalse(model.viewState.value.dirty)
        fixture.repository.save(
            baseUrl = "https://api.anthropic.com/v1/", modelId = "old-manual-model", apiKey = "saved-key",
        )
        runCurrent()
        assertEquals("claude", model.viewState.value.providerId)
        assertTrue(model.viewState.value.manualModel)
        assertEquals(AssistantProtocol.OPENAI_COMPATIBLE, model.viewState.value.protocol)
        dispatch(model, AssistantSettingsIntent.SelectProvider("claude"))
        assertEquals(AssistantProtocol.ANTHROPIC_MESSAGES, model.viewState.value.protocol)
        assertEquals(AssistantProtocol.OPENAI_COMPATIBLE, fixture.repository.connection().protocol)
        val previous = model.viewState.value
        dispatch(model, AssistantSettingsIntent.SelectProvider(AssistantProviderPresets.CUSTOM))
        assertEquals(previous.baseUrl, model.viewState.value.baseUrl)
        assertEquals(previous.modelId, model.viewState.value.modelId)
        assertEquals(previous.protocol, model.viewState.value.protocol)
        assertTrue(model.viewState.value.custom)
    }

    @Test
    fun editingKeyAgainInvalidatesOldSaveEvenWhenBothInputsAreNonempty() = runTest {
        val fixture = createFixture()
        runCurrent()
        val model = fixture.model
        dispatch(model, newKey(model))
        val late = save(model, "first-input")
        dispatch(model, newKey(model))
        dispatch(model, late)
        assertEquals("initial-secret", fixture.repository.connection().apiKey)
        assertTrue(model.viewState.value.hasNewKey)
        dispatch(model, save(model, "second-input"))
        assertEquals("second-input", fixture.repository.connection().apiKey)
    }

    @Test
    fun effortEditsSaveInOrderPreserveCompatibleModelsAndResetUnsupportedConnections() = runTest {
        val fixture = createFixture(configured = false)
        val model = fixture.model
        runCurrent()
        fixture.repository.save(
            baseUrl = "https://api.openai.com/v1/",
            modelId = "gpt-6.1-sol",
            apiKey = "secret",
        )
        runCurrent()
        val initial = model.viewState.value
        assertTrue(initial.canVerify)
        dispatch(
            model,
            AssistantSettingsIntent.SetReasoningEffort(AssistantReasoningEffort.MAX),
            AssistantSettingsIntent.Save(
                apiKey = null,
                editRevision = initial.editRevision + 1,
                keyRevision = initial.keyRevision,
            ),
        )
        assertEquals(AssistantReasoningEffort.MAX, fixture.repository.connection().reasoningEffort)
        assertFalse(model.viewState.value.dirty)
        assertTrue(model.viewState.value.canVerify)
        val writes = fixture.credentials.writes
        val staleSave = save(model, null)
        dispatch(model, AssistantSettingsIntent.SetReasoningEffort(AssistantReasoningEffort.HIGH))
        assertTrue(model.viewState.value.dirty)
        assertFalse(model.viewState.value.canVerify)
        dispatch(model, staleSave, AssistantSettingsIntent.Verify)
        assertEquals(AssistantReasoningEffort.MAX, fixture.repository.connection().reasoningEffort)
        assertEquals(0, fixture.assistant.verifyCalls)
        dispatch(model, AssistantSettingsIntent.SetModelId("gpt-6.1-terra-snapshot"))
        assertEquals(AssistantReasoningEffort.HIGH, model.viewState.value.reasoningEffort)
        dispatch(model, AssistantSettingsIntent.SetModelId("unknown-model"))
        assertFalse(model.viewState.value.supportsReasoningEffort)
        assertTrue(model.viewState.value.reasoningEffortReset)
        assertEquals(AssistantReasoningEffort.LOW, model.viewState.value.reasoningEffort)
        dispatch(model, AssistantSettingsIntent.SetReasoningEffort(AssistantReasoningEffort.MAX))
        assertEquals(AssistantReasoningEffort.LOW, model.viewState.value.reasoningEffort)
        dispatch(model, AssistantSettingsIntent.SetModelId("gpt-6.1-sol"))
        assertTrue(model.viewState.value.supportsReasoningEffort)
        assertFalse(model.viewState.value.reasoningEffortReset)
        dispatch(model, save(model, null))
        assertEquals(AssistantReasoningEffort.LOW, fixture.repository.connection().reasoningEffort)
        assertEquals(writes, fixture.credentials.writes)
    }

    @Test
    fun testingAnInactiveProfileUsesItsConnectionAndKeepsGlobalSelection() = runTest {
        val fixture = createFixture()
        runCurrent()
        val active = fixture.repository.snapshot()
        val backup = fixture.repository.save(
            baseUrl = "https://second.example",
            modelId = "backup-model",
            name = "Backup",
            apiKey = "backup-key",
        )
        runCurrent()
        dispatch(fixture.model, AssistantSettingsIntent.EditProfile(backup.configId))
        dispatch(fixture.model, AssistantSettingsIntent.Verify)
        assertEquals("backup-model", fixture.assistant.verifiedModel)
        assertEquals(active, fixture.repository.snapshot())
    }

    @Test
    fun deletingDuringVerificationReconcilesTheEditorAndRejectsStaleDelete() = runTest {
        val fixture = createFixture()
        runCurrent()
        val original = fixture.repository.snapshot()
        val backup = fixture.repository.save(baseUrl = original.baseUrl, modelId = "backup", apiKey = "backup")
        fixture.assistant.waitForCancellation = true
        dispatch(fixture.model, AssistantSettingsIntent.Verify)
        fixture.repository.delete(id = requireNotNull(original.configId), replacementId = backup.configId)
        runCurrent()
        dispatch(fixture.model, AssistantSettingsIntent.CancelVerification)
        assertEquals(backup, fixture.model.viewState.value.saved)
        dispatch(fixture.model, AssistantSettingsIntent.Clear(profileId = original.configId))
        assertEquals(backup, fixture.repository.snapshot())
    }

    @Test
    fun conflictingEditorCanReloadLatestVersionBeforeRetrying() = runTest {
        val fixture = createFixture()
        runCurrent()
        val model = fixture.model
        dispatch(model, AssistantSettingsIntent.SetName("Local draft"))
        val original = fixture.repository.snapshot()
        val external = fixture.repository.save(
            baseUrl = original.baseUrl,
            modelId = "external-model",
            apiKey = null,
            editing = original,
        )
        runCurrent()
        assertTrue(model.viewState.value.hasEditConflict)
        assertEquals("Local draft", model.viewState.value.name)
        dispatch(model, AssistantSettingsIntent.DiscardConflictingEdits)
        assertEquals(external, model.viewState.value.saved)
        dispatch(model, AssistantSettingsIntent.SetName("Work"))
        dispatch(model, save(model, null))
        assertEquals("Work", fixture.repository.snapshot().name)
        assertEquals("external-model", fixture.repository.snapshot().modelId)
    }

    @Test
    fun lateVerificationDoesNotReportSuccessForAnotherProfile() = runTest {
        val fixture = createFixture()
        runCurrent()
        val gate = CompletableDeferred<Unit>()
        fixture.assistant.verificationGate = gate
        dispatch(fixture.model, AssistantSettingsIntent.Verify)
        fixture.repository.delete(id = requireNotNull(fixture.repository.snapshot().configId), replacementId = null)
        gate.complete(Unit)
        runCurrent()
        assertTrue(fixture.events.isEmpty())
        assertFalse(fixture.model.viewState.value.saved.configured)
    }

    @Test
    fun selectingTheSameEditorPreservesItsNameAndPendingKey() = runTest {
        val fixture = createFixture()
        runCurrent()
        val model = fixture.model
        dispatch(model, AssistantSettingsIntent.EditProfile(model.viewState.value.saved.configId))
        dispatch(model, AssistantSettingsIntent.SetName("Work"), newKey(model))
        val draft = model.viewState.value
        dispatch(model, AssistantSettingsIntent.EditProfile(draft.saved.configId))
        dispatch(model, AssistantSettingsIntent.SelectProvider(AssistantProviderPresets.CUSTOM))
        assertEquals(draft, model.viewState.value)
    }

    private fun newKey(model: AssistantSettingsViewModel) = AssistantSettingsIntent.SetHasNewKey(
        value = true, keyRevision = model.viewState.value.keyRevision,
    )

    private fun save(model: AssistantSettingsViewModel, secret: String?) = AssistantSettingsIntent.Save(
        apiKey = secret, editRevision = model.viewState.value.editRevision, keyRevision = model.viewState.value.keyRevision,
    )

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
            repository.save(baseUrl = "https://first.example/v1", modelId = "saved-model", apiKey = "initial-secret")
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
        var verifiedModel: String? = null
        var verificationGate: CompletableDeferred<Unit>? = null
        var verifyCalls = 0
        var runCalls = 0
        var waitForCancellation = false
        var verificationCancelled = false
        override fun run(request: AssistantTurnRequest, connection: com.skyd.podaura.nano.core.AssistantConnection): Flow<AssistantRunEvent> {
            runCalls++
            return flowOf(AssistantRunEvent.ReplyDelta("Previous reply"), AssistantRunEvent.Completed)
        }
        override suspend fun verify(connection: suspend () -> AssistantConnection): AssistantConnectionResult {
            verifiedModel = connection().modelId
            verifyCalls++
            verificationGate?.await()
            if (waitForCancellation) {
                try { awaitCancellation() }
                finally { verificationCancelled = true }
            }
            return AssistantConnectionResult(success = true)
        }
    }
}
