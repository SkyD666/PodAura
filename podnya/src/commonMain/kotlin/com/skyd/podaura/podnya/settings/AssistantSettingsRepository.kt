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
import com.skyd.podaura.podnya.core.AssistantProtocol
import com.skyd.podaura.podnya.core.AssistantReasoningEffort
import com.skyd.podaura.podnya.core.assistantEndpointPath
import com.skyd.podaura.podnya.core.normalizeAssistantBaseUrl
import com.skyd.podaura.podnya.core.supportsAssistantReasoningEffort
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.uuid.Uuid

enum class AssistantConfigurationReadError { NEWER_FORMAT, UNKNOWN_PROTOCOL, CORRUPT, STORAGE }

data class AssistantSettings(
    val configId: String? = null,
    val revision: Long = 0,
    val baseUrl: String = "",
    val modelId: String = "",
    val hasApiKey: Boolean = false,
    val sessionOnly: Boolean = false,
    val consentAccepted: Boolean = false,
    val providerId: String? = null,
    val protocol: AssistantProtocol = AssistantProtocol.OPENAI_COMPATIBLE,
    val reasoningEffort: AssistantReasoningEffort = AssistantReasoningEffort.LOW,
    val readError: AssistantConfigurationReadError? = null,
) {
    val configured: Boolean get() = readError == null && baseUrl.isNotBlank() && modelId.isNotBlank() && hasApiKey
    val endpoint: String
        get() = if (baseUrl.isEmpty()) "" else baseUrl +
                assistantEndpointPath(baseUrl = baseUrl, modelId = modelId, protocol = protocol)
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
        val providerId: String? = null,
        val protocol: String? = null,
        val reasoningEffort: String? = null,
    )

    private val key = stringPreferencesKey("podnya.configuration.v1")
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    // A key kept here is never written to DataStore or SavedState.
    private class TemporaryKey(
        val baseUrl: String,
        val protocol: AssistantProtocol,
        val value: String,
    )

    private val temporaryKey = MutableStateFlow<TemporaryKey?>(null)

    private val reload = MutableStateFlow(0L)

    /** Retry re-subscribes even after DataStore has failed; unreadable values are never rewritten. */
    fun reload() {
        reload.value += 1
    }

    val settings: Flow<AssistantSettings> = reload.flatMapLatest {
        combine(dataStore.data, temporaryKey) { preferences, temporary ->
            try {
                val decoded = decode(preferences[key])
                val stored = if (decoded.baseUrl.isNotEmpty() && decoded.configId == null) {
                    mutex.withLock { identified() }
                } else decoded
                stored.settings(temporary)
            } catch (failure: ConfigurationReadException) {
                AssistantSettings(readError = failure.reason)
            }
        }.catch { failure ->
            if (failure is CancellationException) throw failure
            emit(AssistantSettings(readError = AssistantConfigurationReadError.STORAGE))
        }
    }

    private fun Stored.settings(temporary: TemporaryKey?): AssistantSettings {
        val protocol = connectionProtocol()
        val temporaryMatches = temporary?.baseUrl == baseUrl && temporary.protocol == protocol
        return AssistantSettings(
            configId = configId,
            revision = revision,
            baseUrl = baseUrl,
            modelId = modelId,
            hasApiKey = credentialId != null || temporaryMatches,
            sessionOnly = temporaryMatches,
            consentAccepted = baseUrl.isNotBlank() && consentFor == baseUrl && consentVersion == 1,
            providerId = providerId,
            protocol = protocol,
            reasoningEffort = reasoningEffort?.let(AssistantReasoningEffort::fromId)
                ?: AssistantReasoningEffort.LOW,
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
        providerId: String? = null,
        protocol: AssistantProtocol = AssistantProtocol.OPENAI_COMPATIBLE,
        reasoningEffort: AssistantReasoningEffort = AssistantReasoningEffort.LOW,
    ) = mutex.withLock {
        val normalized = normalizeAssistantBaseUrl(input = baseUrl, protocol = protocol)
        val model = modelId.trim()
        if (model.isEmpty() || model.length > 200 || model.any(Char::isISOControl)) {
            throw AssistantException(AssistantError.CONFIGURATION_REQUIRED)
        }
        val previous = decode(dataStore.data.first()[key])
        val sameRecipient =
            previous.baseUrl == normalized && previous.connectionProtocol() == protocol
        val newSecret = apiKey?.trim()?.takeIf(String::isNotEmpty)
        if (newSecret != null && (newSecret.length > 8192 || newSecret.any(Char::isISOControl))) {
            throw AssistantException(AssistantError.CONFIGURATION_REQUIRED)
        }
        val previousTemporary =
            temporaryKey.value?.takeIf { it.baseUrl == normalized && it.protocol == protocol }
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
            version = 3,
            providerId = providerId,
            protocol = protocol.id,
            reasoningEffort = reasoningEffort.id.takeIf {
                supportsAssistantReasoningEffort(
                    baseUrl = normalized,
                    modelId = model,
                    protocol = protocol
                )
            },
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
        temporaryKey.value = if (sessionOnly) TemporaryKey(
            baseUrl = normalized,
            protocol = protocol,
            value = secret
        ) else null
        if (previous.credentialId != null && previous.credentialId != newCredentialId) {
            cleanup(previous.credentialId)
        }
        stored.settings(temporaryKey.value)
    }

    suspend fun connection(): AssistantConnection {
        val current = snapshot()
        return resolveConnection(
            configId = current.configId
                ?: throw AssistantException(AssistantError.CONFIGURATION_REQUIRED),
            expectedRevision = current.revision,
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
            val protocol = stored.connectionProtocol()
            val secret = temporaryKey.value?.takeIf {
                it.baseUrl == stored.baseUrl && it.protocol == protocol
            }?.value ?: stored.credentialId?.let {
                try {
                    credentialStore.get(it)
                } catch (_: CredentialStorageException) {
                    throw AssistantException(AssistantError.CREDENTIAL_STORAGE_UNAVAILABLE)
                }
            }
            if (secret.isNullOrBlank()) throw AssistantException(AssistantError.CONFIGURATION_REQUIRED)
            AssistantConnection(
                baseUrl = normalizeAssistantBaseUrl(input = stored.baseUrl, protocol = protocol),
                modelId = stored.modelId,
                apiKey = secret,
                protocol = protocol,
                reasoningEffort = stored.reasoningEffort?.let(AssistantReasoningEffort::fromId)
                    ?: AssistantReasoningEffort.LOW,
            )
        }

    suspend fun clear() = mutex.withLock {
        val raw = dataStore.data.first()[key]
        val previous = try {
            decode(raw)
        } catch (_: ConfigurationReadException) {
            null // Explicit clear may remove unreadable data; never trust an unknown credential reference.
        }
        dataStore.edit { it.remove(key) }
        temporaryKey.value = null
        previous?.credentialId?.let { cleanup(it) }
        AssistantSettings()
    }

    // Migrate the old single configuration in place, without replacing its credentials.
    private suspend fun identified(): Stored {
        val current = decode(dataStore.data.first()[key])
        if (current.baseUrl.isEmpty() || current.configId != null) return current
        val upgraded = current.copy(configId = Uuid.random().toString(), revision = 1)
        dataStore.edit { it[key] = json.encodeToString(upgraded) }
        return upgraded
    }

    private class ConfigurationReadException(val reason: AssistantConfigurationReadError) :
        Exception("Unreadable assistant configuration")

    private fun Stored.connectionProtocol(): AssistantProtocol = if (version == 1) {
        AssistantProtocol.OPENAI_COMPATIBLE
    } else {
        protocol?.let(AssistantProtocol::fromId)
            ?: throw ConfigurationReadException(AssistantConfigurationReadError.UNKNOWN_PROTOCOL)
    }

    private fun decode(value: String?): Stored {
        if (value == null) return Stored()
        try {
            val objectValue = json.parseToJsonElement(value).jsonObject
            val version = if ("version" in objectValue) {
                objectValue.getValue("version").jsonPrimitive.intOrNull
                    ?: throw ConfigurationReadException(AssistantConfigurationReadError.CORRUPT)
            } else 1
            if (version > 3) throw ConfigurationReadException(AssistantConfigurationReadError.NEWER_FORMAT)
            if (version !in 1..3) throw ConfigurationReadException(AssistantConfigurationReadError.CORRUPT)
            val stored = json.decodeFromString<Stored>(value)
            val protocol = stored.connectionProtocol()
            val supported = supportsAssistantReasoningEffort(
                baseUrl = stored.baseUrl,
                modelId = stored.modelId,
                protocol = protocol,
            )
            val validEffort = if (supported) {
                stored.reasoningEffort?.let(AssistantReasoningEffort::fromId) != null
            } else {
                stored.reasoningEffort == null
            }
            if (version == 3 && !validEffort) {
                throw ConfigurationReadException(AssistantConfigurationReadError.CORRUPT)
            }
            if (stored.baseUrl.isBlank() || stored.modelId.isBlank() || stored.revision < 0 ||
                stored.modelId.length > 200 || stored.modelId.any(Char::isISOControl)
            ) throw ConfigurationReadException(AssistantConfigurationReadError.CORRUPT)
            normalizeAssistantBaseUrl(input = stored.baseUrl, protocol = protocol)
            return if (version < 3) stored.copy(reasoningEffort = null) else stored
        } catch (failure: ConfigurationReadException) {
            throw failure
        } catch (_: Exception) {
            throw ConfigurationReadException(AssistantConfigurationReadError.CORRUPT)
        }
    }

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
