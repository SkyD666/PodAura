package com.skyd.podaura.podnya.settings

import com.skyd.podaura.podnya.core.normalizeAssistantBaseUrl
import com.skyd.podaura.podnya.core.assistantEndpointPath
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.skyd.fundation.credential.CredentialStorageException
import com.skyd.fundation.credential.CredentialStore
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest

class AssistantSettingsRepositoryTest {
    private class PreferencesStore : DataStore<Preferences> {
        override val data = MutableStateFlow(emptyPreferences())
        var failWrite = false
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            if (failWrite) error("disk unavailable")
            return transform(data.value).also { data.value = it }
        }
    }
    private class Credentials : CredentialStore {
        val values = mutableMapOf<String, String>()
        var available = true
        override suspend fun put(id: String, secret: String) {
            if (!available) throw CredentialStorageException()
            values[id] = secret
        }
        override suspend fun get(id: String): String? {
            if (!available) throw CredentialStorageException()
            return values[id]
        }
        override suspend fun delete(id: String) { values.remove(id) }
    }

    @Test
    fun legacyConfigurationCanSendImmediatelyWithItsSavedKey() = runTest {
        val preferences = PreferencesStore()
        val credentials = Credentials().apply { values["saved-key"] = "secret" }
        preferences.data.value = androidx.datastore.preferences.core.mutablePreferencesOf(
            androidx.datastore.preferences.core.stringPreferencesKey("podnya.configuration.v1") to
                """{"version":1,"baseUrl":"https://example.org/v1/","modelId":"model","credentialId":"saved-key","consentFor":"https://example.org/v1/"}"""
        )
        val repository = AssistantSettingsRepository(preferences, credentials)
        assertTrue(repository.snapshot().configured)
        assertEquals("secret", repository.connection().apiKey)
        assertEquals(1, credentials.values.size)
    }

    @Test
    fun configurationIdentitySurvivesEditsAndChangesAfterClearAndOldRevisionsAreRejected() = runTest {
        val preferences = PreferencesStore()
        val credentials = Credentials()
        val repository = AssistantSettingsRepository(preferences, credentials)
        repository.save(baseUrl = "https://example.org/v1", modelId = "first", apiKey = "key-one")
        val first = repository.snapshot()
        repository.save(baseUrl = "https://example.org/v1", modelId = "second", apiKey = "key-two", editing = first)
        val second = repository.snapshot()
        assertEquals(first.configId, second.configId)
        assertTrue(second.revision > first.revision)
        assertEquals(AssistantError.CONFIGURATION_CHANGED, assertFailsWith<AssistantException> {
            repository.resolveConnection(first.configId!!, first.revision)
        }.error)
        val fixed = repository.resolveConnection(second.configId!!, second.revision)
        repository.delete(id = second.configId, replacementId = null)
        repository.save(baseUrl = "https://example.org/v1", modelId = "third", apiKey = "key-three")
        assertTrue(repository.snapshot().configId != second.configId)
        assertEquals("second", fixed.modelId)
        assertEquals("key-two", fixed.apiKey)
    }

    @Test
    fun endpointKeepsTheUsersPrefixAndRejectsCredentialsOrPlaintext() {
        mapOf(
            "https://example.org" to "https://example.org/",
            "https://example.org/v1/" to "https://example.org/v1/",
            "https://example.org/proxy/v2/chat/completions" to "https://example.org/proxy/v2/",
        ).forEach { (input, output) -> assertEquals(output, normalizeAssistantBaseUrl(input)) }
        listOf("http://localhost:11434/v1", "https://key@example.org/v1", "https://example.org/v1?key=secret",
            "https://example.org/#fragment", "example.org/v1", "https://example.org\\bad").forEach {
            assertFailsWith<AssistantException> { normalizeAssistantBaseUrl(it) }
        }
    }

    @Test
    fun officialAstraUsesResponsesWithoutChangingOtherConfigurations() {
        val base = normalizeAssistantBaseUrl("https://api.openai.com/v1/chat/completions")
        assertEquals("https://api.openai.com/v1/responses", AssistantSettings(baseUrl = base, modelId = "gpt-6-astra").endpoint)
        assertEquals("responses", assistantEndpointPath(base, " gpt-6-astra-2026-09-03 "))
        assertEquals("chat/completions", assistantEndpointPath(base, "another-model"))
        assertEquals("chat/completions", assistantEndpointPath("https://api.openai.com.example.org/v1/", "gpt-6-astra"))
        assertEquals("chat/completions", assistantEndpointPath("https://proxy.example.org/prefix/v1/", "gpt-6-astra"))
    }

    @Test
    fun failedConfigWriteKeepsOldKeyAndCleansNewKey() = runTest {
        val preferences = PreferencesStore()
        val credentials = Credentials()
        val repository = AssistantSettingsRepository(preferences, credentials)
        repository.save(baseUrl = "https://example.org/v1", modelId = "model", apiKey = "secret-one")
        preferences.failWrite = true
        assertFailsWith<IllegalStateException> {
            repository.save(baseUrl = "https://example.org/v1", modelId = "model", apiKey = "secret-two")
        }
        assertEquals("secret-one", repository.connection().apiKey)
        assertEquals(listOf("secret-one"), credentials.values.values.toList())
        assertFalse(preferences.data.value.asMap().values.joinToString().contains("secret-one"))
        assertFalse(repository.connection().toString().contains("secret-one"))
    }

    @Test
    fun inaccessibleSavedKeyHasAStorageErrorRatherThanANetworkError() = runTest {
        val credentials = Credentials()
        val repository = AssistantSettingsRepository(PreferencesStore(), credentials)
        repository.save(baseUrl = "https://example.org/v1", modelId = "model", apiKey = "secret")
        credentials.available = false
        assertEquals(AssistantError.CREDENTIAL_STORAGE_UNAVAILABLE, assertFailsWith<AssistantException> {
            repository.connection()
        }.error)
    }

    @Test
    fun editingWithoutAReplacementKeyRequiresTheStoredKeyToStillExist() = runTest {
        val credentials = Credentials()
        val repository = AssistantSettingsRepository(PreferencesStore(), credentials)
        val original = repository.save(baseUrl = "https://example.org", modelId = "model", apiKey = "secret")
        credentials.values.clear()
        val failure = assertFailsWith<AssistantException> {
            repository.save(baseUrl = original.baseUrl, modelId = "edited", apiKey = null, editing = original)
        }
        assertEquals(AssistantError.CONFIGURATION_REQUIRED, failure.error)
        assertEquals(original, repository.snapshot())
    }

    @Test
    fun keysOverTheInputLimitHaveALengthError() = runTest {
        val repository = AssistantSettingsRepository(PreferencesStore(), Credentials())
        val failure = assertFailsWith<AssistantException> {
            repository.save(baseUrl = "https://example.org", modelId = "model", apiKey = "x".repeat(8193))
        }
        assertEquals(AssistantError.CREDENTIAL_TOO_LONG, failure.error)
        assertFalse(repository.snapshot().configured)
    }

    @Test
    fun savedProfilesKeepTheirOwnKeysAndSelectionAcrossRestarts() = runTest {
        val preferences = PreferencesStore()
        val credentials = Credentials()
        val repository = AssistantSettingsRepository(preferences, credentials)
        val first = repository.save(baseUrl = "https://same.example", modelId = "model", apiKey = "first", name = "Work")
        val second = repository.save(baseUrl = "https://same.example", modelId = "model", apiKey = "second", name = "Home")
        assertEquals(first.configId, repository.snapshot().configId)
        repository.select(requireNotNull(second.configId))
        assertFailsWith<AssistantException> {
            repository.delete(id = requireNotNull(first.configId), replacementId = second.configId)
        }
        val restarted = AssistantSettingsRepository(preferences, credentials)
        assertEquals("Home", restarted.snapshot().name)
        assertEquals("second", restarted.connection().apiKey)
        assertFailsWith<AssistantException> {
            restarted.delete(id = second.configId, replacementId = null)
        }
        restarted.delete(id = second.configId, replacementId = first.configId)
        assertEquals("first", restarted.connection().apiKey)
        assertEquals(listOf("first"), credentials.values.values.toList())
    }
}
