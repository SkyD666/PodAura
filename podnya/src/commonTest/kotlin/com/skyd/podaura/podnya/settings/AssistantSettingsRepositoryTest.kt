package com.skyd.podaura.podnya.settings

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
    fun articleOnlyConsentNeedsUpdatingWithoutLosingTheSavedConfiguration() = runTest {
        val preferences = PreferencesStore()
        val credentials = Credentials().apply { values["saved-key"] = "secret" }
        preferences.data.value = androidx.datastore.preferences.core.mutablePreferencesOf(
            androidx.datastore.preferences.core.stringPreferencesKey("podnya.configuration.v1") to
                """{"version":1,"baseUrl":"https://example.org/v1/","modelId":"model","credentialId":"saved-key","consentFor":"https://example.org/v1/"}"""
        )
        val repository = AssistantSettingsRepository(preferences, credentials)
        assertTrue(repository.snapshot().configured)
        assertFalse(repository.snapshot().consentAccepted)
        assertEquals(AssistantError.DISCLOSURE_REQUIRED, assertFailsWith<AssistantException> { repository.connection() }.error)
        repository.save("https://example.org/v1/", "model", null, false, true)
        assertTrue(repository.snapshot().consentAccepted)
        assertEquals("secret", repository.connection().apiKey)
        assertEquals(1, credentials.values.size)
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
        repository.save("https://example.org/v1", "model", "secret-one", false, true)
        preferences.failWrite = true
        assertFailsWith<IllegalStateException> {
            repository.save("https://example.org/v1", "model", "secret-two", false, true)
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
        repository.save("https://example.org/v1", "model", "secret", false, true)
        credentials.available = false
        assertEquals(AssistantError.CREDENTIAL_STORAGE_UNAVAILABLE, assertFailsWith<AssistantException> {
            repository.connection()
        }.error)
    }

    @Test
    fun newRecipientNeverInheritsAKeyOrConsentAndTemporaryKeysDoNotSurviveRestart() = runTest {
        val preferences = PreferencesStore()
        val credentials = Credentials().apply { available = false }
        val repository = AssistantSettingsRepository(preferences, credentials)
        assertFailsWith<CredentialStorageException> {
            repository.save("https://first.example", "model", "temporary", false, true)
        }
        repository.save("https://first.example", "model", "temporary", true, true)
        assertTrue(repository.snapshot().sessionOnly)
        assertEquals("temporary", repository.connection().apiKey)
        assertFailsWith<AssistantException> {
            repository.save("https://second.example", "model", null, true, true)
        }
        repository.save("https://second.example", "model", "second", true, false)
        assertEquals(AssistantError.DISCLOSURE_REQUIRED, assertFailsWith<AssistantException> { repository.connection() }.error)
        val restarted = AssistantSettingsRepository(preferences, credentials)
        assertFalse(restarted.snapshot().hasApiKey)
        assertTrue(credentials.values.isEmpty())
    }
}
