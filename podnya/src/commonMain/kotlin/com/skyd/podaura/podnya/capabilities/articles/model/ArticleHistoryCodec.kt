package com.skyd.podaura.podnya.capabilities.articles.model

import com.skyd.podaura.api.model.bean.ArticleSearchScope
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchContent
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchContext
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchError
import com.skyd.podaura.podnya.capabilities.articles.model.bean.AssistantArticle
import com.skyd.podaura.podnya.capabilities.articles.model.bean.RecommendedArticle
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ValidatedAssistantAnswer
import com.skyd.podaura.podnya.core.AssistantAnswerKind
import com.skyd.podaura.podnya.core.AssistantContent
import com.skyd.podaura.podnya.core.AssistantContext
import com.skyd.podaura.podnya.core.AssistantFailure
import com.skyd.podaura.podnya.core.AssistantHistoryCodec
import com.skyd.podaura.podnya.core.CapabilityRecord
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object ArticleHistoryCodec : AssistantHistoryCodec {
    override val capabilityId = ArticleSearchContent.CAPABILITY_ID
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class Content(
        val kind: AssistantAnswerKind? = null,
        val text: String = "",
        val articles: List<RecommendedArticle> = emptyList(),
        val candidates: List<AssistantArticle> = emptyList(),
        val resultsChanged: Boolean = false,
        val hadCandidates: Boolean = false,
    )

    @Serializable
    private data class Context(
        val restricted: Boolean,
        val feedUrls: Set<String> = emptySet(),
        val articleIds: Set<String> = emptySet(),
    )

    override fun encodeFailure(error: AssistantFailure): String? =
        (error as? ArticleSearchError)?.let { "$capabilityId:${it.name}" }

    override fun decodeFailure(value: String): AssistantFailure? =
        if (value.startsWith("$capabilityId:")) {
            runCatching { ArticleSearchError.valueOf(value.substringAfter(':')) }.getOrNull()
        } else null

    override fun encodeContent(content: AssistantContent): CapabilityRecord? {
        if (content !is ArticleSearchContent) return null
        val answer = content.answer
        return CapabilityRecord(
            capabilityId = capabilityId,
            version = 1,
            payload = json.encodeToString(
                Content(
                    kind = answer?.kind,
                    text = answer?.text.orEmpty(),
                    articles = answer?.articles.orEmpty(),
                    candidates = content.candidates,
                    resultsChanged = answer?.resultsChanged ?: false,
                    hadCandidates = answer?.hadCandidates ?: false,
                )
            ),
            fallback = listOfNotNull(answer?.text).plus(
                answer?.articles?.map { "${it.article.title}\n${it.reason}" }
                    ?: content.candidates.map { it.title }
            ).filter { it.isNotBlank() }.joinToString("\n"),
        )
    }

    override fun decodeContent(record: CapabilityRecord): AssistantContent {
        require(record.version == 1)
        val value = json.decodeFromString<Content>(record.payload)
        return ArticleSearchContent.restore(
            answer = value.kind?.let {
                ValidatedAssistantAnswer(
                    kind = it,
                    text = value.text,
                    articles = value.articles,
                    resultsChanged = value.resultsChanged,
                    hadCandidates = value.hadCandidates,
                )
            },
            candidates = value.candidates,
        )
    }

    override fun encodeContext(context: AssistantContext): CapabilityRecord? {
        if (context !is ArticleSearchContext) return null
        val scope = context.scope
        return CapabilityRecord(
            capabilityId = capabilityId,
            version = 1,
            payload = json.encodeToString(
                Context(
                    restricted = scope is ArticleSearchScope.Restricted,
                    feedUrls = (scope as? ArticleSearchScope.Restricted)?.feedUrls.orEmpty(),
                    articleIds = (scope as? ArticleSearchScope.Restricted)?.articleIds.orEmpty(),
                )
            ),
            fallback = "",
        )
    }

    override fun decodeContext(record: CapabilityRecord): AssistantContext {
        require(record.version == 1)
        val value = json.decodeFromString<Context>(record.payload)
        return ArticleSearchContext(
            scope = if (value.restricted) {
                ArticleSearchScope.Restricted(
                    feedUrls = value.feedUrls,
                    articleIds = value.articleIds
                )
            } else ArticleSearchScope.All,
        )
    }
}
