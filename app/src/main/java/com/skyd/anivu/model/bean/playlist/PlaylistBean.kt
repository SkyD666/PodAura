package com.skyd.anivu.model.bean.playlist

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.skyd.anivu.base.BaseBean
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

const val PLAYLIST_TABLE_NAME = "Playlist"

@Parcelize
@Serializable
@Entity(tableName = PLAYLIST_TABLE_NAME)
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
) : BaseBean, Parcelable {
    companion object {
        const val PLAYLIST_ID_COLUMN = "playlistId"
        const val NAME_COLUMN = "name"
        const val ORDER_POSITION_COLUMN = "orderPosition"
        const val CREATE_TIME_COLUMN = "createTime"
        const val DELETE_MEDIA_ON_FINISH_COLUMN = "deleteMediaOnFinish"
    }
}