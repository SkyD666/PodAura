package com.skyd.podaura.model.bean.playlist

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.skyd.podaura.model.bean.BaseBean
import kotlinx.serialization.Serializable

const val PLAYLIST_TABLE_NAME = "Playlist"

@Serializable
@Entity(
    tableName = PLAYLIST_TABLE_NAME,
    indices = [
        Index(PlaylistBean.ORDER_POSITION_COLUMN),
    ]
)
data class PlaylistBean(
    @PrimaryKey
    @ColumnInfo(name = PLAYLIST_ID_COLUMN)
    val playlistId: String,
    @ColumnInfo(name = NAME_COLUMN)
    val name: String,
    @ColumnInfo(name = ORDER_POSITION_COLUMN)
    val orderPosition: Double,
    @ColumnInfo(name = CREATE_TIME_COLUMN)
    val createTime: Long,
    @ColumnInfo(name = DELETE_MEDIA_ON_FINISH_COLUMN)
    val deleteMediaOnFinish: Boolean,
    // Null only for pre-migration rows, until their old global preference is copied.
    @ColumnInfo(name = MEDIA_SORT_BY_COLUMN)
    val mediaSortBy: String? = "Manual",
    @ColumnInfo(name = MEDIA_SORT_ASC_COLUMN, defaultValue = "1")
    val mediaSortAsc: Boolean = true,
) : BaseBean {
    companion object {
        const val PLAYLIST_ID_COLUMN = "playlistId"
        const val NAME_COLUMN = "name"
        const val ORDER_POSITION_COLUMN = "orderPosition"
        const val CREATE_TIME_COLUMN = "createTime"
        const val DELETE_MEDIA_ON_FINISH_COLUMN = "deleteMediaOnFinish"
        const val MEDIA_SORT_BY_COLUMN = "mediaSortBy"
        const val MEDIA_SORT_ASC_COLUMN = "mediaSortAsc"
    }
}
