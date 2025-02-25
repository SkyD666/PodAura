package com.skyd.anivu.ui.mpv.service

import com.skyd.anivu.model.bean.article.ArticleWithFeed

data class PlaylistBean(
    val path: String,
    val customMediaData: CustomMediaData,
)

data class CustomMediaData(
    val articleId: String? = null,
    val title: String? = null,
    val thumbnail: String? = null,
    val artist: String? = null,
) {
    companion object {
        fun fromArticleWithFeed(articleWithFeed: ArticleWithFeed?): CustomMediaData {
            articleWithFeed ?: return CustomMediaData()
            val articleWithEnclosure = articleWithFeed.articleWithEnclosure
            val article = articleWithFeed.articleWithEnclosure.article
            return CustomMediaData(
                articleId = article.articleId,
                title = article.title,
                thumbnail = articleWithEnclosure.media?.image
                    ?: articleWithFeed.feed.icon,
                artist = article.author.orEmpty().ifEmpty {
                    articleWithFeed.feed.title
                }
            )
        }
    }
}