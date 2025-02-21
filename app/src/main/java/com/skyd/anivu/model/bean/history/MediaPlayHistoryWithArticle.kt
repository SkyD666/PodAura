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
data class MediaPlayHistoryWithArticle(
    @Embedded
    var mediaPlayHistoryBean: MediaPlayHistoryBean,
    @Relation(
        entity = ArticleBean::class,
        parentColumn = MediaPlayHistoryBean.ARTICLE_ID_COLUMN,
        entityColumn = ArticleBean.ARTICLE_ID_COLUMN,
    )
    var article: ArticleWithFeed?,
) : Serializable, Parcelable