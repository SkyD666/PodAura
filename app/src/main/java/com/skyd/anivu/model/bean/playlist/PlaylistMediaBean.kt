package com.skyd.anivu.model.bean.playlist

import android.media.MediaMetadataRetriever
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import com.skyd.anivu.base.BaseBean
import com.skyd.anivu.ext.isLocalFile
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
        )
    ],
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
    val isLocalFile = url.isLocalFile()

    @Ignore
    var title: String? = null

    @Ignore
    var duration: Long? = null

    @Ignore
    var artist: String? = null

    @Ignore
    var thumbnail: String? = null

    fun updateLocalMediaMetadata() {
        val retriever = MediaMetadataRetriever()
        try {
            with(retriever) {
                setDataSource(url)
                duration =
                    extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                title = extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                artist = extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        retriever.release()
    }

    companion object {
        const val PLAYLIST_ID_COLUMN = "playlistId"
        const val URL_COLUMN = "url"
        const val ARTICLE_ID_COLUMN = "articleId"
        const val ORDER_POSITION_COLUMN = "orderPosition"
        const val CREATE_TIME_COLUMN = "createTime"
    }
}