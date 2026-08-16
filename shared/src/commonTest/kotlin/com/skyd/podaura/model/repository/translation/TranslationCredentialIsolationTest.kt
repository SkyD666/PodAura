package com.skyd.podaura.model.repository.translation

import com.skyd.podaura.model.bean.translation.HtmlTranslationRequest
import com.skyd.podaura.model.bean.translation.TranslationProfile
import com.skyd.podaura.model.bean.translation.TranslationProviderConfig
import com.skyd.podaura.model.bean.translation.TranslationProviderResult
import com.skyd.podaura.model.bean.translation.TranslationProviderType
import com.skyd.podaura.model.bean.translation.TranslationVerificationResult
import com.skyd.podaura.model.db.dao.TranslationProfileDao
import com.skyd.podaura.model.db.entity.TranslationProfileEntity
import com.skyd.podaura.model.preference.preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TranslationCredentialIsolationTest {
    @Test
    fun preferenceExportRegistryContainsNoTranslationCredentialFields() {
        val exportedPreferenceNames = preferences.mapNotNull { (preference, _) ->
            preference.key?.name?.lowercase()
        }

        assertFalse(
            exportedPreferenceNames.any { name ->
                "translation" in name || "credential" in name || "api_key" in name
            }
        )
    }

    @Test
    fun profileRoomRecordContainsOnlyAnOpaqueRepositoryOwnedCredentialId() = runTest {
        val dao = RecordingProfileDao()
        val credentials = RecordingCredentialStore()
        val repository = TranslationProfileRepository(
            dao = dao,
            credentialStore = credentials,
            providers = emptyList(),
            cache = InMemoryTranslationCache(),
            json = Json { encodeDefaults = true },
        )
        val stored = repository.save(
            profile = TranslationProfile(
                id = "profile",
                name = "DeepL",
                providerType = TranslationProviderType.DeepL,
                credentialId = "caller-controlled-id",
                config = TranslationProviderConfig.DeepL(),
            ),
            credential = "super-secret-key",
        )

        val entity = assertNotNull(dao.entity)
        val credentialId = assertNotNull(stored.credentialId)
        assertNotEquals("caller-controlled-id", credentialId)
        assertEquals(credentialId, entity.credentialId)
        assertFalse(entity.toString().contains("super-secret-key"))
        assertEquals("super-secret-key", credentials.values[credentialId])
    }

    @Test
    fun replacingCredentialDoesNotReadAnInaccessiblePreviousEntry() = runTest {
        val dao = RecordingProfileDao()
        val credentials = RecordingCredentialStore()
        val repository = TranslationProfileRepository(
            dao = dao,
            credentialStore = credentials,
            providers = emptyList(),
            cache = InMemoryTranslationCache(),
            json = Json { encodeDefaults = true },
        )
        val initial = repository.save(
            profile = TranslationProfile(
                id = "profile",
                name = "DeepL",
                providerType = TranslationProviderType.DeepL,
                config = TranslationProviderConfig.DeepL(),
            ),
            credential = "old-secret",
        )
        val previousCredentialId = assertNotNull(initial.credentialId)
        credentials.failReadsFor += previousCredentialId

        val updated = repository.save(
            profile = initial.copy(name = "Updated"),
            credential = "new-secret",
        )

        val updatedCredentialId = assertNotNull(updated.credentialId)
        assertNotEquals(previousCredentialId, updatedCredentialId)
        assertEquals("new-secret", credentials.values[updatedCredentialId])
        assertFalse(previousCredentialId in credentials.values)
    }

    @Test
    fun disablingDefaultProfilePersistsDisabledAndClearsDefault() = runTest {
        val dao = RecordingProfileDao()
        val repository = TranslationProfileRepository(
            dao = dao,
            credentialStore = RecordingCredentialStore(),
            providers = emptyList(),
            cache = InMemoryTranslationCache(),
            json = Json { encodeDefaults = true },
        )

        val stored = repository.save(
            profile = TranslationProfile(
                id = "profile",
                name = "DeepL",
                providerType = TranslationProviderType.DeepL,
                enabled = false,
                isDefault = true,
                config = TranslationProviderConfig.DeepL(),
            ),
            credential = null,
        )

        assertFalse(stored.enabled)
        assertFalse(stored.isDefault)
        assertFalse(assertNotNull(dao.entity).enabled)
        assertFalse(assertNotNull(dao.entity).isDefault)
    }

    @Test
    fun verifyingADraftDoesNotPersistItOrItsCredential() = runTest {
        val dao = RecordingProfileDao()
        val credentials = RecordingCredentialStore()
        val provider = RecordingProvider()
        val repository = TranslationProfileRepository(
            dao = dao,
            credentialStore = credentials,
            providers = listOf(provider),
            cache = InMemoryTranslationCache(),
            json = Json { encodeDefaults = true },
        )
        val draft = TranslationProfile(
            id = "profile",
            name = "Edited",
            providerType = TranslationProviderType.DeepL,
            credentialId = "saved-credential",
            requestTimeoutMillis = 15_000,
            targetLanguage = "DE",
            config = TranslationProviderConfig.DeepL(useFreeEndpoint = false),
        )

        val result = repository.verify(draft, credential = "draft-api-key")

        assertIs<TranslationVerificationResult.Success>(result)
        assertEquals(draft, provider.profile)
        assertEquals("draft-api-key", provider.credential)
        assertNull(dao.entity)
        assertEquals(emptyMap(), credentials.values)
    }

    private class RecordingProvider : TranslationProvider {
        override val type = TranslationProviderType.DeepL
        var profile: TranslationProfile? = null
        var credential: String? = null

        override suspend fun getCapabilities(profile: TranslationProfile) =
            DeepLTranslationProvider.CAPABILITIES

        override suspend fun verify(
            profile: TranslationProfile,
            credential: String?,
        ): TranslationVerificationResult {
            this.profile = profile
            this.credential = credential
            return TranslationVerificationResult.Success(DeepLTranslationProvider.CAPABILITIES)
        }

        override suspend fun translateHtml(
            profile: TranslationProfile,
            request: HtmlTranslationRequest,
        ): TranslationProviderResult = error("Not used")
    }

    private class RecordingCredentialStore : CredentialStore {
        val values = mutableMapOf<String, String>()
        val failReadsFor = mutableSetOf<String>()
        override suspend fun put(id: String, secret: String) {
            values[id] = secret
        }
        override suspend fun get(id: String): String? {
            if (id in failReadsFor) throw CredentialStorageException()
            return values[id]
        }
        override suspend fun delete(id: String) {
            values.remove(id)
        }
    }

    private class RecordingProfileDao : TranslationProfileDao {
        var entity: TranslationProfileEntity? = null
        private val flow = MutableStateFlow<List<TranslationProfileEntity>>(emptyList())

        override fun observeAll(): Flow<List<TranslationProfileEntity>> = flow
        override fun observeEnabled(): Flow<List<TranslationProfileEntity>> = flow
        override suspend fun find(id: String): TranslationProfileEntity? =
            entity?.takeIf { it.id == id }
        override suspend fun findDefault(): TranslationProfileEntity? =
            entity?.takeIf { it.enabled && it.isDefault }
        override suspend fun upsert(entity: TranslationProfileEntity) {
            this.entity = entity
            flow.value = listOf(entity)
        }
        override suspend fun clearDefault() {
            entity = entity?.copy(isDefault = false)
        }
        override suspend fun markDefault(id: String) {
            entity = entity?.takeIf { it.id == id }?.copy(isDefault = true, enabled = true)
        }
        override suspend fun setDefault(id: String): Int {
            if (entity?.id != id) return 0
            entity = entity?.copy(isDefault = true, enabled = true)
            return 1
        }
        override suspend fun delete(id: String): Int {
            if (entity?.id != id) return 0
            entity = null
            flow.value = emptyList()
            return 1
        }
    }
}
