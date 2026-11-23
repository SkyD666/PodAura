package com.skyd.podaura.nano.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.fundation.credential.CredentialStore
import com.skyd.podaura.nano.core.AssistantReasoningEffort
import com.skyd.podaura.nano.core.AssistantProtocol
import com.skyd.podaura.nano.core.assistantEndpointPath
import com.skyd.podaura.nano.core.normalizeAssistantBaseUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AssistantConfigurationFormatTest {
    private val key = stringPreferencesKey("nano.configuration.v1")
    private class Store : DataStore<Preferences> {
        val values = MutableStateFlow(emptyPreferences())
        var failRead = false
        var failWrite = false
        override val data = flow {
            if (failRead) error("Read failure")
            emit(values.value)
        }
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            if (failWrite) error("Write failure")
            return transform(values.value).also { values.value = it }
        }
    }
    private class Credentials : CredentialStore {
        val values = mutableMapOf<String, String>()
        override suspend fun put(id: String, secret: String) { values[id] = secret }
        override suspend fun get(id: String) = values[id]
        override suspend fun delete(id: String) { values.remove(id) }
    }

    @Test
    fun legacyEffortDefaultsToLowAndEveryLevelSurvivesReopeningWithoutReplacingCredentials() = runTest {
        for (version in 1..2) {
            val raw = """{"version":$version,"protocol":"openai-compatible","configId":"stable","revision":7,"baseUrl":"https://api.openai.com/v1/","modelId":"gpt-6.1-terra","credentialId":"key","consentFor":"https://api.openai.com/v1/","consentVersion":1}"""
            val store = Store().apply { values.value = mutablePreferencesOf(key to raw) }
            val credentials = Credentials().apply { values["key"] = "secret" }
            val repository = AssistantSettingsRepository(dataStore = store, credentialStore = credentials)
            val original = repository.connection()
            assertEquals(AssistantReasoningEffort.LOW, original.reasoningEffort)
            assertTrue(store.values.value[key]!!.contains("\"version\":4"))
            for (effort in AssistantReasoningEffort.entries) {
                val before = repository.snapshot()
                repository.save(
                    editing = before,
                    baseUrl = before.baseUrl,
                    modelId = before.modelId,
                    apiKey = null,
                    reasoningEffort = effort,
                )
                val reopened = AssistantSettingsRepository(dataStore = store, credentialStore = credentials)
                assertEquals(effort, reopened.connection().reasoningEffort)
                assertEquals(effort, reopened.snapshot().reasoningEffort)
                assertEquals("stable", reopened.snapshot().configId)
                assertEquals(before.revision + 1, reopened.snapshot().revision)
                assertEquals(mapOf("key" to "secret"), credentials.values)
                assertTrue(store.values.value[key]!!.contains("\"version\":4"))
            }
            assertEquals(AssistantReasoningEffort.LOW, original.reasoningEffort)
        }
    }

    @Test
    fun unknownModelsAndGatewaysDoNotPersistOrApplyEffort() = runTest {
        for ((url, model) in listOf(
            "https://gateway.example/v1/" to "gpt-6.1-terra",
            "https://api.openai.com/v1/" to "unknown-model",
            "https://api.openai.com.evil.example/v1/" to "gpt-6.1-terra",
        )) {
            val store = Store()
            val repository = AssistantSettingsRepository(dataStore = store, credentialStore = Credentials())
            repository.save(
                baseUrl = url,
                modelId = model,
                apiKey = "secret",
                reasoningEffort = AssistantReasoningEffort.MAX,
            )
            assertNull(repository.connection().reasoningEffort)
            assertEquals(AssistantReasoningEffort.LOW, repository.snapshot().reasoningEffort)
            assertFalse(store.values.value[key]!!.contains("max"))
        }
    }

    @Test
    fun invalidV3EffortNeverSilentlyDowngradesOrOverwritesConfiguration() = runTest {
        for (effort in listOf("null", "\"none\"", "\"future\"", "42")) {
            val raw = """{"version":3,"protocol":"openai-compatible","baseUrl":"https://api.openai.com/v1/","modelId":"gpt-6.1-sol","reasoningEffort":$effort}"""
            val store = Store().apply { values.value = mutablePreferencesOf(key to raw) }
            val repository = AssistantSettingsRepository(dataStore = store, credentialStore = Credentials())
            assertEquals(AssistantConfigurationReadError.CORRUPT, repository.snapshot().readError)
            assertFails { repository.connection() }
            assertEquals(raw, store.values.value[key])
        }
    }

    @Test
    fun unreadableConfigurationsRemainIntactAndCannotBeOverwrittenBySave() = runTest {
        val samples = mapOf(
            "{bad" to AssistantConfigurationReadError.CORRUPT,
            "{}" to AssistantConfigurationReadError.CORRUPT,
            """{"version":"oops"}""" to AssistantConfigurationReadError.CORRUPT,
            """{"version":5,"future":"preserve"}""" to AssistantConfigurationReadError.NEWER_FORMAT,
            """{"version":2,"baseUrl":"https://example.org/v1/","modelId":"model","protocol":"future"}""" to AssistantConfigurationReadError.UNKNOWN_PROTOCOL,
            """{"version":2,"baseUrl":"https://example.org/v1/","modelId":"model"}""" to AssistantConfigurationReadError.UNKNOWN_PROTOCOL,
        )
        for ((raw, error) in samples) {
            val store = Store().apply { values.value = mutablePreferencesOf(key to raw) }
            val credentials = Credentials()
            val repository = AssistantSettingsRepository(dataStore = store, credentialStore = credentials)
            assertEquals(error, repository.snapshot().readError)
            assertFalse(repository.snapshot().configured)
            assertFails { repository.connection() }
            assertFails {
                repository.save(
                    baseUrl = "https://new.example/v1/", modelId = "model", apiKey = "secret",
                )
            }
            repository.reload()
            assertEquals(error, repository.snapshot().readError)
            assertEquals(raw, store.values.value[key])
            assertTrue(credentials.values.isEmpty())
            repository.clearUnreadable()
            assertNull(repository.snapshot().readError)
            assertNull(store.values.value[key])
        }
    }

    @Test
    fun migrationRetainsCustomAndUnknownProviderIds() = runTest {
        val raw = """{"version":1,"configId":"stable","revision":7,"baseUrl":"https://api.anthropic.com/v1/","modelId":"old-manual-model","credentialId":"key","consentFor":"https://api.anthropic.com/v1/","consentVersion":1}"""
        val store = Store().apply { values.value = mutablePreferencesOf(key to raw) }
        val credentials = Credentials().apply { values["key"] = "secret" }
        val repository = AssistantSettingsRepository(dataStore = store, credentialStore = credentials)
        assertEquals(AssistantProtocol.OPENAI_COMPATIBLE, repository.connection().protocol)
        assertTrue(store.values.value[key]!!.contains("\"version\":4"))
        for (provider in listOf("custom", "future-provider")) {
            repository.save(
                baseUrl = "https://api.anthropic.com/v1/", modelId = "old-manual-model", apiKey = null,
                editing = repository.snapshot(), providerId = provider,
            )
            val saved = repository.snapshot()
            assertEquals(provider, saved.providerId)
            assertEquals("stable", saved.configId)
            assertEquals("old-manual-model", saved.modelId)
            assertTrue(store.values.value[key]!!.contains("\"version\":4"))
        }
        assertEquals(9, repository.snapshot().revision)
    }

    @Test
    fun protocolChangesRequireNewKeyAndFailedCommitPreservesPreviousConnection() = runTest {
        val store = Store()
        val credentials = Credentials()
        val repository = AssistantSettingsRepository(dataStore = store, credentialStore = credentials)
        repository.save(
            baseUrl = "https://example.org/v1", modelId = "model", apiKey = "old",
        )
        val fixed = repository.connection()
        assertFails {
            repository.save(
                editing = repository.snapshot(),
                baseUrl = "https://example.org/v1", modelId = "model", apiKey = null,

                protocol = AssistantProtocol.ANTHROPIC_MESSAGES,
            )
        }
        store.failWrite = true
        assertFails {
            repository.save(
                editing = repository.snapshot(),
                baseUrl = "https://example.org/v1", modelId = "model", apiKey = "new",

                protocol = AssistantProtocol.ANTHROPIC_MESSAGES,
            )
        }
        assertEquals("old", repository.connection().apiKey)
        assertFalse(credentials.values.containsValue("new"))
        store.failWrite = false
        repository.save(
            editing = repository.snapshot(),
            baseUrl = "https://example.org/v1", modelId = "model", apiKey = "new",

            protocol = AssistantProtocol.ANTHROPIC_MESSAGES,
        )
        assertEquals(AssistantProtocol.ANTHROPIC_MESSAGES, repository.connection().protocol)
        repository.delete(id = repository.snapshot().configId!!, replacementId = null)
        assertEquals("old", fixed.apiKey)
        assertEquals(AssistantProtocol.OPENAI_COMPATIBLE, fixed.protocol)
    }

    @Test
    fun legacyTemporaryKeyRequiresOnePersistentKeyAfterUpgrade() = runTest {
        val raw = """{"version":3,"protocol":"openai-compatible","baseUrl":"https://example.org/","modelId":"legacy"}"""
        val store = Store().apply { values.value = mutablePreferencesOf(key to raw) }
        val credentials = Credentials()
        val repository = AssistantSettingsRepository(dataStore = store, credentialStore = credentials)
        val migrated = repository.snapshot()
        assertEquals("legacy", migrated.name)
        assertFalse(migrated.hasApiKey)
        repository.save(
            baseUrl = migrated.baseUrl,
            modelId = migrated.modelId,
            apiKey = "persistent",
            editing = migrated,
        )
        val restarted = AssistantSettingsRepository(dataStore = store, credentialStore = credentials)
        assertEquals(migrated.configId, restarted.snapshot().configId)
        assertEquals("persistent", restarted.connection().apiKey)
    }

    @Test
    fun storageFailureAndFailedReloadNeverWriteEmptyConfiguration() = runTest {
        val raw = """{"version":9}"""
        val store = Store().apply {
            values.value = mutablePreferencesOf(key to raw)
            failRead = true
        }
        val repository = AssistantSettingsRepository(dataStore = store, credentialStore = Credentials())
        repeat(2) {
            assertEquals(AssistantConfigurationReadError.STORAGE, repository.snapshot().readError)
            repository.reload()
            assertEquals(raw, store.values.value[key])
        }
        store.failRead = false
        repository.reload()
        assertEquals(AssistantConfigurationReadError.NEWER_FORMAT, repository.snapshot().readError)
    }

    @Test
    fun protocolEndpointsPreservePrefixesAndStripOnlyTheirOwnOperation() {
        val samples = listOf(
            Triple(AssistantProtocol.OPENAI_COMPATIBLE, "https://gateway.example/prefix/v1/chat/completions", "chat/completions"),
            Triple(AssistantProtocol.ANTHROPIC_MESSAGES, "https://gateway.example/prefix/v1/messages", "messages"),
            Triple(AssistantProtocol.GEMINI_GENERATE_CONTENT, "https://gateway.example/prefix/v1beta/models/model:generateContent", "models/model:generateContent"),
        )
        for ((protocol, input, suffix) in samples) {
            val base = normalizeAssistantBaseUrl(input = input, protocol = protocol)
            assertEquals(input, base + assistantEndpointPath(baseUrl = base, modelId = "model", protocol = protocol))
            assertTrue(input.endsWith(suffix))
        }
        assertEquals(
            "https://gateway.example/v1/chat/completions/",
            normalizeAssistantBaseUrl(input = "https://gateway.example/v1/chat/completions", protocol = AssistantProtocol.ANTHROPIC_MESSAGES),
        )
        assertNull(AssistantProtocol.fromId("unknown"))
    }
}
