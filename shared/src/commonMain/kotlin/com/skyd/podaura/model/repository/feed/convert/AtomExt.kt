package com.skyd.podaura.model.repository.feed.convert

import com.skyd.fundation.ext.currentTimeMillis
import com.skyd.fundation.ext.tryParse
import com.skyd.podaura.ext.encodeURL
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.article.ArticleCategoryBean
import com.skyd.podaura.model.bean.article.ArticleWithEnclosureBean
import com.skyd.podaura.model.bean.article.EnclosureBean
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.bean.feed.FeedWithArticleBean
import com.skyd.podaura.model.repository.feed.rssparser.atom.Entry
import com.skyd.podaura.model.repository.feed.rssparser.atom.Feed
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

fun Feed.feedToFeedWithArticleBean(url: String, icon: String? = null): FeedWithArticleBean {
    return FeedWithArticleBean(
        feed = FeedBean(
            url = url,
            title = title,
            description = subtitle,
            link = link,
            icon = logo ?: this.icon ?: icon,
        ),
        articles = entries.map { it.toArticleWithEnclosureBean(url) },
    )
}

fun Feed.feedUpdateFeedWithArticleBean(
    url: String,
    feed: FeedBean,
    icon: String? = null,
    articleTakeWhile: (String?) -> Boolean,
): FeedWithArticleBean {
    return FeedWithArticleBean(
        feed = feed.copy(
            title = title,
            description = subtitle,
            link = link,
            icon = logo ?: this.icon ?: icon,
        ),
        articles = entries.run {
            if (feed.sortXmlArticlesOnUpdate) {
                sortedByDescending { it.published?.let { Instant.tryParse(it) } }
            } else {
                this
            }
        }.takeWhile {
            articleTakeWhile(it.links?.firstOrNull { it.rel == "alternate" }?.href)
        }.map {
            it.toArticleWithEnclosureBean(url)
        },
    )
}

fun Entry.toArticleWithEnclosureBean(feedUrl: String): ArticleWithEnclosureBean {
    val article = toArticleBean(feedUrl)
    val articleId = article.articleId
    return ArticleWithEnclosureBean(
        article = article,
        enclosures = toEnclosures(articleId),
        categories = categories.map { it.toArticleCategoryBean(articleId) },
        media = null,
    )
}

fun Entry.toArticleBean(feedUrl: String): ArticleBean {
    val articleId = Uuid.random().toString()
    return ArticleBean(
        articleId = articleId,
        feedUrl = feedUrl,
        date = ((published ?: updated)?.let { Instant.tryParse(it) }
            ?: Clock.System.now()).toEpochMilliseconds(),
        title = title,
        author = author?.name,
        description = content?.text,
        image = findImg((content?.text).orEmpty()),
        link = links?.firstOrNull { it.rel == "alternate" }?.href,
        guid = id,
        updateAt = Clock.currentTimeMillis(),
    )
}

fun Entry.Category.toArticleCategoryBean(articleId: String): ArticleCategoryBean {
    return ArticleCategoryBean(
        articleId = articleId,
        category = label ?: term,
    )
}

fun Entry.toEnclosures(articleId: String): List<EnclosureBean> {
    val enclosure = links.orEmpty().filter { it.rel == "enclosure" }.map { link ->
        EnclosureBean(
            articleId = articleId,
            url = link.href.encodeURL(),
            length = link.length ?: 0L,
            type = link.type,
        )
    }
    return enclosure + listOfNotNull(
        *mediaGroup?.contents.orEmpty().toTypedArray()
    ).map { it.toEnclosureBean(articleId) }
}