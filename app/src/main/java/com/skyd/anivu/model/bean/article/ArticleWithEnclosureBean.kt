package com.skyd.anivu.model.bean.article

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
@kotlinx.serialization.Serializable
data class ArticleWithEnclosureBean(
    @Embedded
    var article: ArticleBean,
    @Relation(
        parentColumn = ArticleBean.ARTICLE_ID_COLUMN,
        entityColumn = EnclosureBean.ARTICLE_ID_COLUMN,
    )
    var enclosures: List<EnclosureBean>,
    @Relation(
        parentColumn = ArticleBean.ARTICLE_ID_COLUMN,
        entityColumn = ArticleCategoryBean.ARTICLE_ID_COLUMN,
    )
    var categories: List<ArticleCategoryBean>,
    @Relation(
        parentColumn = ArticleBean.ARTICLE_ID_COLUMN,
        entityColumn = RssMediaBean.ARTICLE_ID_COLUMN,
    )
    var media: RssMediaBean?,
) : Serializable, Parcelable
