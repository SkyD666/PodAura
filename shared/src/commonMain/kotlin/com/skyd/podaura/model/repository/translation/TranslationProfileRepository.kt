package com.skyd.podaura.model.repository.translation

import com.skyd.podaura.model.bean.translation.TranslationHeader
import com.skyd.podaura.model.bean.translation.TranslationProfile
import com.skyd.podaura.model.bean.translation.TranslationProviderConfig
import com.skyd.podaura.model.bean.translation.TranslationProviderType
import com.skyd.podaura.model.bean.translation.TranslationVerificationResult
import com.skyd.podaura.model.db.dao.TranslationProfileDao
import com.skyd.podaura.model.db.entity.TranslationProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class TranslationProfileRepository(
    private val dao: TranslationProfileDao,
    private val credentialStore: CredentialStore,
    providers: List<TranslationProvider>,
    private val cache: InMemoryTranslationCache,
    private val json: Json,
) {
    private val providersByType = providers.associateBy { it.type }

    fun observeAll(): Flow<List<TranslationProfile>> = dao.observeAll().map { entities ->
        entities.mapNotNull(::decode)
    }

    fun observeEnabled(): Flow<List<TranslationProfile>> = dao.observeEnabled().map { entities ->
        entities.mapNotNull(::decode)
    }

    suspend fun find(id: String): TranslationProfile? = dao.find(id)?.let(::decode)

    suspend fun findDefault(): TranslationProfile? = dao.findDefault()?.let(::decode)

    @OptIn(ExperimentalUuidApi::class)
    suspend fun save(profile: TranslationProfile, credential: String?): TranslationProfile {
        val normalizedProfile = if (!profile.enabled && profile.isDefault) {
            profile.copy(isDefault = false)
        } else {
            profile
        }
        require(TranslationProfileValidator.isValid(normalizedProfile))
        if (credential != null) require(credential.isNotBlank())
        val existing = dao.find(normalizedProfile.id)
        val previousCredentialId = existing?.credentialId
        val credentialId = if (credential != null) {
            Uuid.random().toString()
        } else {
            previousCredentialId
        }
        val storedProfile = normalizedProfile.copy(credentialId = credentialId)
        if (credential != null && credentialId != null) credentialStore.put(
            credentialId,
            credential
        )
        try {
            val now = Clock.System.now().toEpochMilliseconds()
            dao.save(
                encode(
                    profile = storedProfile,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                )
            )
        } catch (throwable: Throwable) {
            if (credential != null && credentialId != null) {
                runCatching { credentialStore.delete(credentialId) }
            }
            throw throwable
        }
        if (credential != null && previousCredentialId != null) {
            runCatching { credentialStore.delete(previousCredentialId) }
        }
        cache.clearForProfile(storedProfile.id)
        return storedProfile
    }

    suspend fun setDefault(id: String) {
        check(dao.setDefault(id) > 0) { "Translation profile does not exist" }
    }

    suspend fun copy(sourceId: String, newId: String, newName: String): TranslationProfile {
        val source = find(sourceId) ?: error("Translation profile does not exist")
        val credential = source.credentialId?.let { credentialStore.get(it) }
        return save(
            profile = source.copy(
                id = newId,
                name = newName,
                credentialId = null,
                isDefault = false,
            ),
            credential = credential,
        )
    }

    suspend fun delete(id: String, clearCachedTranslations: Boolean) {
        val entity = dao.find(id) ?: return
        dao.delete(id)
        entity.credentialId?.let { credentialStore.delete(it) }
        if (clearCachedTranslations) cache.clearForProfile(id)
    }

    suspend fun clearTranslationCache() = cache.clear()

    suspend fun verify(id: String): TranslationVerificationResult {
        val profile = find(id)
            ?: return TranslationVerificationResult.Failure(
                com.skyd.podaura.model.bean.translation.TranslationError.InvalidConfiguration
            )
        return verify(profile, credential = null)
    }

    suspend fun verify(
        profile: TranslationProfile,
        credential: String?,
    ): TranslationVerificationResult {
        if (!TranslationProfileValidator.isValid(profile) ||
            credential != null && credential.isBlank()
        ) {
            return TranslationVerificationResult.Failure(
                com.skyd.podaura.model.bean.translation.TranslationError.InvalidConfiguration
            )
        }
        val provider = providersByType[profile.providerType]
            ?: return TranslationVerificationResult.Failure(
                com.skyd.podaura.model.bean.translation.TranslationError.InvalidConfiguration
            )
        return provider.verify(profile, credential)
    }

    private fun encode(
        profile: TranslationProfile,
        createdAt: Long,
        updatedAt: Long,
    ) = TranslationProfileEntity(
        id = profile.id,
        name = profile.name,
        providerType = profile.providerType.name,
        endpoint = profile.endpoint,
        credentialId = profile.credentialId,
        customHeadersJson = json.encodeToString(profile.customHeaders),
        requestTimeoutMillis = profile.requestTimeoutMillis,
        enabled = profile.enabled,
        isDefault = profile.isDefault,
        targetLanguage = profile.targetLanguage,
        providerConfigJson = json.encodeToString(profile.config),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun decode(entity: TranslationProfileEntity): TranslationProfile? = runCatching {
        TranslationProfile(
            id = entity.id,
            name = entity.name,
            providerType = TranslationProviderType.valueOf(entity.providerType),
            endpoint = entity.endpoint,
            credentialId = entity.credentialId,
            customHeaders = json.decodeFromString<List<TranslationHeader>>(entity.customHeadersJson),
            requestTimeoutMillis = entity.requestTimeoutMillis,
            enabled = entity.enabled,
            isDefault = entity.isDefault,
            targetLanguage = entity.targetLanguage,
            config = json.decodeFromString<TranslationProviderConfig>(entity.providerConfigJson),
        )
    }.getOrNull()?.takeIf(TranslationProfileValidator::isValid)
}
