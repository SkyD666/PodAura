package com.skyd.anivu.model.bean.download.bt

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import com.skyd.anivu.model.bean.BaseBean
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

const val DOWNLOAD_INFO_TABLE_NAME = "DownloadInfo"

@Parcelize
@Serializable
@Entity(
    tableName = DOWNLOAD_INFO_TABLE_NAME,
    primaryKeys = [
        BtDownloadInfoBean.LINK_COLUMN
    ],
    indices = [
        Index(BtDownloadInfoBean.LINK_COLUMN, unique = true),
    ]
)
data class BtDownloadInfoBean(
    @ColumnInfo(name = LINK_COLUMN)
    val link: String,
    @ColumnInfo(name = NAME_COLUMN)
    val name: String,
    @ColumnInfo(name = PATH_COLUMN)
    val path: String,
    @ColumnInfo(name = DOWNLOAD_DATE_COLUMN)
    var downloadDate: Long,
    @ColumnInfo(name = SIZE_COLUMN)
    val size: Long,
    @ColumnInfo(name = PROGRESS_COLUMN)
    val progress: Float,
    @ColumnInfo(name = DESCRIPTION_COLUMN)
    val description: String? = null,
    @ColumnInfo(name = DOWNLOAD_STATE_COLUMN)
    val downloadState: DownloadState = DownloadState.Init,
    @ColumnInfo(name = DOWNLOAD_REQUEST_ID_COLUMN)
    val downloadRequestId: String,
) : BaseBean, Parcelable {
    @IgnoredOnParcel
    @Ignore
    var peerInfoList: List<PeerInfoBean> = emptyList()

    @IgnoredOnParcel
    @Ignore
    var uploadPayloadRate: Int = 0

    @IgnoredOnParcel
    @Ignore
    var downloadPayloadRate: Int = 0

    enum class DownloadState {
        Init, Downloading, Paused, Completed, ErrorPaused, StorageMovedFailed, Seeding, SeedingPaused;

        fun downloadComplete(): Boolean {
            return this == Completed || this == Seeding || this == SeedingPaused
        }
    }

    companion object {
        const val LINK_COLUMN = "link"
        const val NAME_COLUMN = "name"
        const val PATH_COLUMN = "path"
        const val DOWNLOAD_DATE_COLUMN = "downloadDate"
        const val SIZE_COLUMN = "size"
        const val PROGRESS_COLUMN = "progress"
        const val DESCRIPTION_COLUMN = "description"
        const val DOWNLOAD_STATE_COLUMN = "downloadState"
        const val DOWNLOAD_REQUEST_ID_COLUMN = "downloadRequestId"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BtDownloadInfoBean

        if (link != other.link) return false
        if (name != other.name) return false
        if (downloadDate != other.downloadDate) return false
        if (size != other.size) return false
        if (progress != other.progress) return false
        if (description != other.description) return false
        if (downloadState != other.downloadState) return false
        if (downloadRequestId != other.downloadRequestId) return false
        if (uploadPayloadRate != other.uploadPayloadRate) return false
        if (downloadPayloadRate != other.downloadPayloadRate) return false
        return peerInfoList == other.peerInfoList
    }

    override fun hashCode(): Int {
        var result = link.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + downloadDate.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + progress.hashCode()
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + downloadState.hashCode()
        result = 31 * result + downloadRequestId.hashCode()
        result = 31 * result + peerInfoList.hashCode()
        result = 31 * result + uploadPayloadRate.hashCode()
        result = 31 * result + downloadPayloadRate.hashCode()
        return result
    }
}