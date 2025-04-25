package com.skyd.anivu.model.bean.download.bt

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import com.skyd.anivu.model.bean.BaseBean
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

const val DOWNLOAD_LINK_UUID_MAP_TABLE_NAME = "DownloadLinkUuidMap"

@Parcelize
@Serializable
@Entity(
    tableName = DOWNLOAD_LINK_UUID_MAP_TABLE_NAME,
    primaryKeys = [
        DownloadLinkUuidMapBean.LINK_COLUMN,
        DownloadLinkUuidMapBean.UUID_COLUMN,
    ],
)
data class DownloadLinkUuidMapBean(
    @ColumnInfo(name = LINK_COLUMN)
    val link: String,
    @ColumnInfo(name = UUID_COLUMN)
    val uuid: String,
) : BaseBean, Parcelable {
    companion object {
        const val LINK_COLUMN = "link"
        const val UUID_COLUMN = "uuid"
    }
}