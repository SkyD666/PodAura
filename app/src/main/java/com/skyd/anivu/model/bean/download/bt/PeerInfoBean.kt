package com.skyd.anivu.model.bean.download.bt

import android.os.Parcelable
import com.skyd.anivu.model.bean.BaseBean
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.libtorrent4j.PeerInfo.ConnectionType

@Parcelize
@Serializable
data class PeerInfoBean(
    var client: String? = null,
    var totalDownload: Long = 0,
    var totalUpload: Long = 0,
    var flags: Int = 0,
    var source: Byte = 0,
    var upSpeed: Int = 0,
    var downSpeed: Int = 0,
    var connectionType: ConnectionType? = null,
    var progress: Float = 0f,
    var progressPpm: Int = 0,
    var ip: String? = null,
) : BaseBean, Parcelable {
    companion object {
        fun from(peerInfo: org.libtorrent4j.PeerInfo): PeerInfoBean {
            return PeerInfoBean(
                client = peerInfo.client(),
                totalDownload = peerInfo.totalDownload(),
                totalUpload = peerInfo.totalUpload(),
                flags = peerInfo.flags(),
                source = peerInfo.source(),
                upSpeed = peerInfo.upSpeed(),
                downSpeed = peerInfo.downSpeed(),
                connectionType = peerInfo.connectionType(),
                progress = peerInfo.progress(),
                progressPpm = peerInfo.progressPpm(),
                ip = peerInfo.ip()
            )
        }
    }
}