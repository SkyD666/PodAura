package com.skyd.podaura.model.bean.article

import androidx.room3.Embedded
import androidx.room3.Relation
import com.skyd.podaura.model.bean.BaseBean
import kotlinx.serialization.Serializable

@Serializable
data class ArticleWithEnclosureBean(
    @Embedded
    var article: ArticleBean,
    @Relation(
        parentColumns = [ArticleBean.ARTICLE_ID_COLUMN],
        entityColumns = [EnclosureBean.ARTICLE_ID_COLUMN],
    )
    var enclosures: List<EnclosureBean>,
    @Relation(
        parentColumns = [ArticleBean.ARTICLE_ID_COLUMN],
        entityColumns = [ArticleCategoryBean.ARTICLE_ID_COLUMN],
    )
    var categories: List<ArticleCategoryBean>,
    @Relation(
        parentColumns = [ArticleBean.ARTICLE_ID_COLUMN],
        entityColumns = [RssMediaBean.ARTICLE_ID_COLUMN],
    )
    var media: RssMediaBean?,
) : BaseBean
