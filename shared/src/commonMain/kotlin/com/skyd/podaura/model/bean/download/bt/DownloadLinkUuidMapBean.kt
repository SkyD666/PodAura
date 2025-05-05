package com.skyd.podaura.model.bean.download.bt

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.skyd.podaura.model.bean.BaseBean
import kotlinx.serialization.Serializable

const val DOWNLOAD_LINK_UUID_MAP_TABLE_NAME = "DownloadLinkUuidMap"

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
) : BaseBean {
    companion object {
        const val LINK_COLUMN = "link"
        const val UUID_COLUMN = "uuid"
    }
}