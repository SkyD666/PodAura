package com.skyd.podaura.model.bean.feed

import androidx.room3.Embedded
import androidx.room3.Relation
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.article.ArticleWithEnclosureBean

/**
 * A [feed] contains many [articles].
 */
data class FeedWithArticleBean(
    @Embedded
    var feed: FeedBean,
    @Relation(
        parentColumns = [FeedBean.URL_COLUMN],
        entityColumns = [ArticleBean.FEED_URL_COLUMN],
    )
    var articles: List<ArticleWithEnclosureBean>,
)
