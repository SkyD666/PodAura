package com.skyd.podaura.model.bean.history

import androidx.room.Embedded
import androidx.room.Relation
import com.skyd.podaura.model.bean.BaseBean
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.article.ArticleWithFeed
import kotlinx.serialization.Serializable


@Serializable
data class ReadHistoryWithArticle(
    @Embedded
    var readHistoryBean: ReadHistoryBean,
    @Relation(
        entity = ArticleBean::class,
        parentColumn = ReadHistoryBean.ARTICLE_ID_COLUMN,
        entityColumn = ArticleBean.ARTICLE_ID_COLUMN,
    )
    var article: ArticleWithFeed,
) : BaseBean