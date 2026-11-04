package com.skyd.podaura.model.repository.feed.convert

import com.skyd.fundation.ext.currentTimeMillis
import com.skyd.fundation.ext.tryParse
import com.skyd.podaura.ext.encodeURL
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.article.ArticleCategoryBean
import com.skyd.podaura.model.bean.article.ArticleWithEnclosureBean
import com.skyd.podaura.model.bean.article.EnclosureBean
import com.skyd.podaura.model.bean.article.RssMediaBean
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.bean.feed.FeedWithArticleBean
import com.skyd.podaura.model.repository.feed.rssparser.rss.Item
import com.skyd.podaura.model.repository.feed.rssparser.rss.Rss
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.uuid.Uuid

suspend fun Rss.rssToFeedWithArticleBean(url: String, icon: String? = null): FeedWithArticleBean {
    return FeedWithArticleBean(
        feed = FeedBean(
            url = url,
            title = channel.title,
            description = channel.description,
            link = channel.link,
            icon = channel.itunesImage?.href ?: channel.image?.url ?: icon,
        ),
        articles = channel.items.map {
            currentCoroutineContext().ensureActive()
            it.toArticleWithEnclosureBean(url)
        },
    )
}

suspend fun Rss.rssUpdateFeedWithArticleBean(
    url: String,
    feed: FeedBean,
    icon: String? = null,
    articleTakeWhile: (String?) -> Boolean,
): FeedWithArticleBean {
    val context = currentCoroutineContext()
    context.ensureActive()
    return FeedWithArticleBean(
        feed = feed.copy(
            title = channel.title,
            description = channel.description,
            link = channel.link,
            icon = channel.itunesImage?.href ?: channel.image?.url ?: icon,
        ),
        articles = channel.items.run {
            if (feed.sortXmlArticlesOnUpdate) {
                sortedByDescending { item ->
                    context.ensureActive()
                    item.pubDate?.let { Instant.tryParse(it) }
                }
            } else {
                this
            }
        }.takeWhile {
            context.ensureActive()
            articleTakeWhile(it.link)
        }.map {
            context.ensureActive()
            it.toArticleWithEnclosureBean(url)
        },
    )
}

fun Item.toArticleWithEnclosureBean(feedUrl: String): ArticleWithEnclosureBean {
    val article = toArticleBean(feedUrl)
    val articleId = article.articleId
    return ArticleWithEnclosureBean(
        article = article,
        enclosures = toEnclosures(articleId),
        categories = categories.map { it.toArticleCategoryBean(articleId) } +
                itunesCategories?.flatMap { it.toArticleCategoryBean(articleId) }.orEmpty(),
        media = toRssMediaBean(articleId),
    )
}

fun Item.toArticleBean(feedUrl: String): ArticleBean {
    val articleId = Uuid.random().toString()
    return ArticleBean(
        articleId = articleId,
        feedUrl = feedUrl,
        date = (pubDate?.let { Instant.tryParse(it) } ?: Clock.System.now()).toEpochMilliseconds(),
        title = title.toString(),
        author = author ?: itunesAuthor,
        description = contentEncoded ?: description,
        image = itunesImage?.href ?: findImg((contentEncoded ?: description).orEmpty()),
        link = link,
        guid = guid,
        updateAt = Clock.currentTimeMillis(),
    )
}

fun Item.toRssMediaBean(articleId: String): RssMediaBean {
    return RssMediaBean(
        articleId = articleId,
        duration = mediaContent?.duration?.times(1000)
            ?: mediaGroup?.contents?.firstOrNull()?.duration?.times(1000)
            ?: itunesDuration?.let(::parsePodcastDurationMillis),
        adult = (mediaRating?.contains("adult", true) ?: itunesExplicit) == true,
        image = mediaThumbnail.firstOrNull()?.url
            ?: itunesImage?.href,
        episode = itunesEpisode,
    )
}

private val podcastDurationPattern = Regex("""[0-9]+(?::0*[0-5]?[0-9]){0,2}(?:\.[0-9]+)?""")

/** Converts seconds, mm:ss or hh:mm:ss to an ISO duration for the standard parser. */
internal fun parsePodcastDurationMillis(input: String): Long? {
    val value = input.trim()
    if (!podcastDurationPattern.matches(value)) return null
    // Preserve millisecond truncation before Duration rounds sub-nanosecond fractions.
    val decimalIndex = value.indexOf('.')
    val normalized = if (decimalIndex < 0) value else value.take(decimalIndex + 4)
    val parts = normalized.split(':')
    val units = listOf("H", "M", "S").takeLast(parts.size)
    val isoDuration = parts.zip(units).joinToString(separator = "", prefix = "PT") { (part, unit) ->
        part + unit
    }
    return Duration.parseIsoStringOrNull(isoDuration)?.takeIf { it.isFinite() }?.inWholeMilliseconds
}

fun Item.Category.toArticleCategoryBean(articleId: String): ArticleCategoryBean {
    return ArticleCategoryBean(
        articleId = articleId,
        category = text,
    )
}

fun Item.ItunesCategory.toArticleCategoryBean(articleId: String): List<ArticleCategoryBean> {
    var result = if (!text.isNullOrBlank()) {
        listOf(ArticleCategoryBean(articleId = articleId, category = text))
    } else {
        emptyList()
    }

    if (itunesCategory != null) {
        result = result + itunesCategory.toArticleCategoryBean(articleId)
        result = result.distinctBy { it.category }
    }
    return result
}

fun Item.toEnclosures(articleId: String): List<EnclosureBean> {
    return enclosures.map { it.toEnclosureBean(articleId) } + listOfNotNull(
        mediaContent,
        *mediaGroup?.contents.orEmpty().toTypedArray()
    ).map { it.toEnclosureBean(articleId) }
}

fun Item.MediaContent.toEnclosureBean(articleId: String): EnclosureBean {
    return EnclosureBean(
        articleId = articleId,
        url = url.encodeURL(),
        length = fileSize ?: 0L,
        type = type,
    )
}

fun Item.Enclosure.toEnclosureBean(articleId: String): EnclosureBean {
    return EnclosureBean(
        articleId = articleId,
        url = url.encodeURL(),
        length = length ?: 0,
        type = type,
    )
}
