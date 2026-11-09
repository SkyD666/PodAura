package com.skyd.podaura.podnya.capabilities.feeds.model.repository

import com.fleeksoft.ksoup.Ksoup
import com.skyd.podaura.api.model.bean.FeedQuery
import com.skyd.podaura.api.model.repository.IFeedRepository
import com.skyd.podaura.podnya.capabilities.feeds.model.bean.FeedSearchError
import com.skyd.podaura.podnya.capabilities.feeds.model.bean.FeedSearchLimits
import com.skyd.podaura.podnya.core.AssistantException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class LocalFeedSearchRepository(private val repository: IFeedRepository) {
    internal suspend fun search(query: FeedQuery): List<LocalFeed> = withContext(Dispatchers.IO) {
        try {
            require(query.limit in 1..FeedSearchLimits.SEARCH_RESULTS + 1)
            require(query.keywords.size <= 6 && query.keywords.all { it.isNotBlank() && it.length <= 80 })
            repository.query(query).take(query.limit).map { record ->
                fun String.plain(limit: Int) = Ksoup.parse(take(16_384)).text().take(limit)
                LocalFeed(
                    record.url,
                    (record.nickname?.takeIf(String::isNotBlank) ?: record.title).orEmpty()
                        .plain(200),
                    (record.customDescription?.takeIf(String::isNotBlank) ?: record.description)
                        .orEmpty().plain(FeedSearchLimits.DESCRIPTION_CHARS),
                    record.muted,
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            throw AssistantException(FeedSearchError.LOCAL_SEARCH)
        }
    }
}

internal data class LocalFeed(
    val url: String,
    val title: String,
    val description: String,
    val muted: Boolean
)
