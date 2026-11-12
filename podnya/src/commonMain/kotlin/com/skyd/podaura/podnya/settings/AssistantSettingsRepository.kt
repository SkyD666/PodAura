package com.skyd.podaura.podnya.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.fundation.credential.CredentialStorageException
import com.skyd.fundation.credential.CredentialStore
import com.skyd.podaura.podnya.core.AssistantConnection
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantException
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.uuid.Uuid

data class AssistantSettings(
    val configId: String? = null,
    val revision: Long = 0,
    val baseUrl: String = "",
    val modelId: String = "",
    val hasApiKey: Boolean = false,
    val sessionOnly: Boolean = false,
    val consentAccepted: Boolean = false,
) {
    val configured: Boolean get() = baseUrl.isNotBlank() && modelId.isNotBlank() && hasApiKey
    val endpoint: String
        get() = if (baseUrl.isEmpty()) "" else baseUrl +
                assistantEndpointPath(baseUrl, modelId)
}

/** Astra needs Responses for tools; other compatible-service configurations keep their protocol. */
fun assistantEndpointPath(baseUrl: String, modelId: String): String {
    val model = modelId.trim()
    val isAstra = model == "gpt-6-astra" || model.startsWith("gpt-6-astra-")
    val officialHost =
        runCatching { Url(baseUrl).host.equals("api.openai.com", ignoreCase = true) }.getOrDefault(
            false
        )
    return if (isAstra && officialHost) "responses" else "chat/completions"
}

