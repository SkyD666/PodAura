package com.skyd.podaura.model.bean.article

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import com.skyd.podaura.model.bean.BaseBean
import kotlinx.serialization.Serializable

const val ENCLOSURE_TABLE_NAME = "enclosure"

@Serializable
@Entity(
    tableName = ENCLOSURE_TABLE_NAME,
    primaryKeys = [EnclosureBean.ARTICLE_ID_COLUMN, EnclosureBean.URL_COLUMN],
    foreignKeys = [
        ForeignKey(
            entity = ArticleBean::class,
            parentColumns = [ArticleBean.ARTICLE_ID_COLUMN],
            childColumns = [EnclosureBean.ARTICLE_ID_COLUMN],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class EnclosureBean(
    @ColumnInfo(name = ARTICLE_ID_COLUMN)
    val articleId: String,
    @ColumnInfo(name = URL_COLUMN)
    val url: String,
    @ColumnInfo(name = LENGTH_COLUMN)
    val length: Long,
    @ColumnInfo(name = TYPE_COLUMN)
    val type: String?,
) : BaseBean {
    companion object {
        const val ARTICLE_ID_COLUMN = "articleId"
        const val URL_COLUMN = "url"
        const val LENGTH_COLUMN = "length"
        const val TYPE_COLUMN = "type"

        val videoExtensions = listOf(
            ".m3u8", ".m4v", ".mov", ".avi", ".webm",
            ".mp4", ".mkv",
        )
        val audioExtensions = listOf(
            ".ogg", ".mp3", ".flac", ".m4a",
        )
    }

    val isMedia: Boolean
        get() = isVideo || isAudio

    val isVideo: Boolean
        get() = type?.startsWith("video/") == true ||
                type == "application/vnd.apple.mpegurl" ||
                videoExtensions.any { url.endsWith(it) }

    val isAudio: Boolean
        get() = type?.startsWith("audio/") == true ||
                audioExtensions.any { url.endsWith(it) }
}