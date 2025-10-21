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
import kotlinx.datetime.LocalTime
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

fun Rss.rssToFeedWithArticleBean(url: String, icon: String? = null): FeedWithArticleBean {
    return FeedWithArticleBean(
        feed = FeedBean(
            url = url,
            title = channel.title,
            description = channel.description,
            link = channel.link,
            icon = channel.itunesImage?.href ?: channel.image?.url ?: icon,
        ),
        articles = channel.items.map { it.toArticleWithEnclosureBean(url) },
    )
}

fun Rss.rssUpdateFeedWithArticleBean(
    url: String,
    feed: FeedBean,
    icon: String? = null,
    articleTakeWhile: (String?) -> Boolean,
): FeedWithArticleBean {
    return FeedWithArticleBean(
        feed = feed.copy(
            title = channel.title,
            description = channel.description,
            link = channel.link,
            icon = channel.itunesImage?.href ?: channel.image?.url ?: icon,
        ),
        articles = channel.items.run {
            if (feed.sortXmlArticlesOnUpdate) {
                sortedByDescending { it.pubDate?.let { Instant.tryParse(it) } }
            } else {
                this
            }
        }.takeWhile {
            articleTakeWhile(it.link)
        }.map {
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
                itunesCategories?.map { it.toArticleCategoryBean(articleId) }?.flatten().orEmpty(),
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
            ?: itunesDuration?.let { LocalTime.tryParse(it)?.toMillisecondOfDay()?.toLong() },
        adult = (mediaRating?.contains("adult", true) ?: itunesExplicit) == true,
        image = mediaThumbnail.firstOrNull()?.url
            ?: itunesImage?.href,
        episode = itunesEpisode,
    )
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