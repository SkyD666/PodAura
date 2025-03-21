package com.skyd.anivu.model.bean.history

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.skyd.anivu.base.BaseBean
import com.skyd.anivu.model.bean.article.ArticleBean
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

const val MEDIA_PLAY_HISTORY_TABLE_NAME = "MediaPlayHistory"

@Parcelize
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
) : BaseBean, Parcelable {
    companion object {
        const val PATH_COLUMN = "path"
        const val DURATION_COLUMN = "duration"
        const val LAST_PLAY_POSITION_COLUMN = "lastPlayPosition"
        const val LAST_TIME_COLUMN = "lastTime"
        const val ARTICLE_ID_COLUMN = "articleId"
    }
}