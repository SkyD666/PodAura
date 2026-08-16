package com.skyd.podaura.model.repository.translation

import com.skyd.podaura.model.bean.translation.ArticleTranslationResult
import com.skyd.podaura.model.bean.translation.HtmlTranslationRequest
import com.skyd.podaura.model.bean.translation.HtmlTranslationResult
import com.skyd.podaura.model.bean.translation.TranslationCapabilities
import com.skyd.podaura.model.bean.translation.TranslationContentSource
import com.skyd.podaura.model.bean.translation.TranslationError
import com.skyd.podaura.model.bean.translation.TranslationHeader
import com.skyd.podaura.model.bean.translation.TranslationProfile
import com.skyd.podaura.model.bean.translation.TranslationProviderConfig
import com.skyd.podaura.model.bean.translation.TranslationProviderResult
import com.skyd.podaura.model.bean.translation.TranslationProviderType
import com.skyd.podaura.model.bean.translation.TranslationVerificationResult
import com.skyd.podaura.model.db.dao.TranslationProfileDao
import com.skyd.podaura.model.db.entity.TranslationProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TranslationRepositoryTest {
    private val json = Json { encodeDefaults = true }
    private val profile = TranslationProfile(
        id = "profile",
        name = "DeepL",
        providerType = TranslationProviderType.DeepL,
        credentialId = "credential",
        isDefault = true,
        targetLanguage = "ZH",
        config = TranslationProviderConfig.DeepL(),
    )

    @Test
    fun validResultIsCachedOnlyInTheProvidedMemoryCache() = runTest {
        val provider = FakeProvider { request -> request.html }
        val firstRepository = repository(provider, InMemoryTranslationCache())

        val first = firstRepository.translateArticle()
        val cached = firstRepository.translateArticle()

        assertFalse(assertIs<ArticleTranslationResult.Success>(first).translation.fromCache)
        assertTrue(assertIs<ArticleTranslationResult.Success>(cached).translation.fromCache)
        assertEquals(1, provider.calls)

        val repositoryAfterMemoryWasLost = repository(provider, InMemoryTranslationCache())
        val afterMemoryWasLost = repositoryAfterMemoryWasLost.translateArticle()

        assertFalse(
            assertIs<ArticleTranslationResult.Success>(afterMemoryWasLost).translation.fromCache
        )
        assertEquals(2, provider.calls)
    }

    @Test
    fun structurallyInvalidResultIsNeverCached() = runTest {
        val provider = FakeProvider { "<html><body><p>Broken</p></body></html>" }
        val repository = repository(provider, InMemoryTranslationCache())

        val first = repository.translateArticle()
        val second = repository.translateArticle()

        assertEquals(
            TranslationError.InvalidHtml,
            assertIs<ArticleTranslationResult.Failure>(first).error,
        )
        assertIs<ArticleTranslationResult.Failure>(second)
        assertEquals(2, provider.calls)
    }

    private fun repository(
        provider: TranslationProvider,
        cache: InMemoryTranslationCache,
    ): TranslationRepository {
        val profileRepository = TranslationProfileRepository(
            dao = FakeProfileDao(profile.toEntity()),
            credentialStore = FakeCredentialStore(),
            providers = listOf(provider),
            cache = cache,
            json = json,
        )
        return TranslationRepository(
            profileRepository = profileRepository,
            providers = listOf(provider),
            documentBuilder = TranslationDocumentBuilder(),
            validator = TranslationHtmlValidator(),
            cache = cache,
            json = json,
        )
    }

    private suspend fun TranslationRepository.translateArticle() = translate(
        articleId = "article",
        contentSource = TranslationContentSource.Feed,
        title = "Title",
        html = "<p>Hello</p><a href='https://example.com'>Link</a>",
        profileId = profile.id,
        targetLanguage = profile.targetLanguage,
    )

    private fun TranslationProfile.toEntity() = TranslationProfileEntity(
        id = id,
        name = name,
        providerType = providerType.name,
        endpoint = endpoint,
        credentialId = credentialId,
        customHeadersJson = json.encodeToString<List<TranslationHeader>>(customHeaders),
        requestTimeoutMillis = requestTimeoutMillis,
        enabled = enabled,
        isDefault = isDefault,
        targetLanguage = targetLanguage,
        providerConfigJson = json.encodeToString<TranslationProviderConfig>(config),
        createdAt = 1,
        updatedAt = 1,
    )

    private class FakeProvider(
        private val response: (HtmlTranslationRequest) -> String,
    ) : TranslationProvider {
        var calls = 0
        override val type = TranslationProviderType.DeepL

        override suspend fun getCapabilities(profile: TranslationProfile) =
            DeepLTranslationProvider.CAPABILITIES

        override suspend fun verify(
            profile: TranslationProfile,
            credential: String?,
        ) =
            TranslationVerificationResult.Success(DeepLTranslationProvider.CAPABILITIES)

        override suspend fun translateHtml(
            profile: TranslationProfile,
            request: HtmlTranslationRequest,
        ): TranslationProviderResult {
            calls++
            return TranslationProviderResult.Success(
                HtmlTranslationResult(
                    html = response(request),
                    detectedSourceLanguage = "EN",
                    usage = null,
                )
            )
        }
    }

    private class FakeCredentialStore : CredentialStore {
        override suspend fun put(id: String, secret: String) = Unit
        override suspend fun get(id: String): String = "key"
        override suspend fun delete(id: String) = Unit
    }

    private class FakeProfileDao(entity: TranslationProfileEntity) : TranslationProfileDao {
        private val entities = mutableListOf(entity)

        override fun observeAll(): Flow<List<TranslationProfileEntity>> = flowOf(entities)
        override fun observeEnabled(): Flow<List<TranslationProfileEntity>> =
            flowOf(entities.filter { it.enabled })
        override suspend fun find(id: String) = entities.firstOrNull { it.id == id }
        override suspend fun findDefault() = entities.firstOrNull { it.enabled && it.isDefault }
        override suspend fun upsert(entity: TranslationProfileEntity) {
            entities.removeAll { it.id == entity.id }
            entities += entity
        }
        override suspend fun clearDefault() {
            entities.replaceAll { it.copy(isDefault = false) }
        }
        override suspend fun markDefault(id: String) {
            entities.replaceAll {
                if (it.id == id) it.copy(isDefault = true, enabled = true) else it
            }
        }
        override suspend fun setDefault(id: String): Int {
            if (entities.none { it.id == id }) return 0
            clearDefault()
            markDefault(id)
            return entities.size
        }
        override suspend fun delete(id: String): Int =
            if (entities.removeAll { it.id == id }) 1 else 0
    }
}
