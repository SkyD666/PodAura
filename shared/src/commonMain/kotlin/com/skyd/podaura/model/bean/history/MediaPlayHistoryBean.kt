package com.skyd.podaura.model.bean.history

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.skyd.podaura.model.bean.BaseBean
import com.skyd.podaura.model.bean.article.ArticleBean
import kotlinx.serialization.Serializable

const val MEDIA_PLAY_HISTORY_TABLE_NAME = "MediaPlayHistory"

@Serializable
@Entity(
    tableName = MEDIA_PLAY_HISTORY_TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = ArticleBean::class,
            parentColumns = [ArticleBean.ARTICLE_ID_COLUMN],
            childColumns = [MediaPlayHistoryBean.ARTICLE_ID_COLUMN],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(MediaPlayHistoryBean.ARTICLE_ID_COLUMN),
    ]
)
data class MediaPlayHistoryBean(
    @PrimaryKey
    @ColumnInfo(name = PATH_COLUMN)
    val path: String,
    @ColumnInfo(name = DURATION_COLUMN)
    val duration: Long,
    @ColumnInfo(name = LAST_PLAY_POSITION_COLUMN)
    val lastPlayPosition: Long,
    @ColumnInfo(name = LAST_TIME_COLUMN)
    val lastTime: Long,
    @ColumnInfo(name = ARTICLE_ID_COLUMN)
    val articleId: String?,
) : BaseBean {
    companion object {
        const val PATH_COLUMN = "path"
        const val DURATION_COLUMN = "duration"
        const val LAST_PLAY_POSITION_COLUMN = "lastPlayPosition"
        const val LAST_TIME_COLUMN = "lastTime"
        const val ARTICLE_ID_COLUMN = "articleId"
    }
}