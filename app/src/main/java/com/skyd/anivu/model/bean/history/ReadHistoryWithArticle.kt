package com.skyd.anivu.model.bean.history

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Relation
import com.skyd.anivu.model.bean.article.ArticleBean
import com.skyd.anivu.model.bean.article.ArticleWithFeed
import kotlinx.parcelize.Parcelize
import java.io.Serializable


@Parcelize
@kotlinx.serialization.Serializable
data class ReadHistoryWithArticle(
    @Embedded
    var readHistoryBean: ReadHistoryBean,
    @Relation(
        entity = ArticleBean::class,
        parentColumn = ReadHistoryBean.ARTICLE_ID_COLUMN,
        entityColumn = ArticleBean.ARTICLE_ID_COLUMN,
    )
    var article: ArticleWithFeed,
) : Serializable, Parcelable