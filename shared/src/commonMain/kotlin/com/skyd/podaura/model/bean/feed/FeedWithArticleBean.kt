package com.skyd.podaura.model.bean.feed

import androidx.room.Embedded
import androidx.room.Relation
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.article.ArticleWithEnclosureBean

/**
 * A [feed] contains many [articles].
 */
data class FeedWithArticleBean(
    @Embedded
    var feed: FeedBean,
    @Relation(
        parentColumn = FeedBean.URL_COLUMN,
        entityColumn = ArticleBean.Companion.FEED_URL_COLUMN,
    )
    var articles: List<ArticleWithEnclosureBean>,
)