/** One versioned preference outside the application's exported @Preference registry. */
class AssistantSettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val credentialStore: CredentialStore,
) {
    @Serializable
    private data class Stored(
        val version: Int = 1,
        val configId: String? = null,
        val revision: Long = 0,
        val baseUrl: String = "",
        val modelId: String = "",
        val credentialId: String? = null,
        val consentFor: String? = null,
        val consentVersion: Int = 0,
    )

    private val key = stringPreferencesKey("podnya.configuration.v1")
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    // A key kept here is never written to DataStore or SavedState.
    private class TemporaryKey(val baseUrl: String, val value: String)

    private val temporaryKey = MutableStateFlow<TemporaryKey?>(null)

    val settings: Flow<AssistantSettings> =
        combine(dataStore.data, temporaryKey) { preferences, temporary ->
            val stored =
                if (decode(preferences[key]).let { it.baseUrl.isNotEmpty() && it.configId == null }) {
                    mutex.withLock { identified() }
                } else decode(preferences[key])
            AssistantSettings(
                configId = stored.configId,
                revision = stored.revision,
                baseUrl = stored.baseUrl,
                modelId = stored.modelId,
                hasApiKey = stored.credentialId != null || temporary?.baseUrl == stored.baseUrl,
                sessionOnly = temporary?.baseUrl == stored.baseUrl,
                consentAccepted = stored.baseUrl.isNotBlank() && stored.consentFor == stored.baseUrl && stored.consentVersion == 1,
            )
        }

    suspend fun snapshot(): AssistantSettings = settings.first()

    /** A blank/null key keeps the current key only when the recipient has not changed. */
    suspend fun save(
        baseUrl: String,
        modelId: String,
        apiKey: String?,
        sessionOnly: Boolean,
        consentAccepted: Boolean,
    ) = mutex.withLock {
        val normalized = normalizeAssistantBaseUrl(baseUrl)
        val model = modelId.trim()
        if (model.isEmpty() || model.length > 200 || model.any(Char::isISOControl)) {
            throw AssistantException(AssistantError.CONFIGURATION_REQUIRED)
        }
        val previous = decode(dataStore.data.first()[key])
        val sameRecipient = previous.baseUrl == normalized
        val newSecret = apiKey?.trim()?.takeIf(String::isNotEmpty)
        if (newSecret != null && (newSecret.length > 8192 || newSecret.any(Char::isISOControl))) {
            throw AssistantException(AssistantError.CONFIGURATION_REQUIRED)
        }
        val previousTemporary = temporaryKey.value?.takeIf { it.baseUrl == normalized }
        val secret = newSecret ?: if (sameRecipient) {
            previousTemporary?.value ?: previous.credentialId?.let { credentialStore.get(it) }
        } else null
        if (secret.isNullOrEmpty()) throw AssistantException(AssistantError.CONFIGURATION_REQUIRED)

        val newCredentialId = when {
            sessionOnly -> null
            sameRecipient && newSecret == null && previous.credentialId != null -> previous.credentialId
            else -> "podnya-${Uuid.random()}"
        }
        val wroteCredential = newCredentialId != null && newCredentialId != previous.credentialId
        if (wroteCredential) credentialStore.put(newCredentialId, secret)
        val stored = Stored(
            configId = previous.configId ?: Uuid.random().toString(),
            revision = previous.revision + 1,
            baseUrl = normalized,
            modelId = model,
            credentialId = newCredentialId,
            consentFor = normalized.takeIf { consentAccepted },
            consentVersion = if (consentAccepted) 1 else 0,
        )
        try {
            dataStore.edit { it[key] = json.encodeToString(stored) }
        } catch (failure: Throwable) {
            if (wroteCredential) cleanup(newCredentialId)
            throw failure
        }
        temporaryKey.value = if (sessionOnly) TemporaryKey(normalized, secret) else null
        if (previous.credentialId != null && previous.credentialId != newCredentialId) {
            cleanup(previous.credentialId)
        }
    }

    suspend fun connection(): AssistantConnection {
        val current = snapshot()
        return resolveConnection(
            current.configId ?: throw AssistantException(AssistantError.CONFIGURATION_REQUIRED),
            current.revision
        )
    }

    suspend fun resolveConnection(configId: String, expectedRevision: Long): AssistantConnection =
        mutex.withLock {
            val stored = identified()
            if (stored.configId != configId || stored.revision != expectedRevision) {
                throw AssistantException(AssistantError.CONFIGURATION_CHANGED)
            }
            if (stored.baseUrl.isEmpty() || stored.modelId.isEmpty()) {
                throw AssistantException(AssistantError.CONFIGURATION_REQUIRED)
            }
            if (stored.consentFor != stored.baseUrl || stored.consentVersion != 1) {
                throw AssistantException(AssistantError.DISCLOSURE_REQUIRED)
            }
            val secret = temporaryKey.value?.takeIf { it.baseUrl == stored.baseUrl }?.value
                ?: stored.credentialId?.let {
                    try {
                        credentialStore.get(it)
                    } catch (_: CredentialStorageException) {
                        throw AssistantException(AssistantError.CREDENTIAL_STORAGE_UNAVAILABLE)
                    }
                }
            if (secret.isNullOrBlank()) throw AssistantException(AssistantError.CONFIGURATION_REQUIRED)
            AssistantConnection(normalizeAssistantBaseUrl(stored.baseUrl), stored.modelId, secret)
        }

    suspend fun clear() = mutex.withLock {
        val previous = decode(dataStore.data.first()[key])
        dataStore.edit { it.remove(key) }
        temporaryKey.value = null
        previous.credentialId?.let { cleanup(it) }
    }

    // Migrate the old single configuration in place, without replacing its credentials.
    private suspend fun identified(): Stored {
        val current = decode(dataStore.data.first()[key])
        if (current.baseUrl.isEmpty() || current.configId != null) return current
        val upgraded = current.copy(configId = Uuid.random().toString(), revision = 1)
        dataStore.edit { it[key] = json.encodeToString(upgraded) }
        return upgraded
    }

    private fun decode(value: String?): Stored = value?.let {
        runCatching { json.decodeFromString<Stored>(it) }.getOrNull()?.takeIf { it.version == 1 }
    } ?: Stored()

    private suspend fun cleanup(id: String) {
        try {
            credentialStore.delete(id)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // The new reference is already durable; cleanup must not revert it to the old key.
        }
    }
}

/** HTTPS only in v1: do not silently depend on mobile cleartext-network exceptions. */
fun normalizeAssistantBaseUrl(input: String): String {
    val raw = input.trim()
    val url = try {
        Url(raw)
    } catch (_: Exception) {
        throw AssistantException(AssistantError.INVALID_ADDRESS)
    }
    if (raw.length > 2048 || raw.any(Char::isISOControl) ||
        !raw.startsWith("https://", ignoreCase = true) || url.protocol != URLProtocol.HTTPS ||
        url.host.isBlank() || url.user != null || url.password != null ||
        raw.contains('?') || raw.contains('#') || raw.contains('\\')
    ) throw AssistantException(AssistantError.INVALID_ADDRESS)
    var normalized = url.toString().trimEnd('/')
    if (normalized.endsWith("/chat/completions")) normalized =
        normalized.removeSuffix("/chat/completions")
    return "$normalized/"
}
