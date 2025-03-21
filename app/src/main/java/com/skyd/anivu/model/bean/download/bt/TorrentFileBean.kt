package com.skyd.anivu.model.bean.download.bt

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import com.skyd.anivu.base.BaseBean
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

const val TORRENT_FILE_TABLE_NAME = "TorrentFile"

@Parcelize
@Serializable
@Entity(
    tableName = TORRENT_FILE_TABLE_NAME,
    primaryKeys = [
        TorrentFileBean.LINK_COLUMN,
        TorrentFileBean.PATH_COLUMN,
    ],
    foreignKeys = [
        ForeignKey(
            entity = BtDownloadInfoBean::class,
            parentColumns = [BtDownloadInfoBean.LINK_COLUMN],
            childColumns = [TorrentFileBean.LINK_COLUMN],
            onDelete = ForeignKey.CASCADE
        )
    ],
)
data class TorrentFileBean(
    @ColumnInfo(name = LINK_COLUMN)
    val link: String,
    @ColumnInfo(name = PATH_COLUMN)
    val path: String,
    @ColumnInfo(name = SIZE_COLUMN)
    val size: Long,
) : BaseBean, Parcelable {
    companion object {
        const val LINK_COLUMN = "link"
        const val PATH_COLUMN = "path"
        const val SIZE_COLUMN = "size"
    }
}