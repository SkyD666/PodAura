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
import com.skyd.podaura.podnya.core.normalizeAssistantBaseUrl
import com.skyd.podaura.podnya.core.supportsAssistantReasoningEffort
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.uuid.Uuid

/** One versioned preference outside the application's exported @Preference registry. */
class AssistantSettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val credentialStore: CredentialStore,
) {
    @Serializable
    private data class Stored(
        val configId: String? = null,
        val revision: Long = 0,
        val baseUrl: String = "",
        val modelId: String = "",
        val credentialId: String? = null,
        val name: String = "",
        val providerId: String? = null,
        val protocol: String? = null,
        val reasoningEffort: String? = null,
    )

    private val key = stringPreferencesKey("podnya.configuration.v1")
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    @Serializable
    private data class Catalog(
        val version: Int,
        val profiles: List<Stored>,
        val activeProfileId: String?,
    ) {
        fun profile(id: String?) = profiles.find { it.configId == id }
            ?: throw AssistantException(AssistantError.CONFIGURATION_CHANGED)
    }

    private val reloads = MutableStateFlow(0L)
    fun reload() {
        reloads.value += 1
    }

    val profiles: Flow<AssistantProfiles> = reloads.flatMapLatest {
        dataStore.data.map { it[key] }.distinctUntilChanged().map { raw ->
            try {
                val decoded = decode(raw)
                val catalog = if (decoded.version < 4) {
                    mutex.withLock { read() }
                } else decoded
                AssistantProfiles(
                    profiles = catalog.profiles.map { it.settings() },
                    activeProfileId = catalog.activeProfileId,
                )
            } catch (failure: ConfigurationReadException) {
                AssistantProfiles(readError = failure.reason)
            }
        }.catch { failure ->
            if (failure is CancellationException) throw failure
            emit(AssistantProfiles(readError = AssistantConfigurationReadError.STORAGE))
        }
    }

    suspend fun snapshot(): AssistantSettings = profiles.first().active

    private fun Stored.settings() = AssistantSettings(
        configId = configId,
        name = name,
        revision = revision,
        baseUrl = baseUrl,
        modelId = modelId,
        providerId = providerId,
        protocol = connectionProtocol(),
        reasoningEffort = reasoningEffort?.let(AssistantReasoningEffort::fromId)
            ?: AssistantReasoningEffort.LOW,
        hasApiKey = credentialId != null,
    )

    suspend fun save(
        baseUrl: String,
        modelId: String,
        apiKey: String?,
        editing: AssistantSettings = AssistantSettings(),
        name: String = modelId,
        activate: Boolean = false,
        providerId: String? = null,
        protocol: AssistantProtocol = AssistantProtocol.OPENAI_COMPATIBLE,
        reasoningEffort: AssistantReasoningEffort = AssistantReasoningEffort.LOW,
    ): AssistantSettings = mutex.withLock {
        val catalog = read()
        val previous = editing.configId?.let { catalog.profile(it) }
        if (previous != null && previous.revision != editing.revision) {
            throw AssistantException(AssistantError.CONFIGURATION_CHANGED)
        }
        val normalized = normalizeAssistantBaseUrl(input = baseUrl, protocol = protocol)
        val model = modelId.trim()
        val label = name.trim().ifEmpty { model }
        if (!validLabel(model) || !validLabel(label)) throw AssistantException(AssistantError.CONFIGURATION_REQUIRED)
        val replacement = apiKey?.trim()?.takeIf(String::isNotEmpty)
        if (replacement != null && replacement.length > 8192) {
            throw AssistantException(AssistantError.CREDENTIAL_TOO_LONG)
        }
        if (replacement?.any(Char::isISOControl) == true) {
            throw AssistantException(AssistantError.CONFIGURATION_REQUIRED)
        }
        val credentialId = if (replacement != null) {
            "podnya-${Uuid.random()}"
        } else {
            val existing =
                previous?.takeIf { it.baseUrl == normalized && it.connectionProtocol() == protocol }
                    ?.credentialId
                    ?: throw AssistantException(AssistantError.CONFIGURATION_REQUIRED)
            readSecret(existing)
            existing
        }
        val id = previous?.configId ?: Uuid.random().toString()
        val stored = Stored(
            configId = id,
            name = label,
            revision = (previous?.revision ?: 0) + 1,
            baseUrl = normalized,
            modelId = model,
            providerId = providerId,
            protocol = protocol.id,
            reasoningEffort = reasoningEffort.id.takeIf {
                supportsAssistantReasoningEffort(
                    baseUrl = normalized,
                    modelId = model,
                    protocol = protocol
                )
            },
            credentialId = credentialId,
        )
        val updated = if (previous == null) {
            catalog.profiles + stored
        } else {
            catalog.profiles.map { if (it.configId == id) stored else it }
        }
        if (replacement != null) credentialStore.put(credentialId, replacement)
        try {
            write(
                catalog.copy(
                    profiles = updated,
                    activeProfileId = if (activate || catalog.activeProfileId == null) id else catalog.activeProfileId,
                )
            )
        } catch (failure: Throwable) {
            if (replacement != null) cleanup(credentialId)
            throw failure
        }
        previous?.credentialId?.takeIf { it != credentialId }?.let { cleanup(it) }
        stored.settings()
    }

    suspend fun select(id: String) = mutex.withLock {
        val catalog = read()
        catalog.profile(id)
        if (catalog.activeProfileId != id) write(catalog.copy(activeProfileId = id))
    }

    suspend fun delete(id: String, replacementId: String?): AssistantSettings = mutex.withLock {
        val catalog = read()
        val removed = catalog.profile(id)
        val remaining = catalog.profiles.filterNot { it.configId == id }
        val active = when {
            replacementId != null -> {
                if (id != catalog.activeProfileId || remaining.none { it.configId == replacementId }) {
                    throw AssistantException(AssistantError.CONFIGURATION_CHANGED)
                }
                replacementId
            }

            remaining.isEmpty() -> null
            id != catalog.activeProfileId -> catalog.activeProfileId
            else -> throw AssistantException(AssistantError.CONFIGURATION_CHANGED)
        }
        write(catalog.copy(profiles = remaining, activeProfileId = active))
        removed.credentialId?.let { cleanup(it) }
        remaining.find { it.configId == active }?.settings() ?: AssistantSettings()
    }

    suspend fun clearUnreadable() = mutex.withLock {
        val raw = dataStore.data.first()[key]
        try {
            decode(raw)
            throw AssistantException(AssistantError.CONFIGURATION_CHANGED)
        } catch (_: ConfigurationReadException) {
            dataStore.edit { it.remove(key) }
        }
    }

    suspend fun connection(): AssistantConnection = mutex.withLock {
        val catalog = read()
        val id = catalog.activeProfileId
            ?: throw AssistantException(AssistantError.CONFIGURATION_REQUIRED)
        val stored = catalog.profile(id)
        stored.connection()
    }

    suspend fun resolveConnection(configId: String, expectedRevision: Long): AssistantConnection =
        mutex.withLock {
            val stored = read().profile(configId)
            if (stored.revision != expectedRevision) throw AssistantException(AssistantError.CONFIGURATION_CHANGED)
            stored.connection()
        }

    private suspend fun readSecret(credentialId: String?): String {
        val secret = try {
            credentialId?.let { credentialStore.get(it) }
        } catch (_: CredentialStorageException) {
            throw AssistantException(AssistantError.CREDENTIAL_STORAGE_UNAVAILABLE)
        }
        if (secret.isNullOrBlank()) throw AssistantException(AssistantError.CONFIGURATION_REQUIRED)
        return secret
    }

    private suspend fun Stored.connection(): AssistantConnection {
        return AssistantConnection(
            baseUrl = normalizeAssistantBaseUrl(input = baseUrl, protocol = connectionProtocol()),
            modelId = modelId,
            apiKey = readSecret(credentialId),
            protocol = connectionProtocol(),
            reasoningEffort = reasoningEffort?.let(AssistantReasoningEffort::fromId)
                ?: AssistantReasoningEffort.LOW,
        )
    }

    private suspend fun read(): Catalog {
        val catalog = decode(dataStore.data.first()[key])
        if (catalog.version == 4) return catalog
        return catalog.copy(version = 4).also { write(it) }
    }

    private suspend fun write(catalog: Catalog) {
        dataStore.edit { it[key] = json.encodeToString(catalog) }
    }

    private class ConfigurationReadException(val reason: AssistantConfigurationReadError) :
        Exception()

    private fun corrupt(): Nothing =
        throw ConfigurationReadException(AssistantConfigurationReadError.CORRUPT)

    private fun validLabel(value: String) =
        value.isNotBlank() && value.length <= 200 && value.none(Char::isISOControl)

    private fun Stored.connectionProtocol() = protocol?.let(AssistantProtocol::fromId)
        ?: throw ConfigurationReadException(AssistantConfigurationReadError.UNKNOWN_PROTOCOL)

    private fun decode(raw: String?): Catalog {
        if (raw == null) return Catalog(version = 4, profiles = emptyList(), activeProfileId = null)
        try {
            val fields = json.parseToJsonElement(raw).jsonObject
            val version = fields["version"]?.let { it.jsonPrimitive.intOrNull ?: corrupt() } ?: 1
            if (version > 4) throw ConfigurationReadException(AssistantConfigurationReadError.NEWER_FORMAT)
            if (version !in 1..4) corrupt()
            val catalog = if (version == 4) {
                json.decodeFromString<Catalog>(raw)
            } else {
                val legacy = json.decodeFromString<Stored>(raw)
                val profile = legacy.copy(
                    configId = legacy.configId ?: Uuid.random().toString(),
                    revision = if (legacy.configId == null) 1 else legacy.revision,
                    name = legacy.modelId,
                    protocol = if (version == 1) AssistantProtocol.OPENAI_COMPATIBLE.id else legacy.protocol,
                    reasoningEffort = if (version >= 3) legacy.reasoningEffort else null,
                )
                val needsDefaultEffort = version < 3 && supportsAssistantReasoningEffort(
                    baseUrl = profile.baseUrl,
                    modelId = profile.modelId,
                    protocol = profile.connectionProtocol(),
                )
                val upgraded = if (needsDefaultEffort) {
                    profile.copy(reasoningEffort = AssistantReasoningEffort.LOW.id)
                } else profile
                Catalog(
                    version = version,
                    profiles = listOf(upgraded),
                    activeProfileId = upgraded.configId
                )
            }
            val ids = mutableSetOf<String>()
            val credentials = mutableSetOf<String>()
            for (profile in catalog.profiles) {
                val id = profile.configId
                if (id.isNullOrBlank() || !ids.add(id) || !validLabel(profile.name) ||
                    !validLabel(profile.modelId) || profile.revision < 0
                ) corrupt()
                if (profile.credentialId != null && !credentials.add(profile.credentialId)) corrupt()
                val protocol = profile.connectionProtocol()
                normalizeAssistantBaseUrl(input = profile.baseUrl, protocol = protocol)
                val supported = supportsAssistantReasoningEffort(
                    baseUrl = profile.baseUrl, modelId = profile.modelId, protocol = protocol
                )
                if (supported && profile.reasoningEffort?.let(AssistantReasoningEffort::fromId) == null ||
                    !supported && profile.reasoningEffort != null
                ) corrupt()
            }
            if (catalog.activeProfileId == null && ids.isNotEmpty() ||
                catalog.activeProfileId != null && catalog.activeProfileId !in ids
            ) corrupt()
            return catalog
        } catch (failure: ConfigurationReadException) {
            throw failure
        } catch (_: Exception) {
            corrupt()
        }
    }

    private suspend fun cleanup(id: String) {
        try {
            credentialStore.delete(id)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // A failed cleanup must not revert the committed configuration.
        }
    }
}
