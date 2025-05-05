package com.skyd.podaura.model.bean.article

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import com.skyd.podaura.model.bean.BaseBean
import kotlinx.serialization.Serializable

const val ARTICLE_CATEGORY_TABLE_NAME = "ArticleCategory"

@Serializable
@Entity(
    tableName = ARTICLE_CATEGORY_TABLE_NAME,
    primaryKeys = [ArticleCategoryBean.ARTICLE_ID_COLUMN, ArticleCategoryBean.CATEGORY_COLUMN],
    foreignKeys = [
        ForeignKey(
            entity = ArticleBean::class,
            parentColumns = [ArticleBean.ARTICLE_ID_COLUMN],
            childColumns = [RssMediaBean.ARTICLE_ID_COLUMN],
            onDelete = ForeignKey.CASCADE
        )
    ],
)
data class ArticleCategoryBean(
    @ColumnInfo(name = ARTICLE_ID_COLUMN)
    val articleId: String,
    @ColumnInfo(name = CATEGORY_COLUMN)
    val category: String,
) : BaseBean {
    companion object {
        const val ARTICLE_ID_COLUMN = "articleId"
        const val CATEGORY_COLUMN = "category"
    }
}