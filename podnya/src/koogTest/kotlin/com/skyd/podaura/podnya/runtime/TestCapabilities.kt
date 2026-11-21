package com.skyd.podaura.podnya.runtime

import com.skyd.podaura.api.model.bean.feed.FeedQuery
import com.skyd.podaura.api.model.bean.feed.FeedRecord
import com.skyd.podaura.api.model.repository.IFeedRepository
import com.skyd.podaura.podnya.capabilities.articles.ArticleSearchCapability
import com.skyd.podaura.podnya.capabilities.articles.model.repository.LocalArticleSearchRepository
import com.skyd.podaura.podnya.capabilities.feeds.FeedSearchCapability
import com.skyd.podaura.podnya.capabilities.feeds.model.repository.LocalFeedSearchRepository
import com.skyd.podaura.podnya.core.AssistantTurnRequest

internal fun testCapabilities(articles: LocalArticleSearchRepository, feeds: LocalFeedSearchRepository):
    (AssistantTurnRequest) -> List<KoogCapability> = { request ->
    listOfNotNull(ArticleSearchCapability.create(articles, request), FeedSearchCapability.create(feeds, request))
}

internal fun emptyFeedSource() = LocalFeedSearchRepository(object : IFeedRepository {
    override suspend fun query(query: FeedQuery) = emptyList<FeedRecord>()
})
