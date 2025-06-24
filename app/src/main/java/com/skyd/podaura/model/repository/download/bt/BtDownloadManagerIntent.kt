package com.skyd.podaura.model.repository.download.bt

import com.skyd.mvi.MviIntent
import com.skyd.podaura.model.bean.download.bt.BtDownloadInfoBean
import com.skyd.podaura.model.bean.download.bt.BtDownloadInfoBean.DownloadState
import com.skyd.podaura.model.bean.download.bt.TorrentFileBean

sealed interface BtDownloadManagerIntent : MviIntent {
    data class UpdateDownloadInfo(val btDownloadInfoBean: BtDownloadInfoBean) :
        BtDownloadManagerIntent

    data class UpdateSessionParams(
        val link: String,
        val sessionStateData: ByteArray,
    ) : BtDownloadManagerIntent {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as UpdateSessionParams

            if (link != other.link) return false
            if (!sessionStateData.contentEquals(other.sessionStateData)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = link.hashCode()
            result = 31 * result + sessionStateData.contentHashCode()
            return result
        }
    }

    data class UpdateDownloadProgress(val link: String, val progress: Float) :
        BtDownloadManagerIntent

    data class UpdateDownloadState(
        val link: String,
        val downloadState: DownloadState,
    ) : BtDownloadManagerIntent

    data class UpdateDownloadSize(val link: String, val size: Long) : BtDownloadManagerIntent
    data class UpdateDownloadName(val link: String, val name: String?) : BtDownloadManagerIntent
    data class UpdateDownloadInfoRequestId(val link: String, val downloadRequestId: String) :
        BtDownloadManagerIntent

    data class UpdateTorrentFiles(val files: List<TorrentFileBean>) : BtDownloadManagerIntent
    data class UpdateDownloadDescription(val link: String, val description: String) :
        BtDownloadManagerIntent
}