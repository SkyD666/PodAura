package com.skyd.podaura.model.bean.playlist

import androidx.room3.Relation
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.article.EnclosureBean

/** Only the article fields and attachments needed to prepare a playlist. */
data class PlaylistArticleBean(
    val articleId: String,
    val title: String?,
    val date: Long?,
    @Relation(
        parentColumns = [ArticleBean.ARTICLE_ID_COLUMN],
        entityColumns = [EnclosureBean.ARTICLE_ID_COLUMN],
    )
    val enclosures: List<EnclosureBean>,
)
