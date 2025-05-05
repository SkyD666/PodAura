package com.skyd.podaura.model.bean.history

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.skyd.podaura.model.bean.BaseBean
import com.skyd.podaura.model.bean.article.ArticleBean
import kotlinx.serialization.Serializable

const val READ_HISTORY_TABLE_NAME = "ReadHistory"

@Serializable
@Entity(
    tableName = READ_HISTORY_TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = ArticleBean::class,
            parentColumns = [ArticleBean.ARTICLE_ID_COLUMN],
            childColumns = [ReadHistoryBean.ARTICLE_ID_COLUMN],
            onDelete = ForeignKey.CASCADE
        )
    ],
)
data class ReadHistoryBean(
    @PrimaryKey
    @ColumnInfo(name = ARTICLE_ID_COLUMN)
    val articleId: String,
    @ColumnInfo(name = LAST_TIME_COLUMN)
    val lastTime: Long,
) : BaseBean {
    companion object {
        const val ARTICLE_ID_COLUMN = "articleId"
        const val LAST_TIME_COLUMN = "lastTime"
    }
}