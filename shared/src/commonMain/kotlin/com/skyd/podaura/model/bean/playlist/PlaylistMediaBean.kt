package com.skyd.podaura.model.bean.playlist

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import com.skyd.podaura.ext.isLocalFile
import com.skyd.podaura.model.bean.BaseBean
import com.skyd.podaura.model.bean.article.ArticleBean
import kotlinx.serialization.Serializable

const val PLAYLIST_MEDIA_TABLE_NAME = "PlaylistMedia"

@Serializable
@Entity(
    tableName = PLAYLIST_MEDIA_TABLE_NAME,
    primaryKeys = [PlaylistMediaBean.PLAYLIST_ID_COLUMN, PlaylistMediaBean.URL_COLUMN],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistBean::class,
            parentColumns = [PlaylistBean.PLAYLIST_ID_COLUMN],
            childColumns = [PlaylistMediaBean.PLAYLIST_ID_COLUMN],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ArticleBean::class,
            parentColumns = [ArticleBean.ARTICLE_ID_COLUMN],
            childColumns = [PlaylistMediaBean.ARTICLE_ID_COLUMN],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(PlaylistMediaBean.ARTICLE_ID_COLUMN),
        Index(PlaylistMediaBean.ORDER_POSITION_COLUMN)
    ]
)
data class PlaylistMediaBean(
    @ColumnInfo(name = PLAYLIST_ID_COLUMN)
    val playlistId: String,
    @ColumnInfo(name = URL_COLUMN)
    val url: String,
    @ColumnInfo(name = ARTICLE_ID_COLUMN)
    val articleId: String?,
    @ColumnInfo(name = ORDER_POSITION_COLUMN)
    val orderPosition: Double,
    @ColumnInfo(name = CREATE_TIME_COLUMN)
    val createTime: Long,
) : BaseBean {
    fun isSamePlaylistMedia(other: PlaylistMediaBean?): Boolean {
        other ?: return false
        return playlistId == other.playlistId && url == other.url
    }

    @Ignore
    val isLocalFile: Boolean = url.isLocalFile()

    @Ignore
    var title: String? = null

    @Ignore
    var duration: Long? = null

    @Ignore
    var artist: String? = null

    @Ignore
    var thumbnail: String? = null

    companion object {
        const val PLAYLIST_ID_COLUMN = "playlistId"
        const val URL_COLUMN = "url"
        const val ARTICLE_ID_COLUMN = "articleId"
        const val ORDER_POSITION_COLUMN = "orderPosition"
        const val CREATE_TIME_COLUMN = "createTime"
    }
}

expect fun PlaylistMediaBean.updateLocalMediaMetadata()