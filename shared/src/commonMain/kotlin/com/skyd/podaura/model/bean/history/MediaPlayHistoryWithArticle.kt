package com.skyd.podaura.model.bean.history

import androidx.room3.Embedded
import androidx.room3.Relation
import com.skyd.podaura.model.bean.BaseBean
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.article.ArticleWithFeed
import kotlinx.serialization.Serializable


@Serializable
data class MediaPlayHistoryWithArticle(
    @Embedded
    var mediaPlayHistoryBean: MediaPlayHistoryBean,
    @Relation(
        entity = ArticleBean::class,
        parentColumns = [MediaPlayHistoryBean.ARTICLE_ID_COLUMN],
        entityColumns = [ArticleBean.ARTICLE_ID_COLUMN],
    )
    var article: ArticleWithFeed?,
) : BaseBean
