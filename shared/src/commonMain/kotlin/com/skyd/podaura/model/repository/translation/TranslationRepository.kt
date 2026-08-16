package com.skyd.podaura.model.repository.translation

import co.touchlab.kermit.Logger
import com.skyd.podaura.model.bean.translation.ArticleTranslation
import com.skyd.podaura.model.bean.translation.ArticleTranslationResult
import com.skyd.podaura.model.bean.translation.HtmlTranslationRequest
import com.skyd.podaura.model.bean.translation.TranslationContentSource
import com.skyd.podaura.model.bean.translation.TranslationError
import com.skyd.podaura.model.bean.translation.TranslationProfile
import com.skyd.podaura.model.bean.translation.TranslationProviderResult
import kotlinx.serialization.json.Json

class TranslationRepository(
    private val profileRepository: TranslationProfileRepository,
    providers: List<TranslationProvider>,
    private val documentBuilder: TranslationDocumentBuilder,
    private val validator: TranslationHtmlValidator,
    private val cache: InMemoryTranslationCache,
    private val json: Json,
) {
    private val providersByType = providers.associateBy { it.type }
    private val log = Logger.withTag("TranslationRepository")

    suspend fun findCached(
        articleId: String,
        contentSource: TranslationContentSource,
        title: String?,
        html: String,
        profile: TranslationProfile,
        targetLanguage: String,
    ): ArticleTranslation? {
        val identity = TranslationCacheKey.create(
            articleId, contentSource, title, html, profile, targetLanguage, json
        )
        return cache.get(identity.cacheKey)?.translation?.copy(fromCache = true)
    }

    suspend fun translate(
        articleId: String,
        contentSource: TranslationContentSource,
        title: String?,
        html: String,
        profileId: String,
        targetLanguage: String,
    ): ArticleTranslationResult {
        val profile = profileRepository.find(profileId)
            ?: return ArticleTranslationResult.Failure(TranslationError.InvalidConfiguration)
        if (!profile.enabled) {
            return ArticleTranslationResult.Failure(TranslationError.InvalidConfiguration)
        }
        val identity = TranslationCacheKey.create(
            articleId, contentSource, title, html, profile, targetLanguage, json
        )
        cache.get(identity.cacheKey)?.let {
            return ArticleTranslationResult.Success(it.translation.copy(fromCache = true))
        }
        val provider = providersByType[profile.providerType]
            ?: return ArticleTranslationResult.Failure(TranslationError.InvalidConfiguration)
        val capabilities = provider.getCapabilities(profile)
        if (!capabilities.supportsHtml) {
            return ArticleTranslationResult.Failure(TranslationError.HtmlNotSupported)
        }
        val envelope = runCatching { documentBuilder.build(title, html) }.getOrNull()
            ?: return ArticleTranslationResult.Failure(TranslationError.InvalidHtml)
        val actualBytes = envelope.html.encodeToByteArray().size.toLong()
        val limitBytes = capabilities.maxTextRequestBytes
        if (limitBytes != null && actualBytes > limitBytes &&
            !capabilities.supportsDocumentTranslation
        ) {
            return ArticleTranslationResult.Failure(
                TranslationError.ContentTooLarge(actualBytes, limitBytes)
            )
        }
        return when (val result = provider.translateHtml(
            profile,
            HtmlTranslationRequest(
                html = envelope.html,
                targetLanguage = targetLanguage,
            ),
        )) {
            is TranslationProviderResult.Failure -> ArticleTranslationResult.Failure(result.error)
            is TranslationProviderResult.Success -> {
                val validated = when (
                    val validation = validator.validateDetailed(result.value.html, envelope)
                ) {
                    is TranslationHtmlValidationResult.Valid -> validation.document
                    is TranslationHtmlValidationResult.Invalid -> {
                        val diagnostic = validation.diagnostic
                        log.w {
                            "HTML validation failed: reason=${diagnostic.reason}, " +
                                    "nodeId=${diagnostic.nodeId}, " +
                                    "expectedTag=${diagnostic.expectedTag}, " +
                                    "actualTag=${diagnostic.actualTag}, " +
                                    "actualCount=${diagnostic.actualCount}, " +
                                    "rootCount=${diagnostic.rootCount}, " +
                                    "titleCount=${diagnostic.titleCount}, " +
                                    "contentCount=${diagnostic.contentCount}, " +
                                    "bodyChildCount=${diagnostic.bodyChildCount}, " +
                                    "rootChildCount=${diagnostic.rootChildCount}, " +
                                    "expectedCriticalNodes=${envelope.criticalNodes.size}, " +
                                    "requestBytes=$actualBytes, " +
                                    "responseBytes=${result.value.html.encodeToByteArray().size}, " +
                                    "provider=${profile.providerType}, " +
                                    "targetLanguage=${targetLanguage.take(MAX_LOGGED_LANGUAGE_LENGTH)}, " +
                                    "detectedSourceLanguage=" +
                                    result.value.detectedSourceLanguage?.take(
                                        MAX_LOGGED_LANGUAGE_LENGTH
                                    )
                        }
                        return ArticleTranslationResult.Failure(TranslationError.InvalidHtml)
                    }
                }
                val translation = ArticleTranslation(
                    title = validated.title,
                    html = validated.contentHtml,
                    detectedSourceLanguage = result.value.detectedSourceLanguage,
                    fromCache = false,
                )
                cache.put(
                    InMemoryTranslationCacheEntry(
                        cacheKey = identity.cacheKey,
                        articleId = articleId,
                        profileId = profile.id,
                        translation = translation,
                    )
                )
                ArticleTranslationResult.Success(translation)
            }
        }
    }

    suspend fun clearCache() = cache.clear()
    suspend fun clearArticleCache(articleId: String) = cache.clearForArticle(articleId)

    private companion object {
        const val MAX_LOGGED_LANGUAGE_LENGTH = 16
    }
}
