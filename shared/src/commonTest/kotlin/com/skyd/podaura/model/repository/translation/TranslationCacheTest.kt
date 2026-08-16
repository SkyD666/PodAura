package com.skyd.podaura.model.repository.translation

import com.skyd.podaura.model.bean.translation.ArticleTranslation
import com.skyd.podaura.model.bean.translation.TranslationContentSource
import com.skyd.podaura.model.bean.translation.TranslationProfile
import com.skyd.podaura.model.bean.translation.TranslationProviderConfig
import com.skyd.podaura.model.bean.translation.TranslationProviderType
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TranslationCacheTest {
    private val json = Json { encodeDefaults = true }
    private val profile = TranslationProfile(
        id = "profile",
        name = "DeepL",
        providerType = TranslationProviderType.DeepL,
        credentialId = "credential",
        targetLanguage = "ZH",
        config = TranslationProviderConfig.DeepL(useFreeEndpoint = true),
    )

    @Test
    fun cacheKeySeparatesSourceContentProfileLanguageAndProtocolInputs() {
        fun key(
            source: TranslationContentSource = TranslationContentSource.Feed,
            html: String = "<p>Hello</p>",
            currentProfile: TranslationProfile = profile,
            language: String = "ZH",
        ) = TranslationCacheKey.create("article", source, "Title", html, currentProfile, language, json)

        val base = key()
        assertEquals(base.cacheKey, key().cacheKey)
        assertNotEquals(base.cacheKey, key(source = TranslationContentSource.FullText).cacheKey)
        assertNotEquals(base.cacheKey, key(html = "<p>Changed</p>").cacheKey)
        assertNotEquals(base.cacheKey, key(language = "JA").cacheKey)
        assertNotEquals(
            base.cacheKey,
            key(
                currentProfile = profile.copy(
                    config = TranslationProviderConfig.DeepL(useFreeEndpoint = false)
                )
            ).cacheKey,
        )
    }

    @Test
    fun memoryCacheEvictsLeastRecentlyUsedAndNeverPersists() = runTest {
        val cache = InMemoryTranslationCache(maximumEntries = 2, maximumBytes = 1024)
        suspend fun put(key: String) = cache.put(
            InMemoryTranslationCacheEntry(
                cacheKey = key,
                articleId = "article-$key",
                profileId = "profile",
                translation = ArticleTranslation(key, "<p>$key</p>", null, fromCache = false),
            )
        )

        put("a")
        put("b")
        assertFalse(cache.get("a")?.translation?.fromCache == false)
        put("c")

        assertNull(cache.get("b"))
        assertTrue(cache.get("a")?.translation?.fromCache == true)
        assertEquals(2, cache.size())
        cache.clearForProfile("profile")
        assertEquals(0, cache.size())
    }
}
