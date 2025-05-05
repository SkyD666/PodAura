package com.skyd.podaura.model.bean.playlist

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
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
) : BaseBean {
    companion object {
        const val PLAYLIST_ID_COLUMN = "playlistId"
        const val NAME_COLUMN = "name"
        const val ORDER_POSITION_COLUMN = "orderPosition"
        const val CREATE_TIME_COLUMN = "createTime"
        const val DELETE_MEDIA_ON_FINISH_COLUMN = "deleteMediaOnFinish"
    }
}