package com.skyd.podaura.model.bean.article

import androidx.room.Embedded
import androidx.room.Relation
import com.skyd.podaura.model.bean.BaseBean
import kotlinx.serialization.Serializable

@Serializable
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
) : BaseBean
