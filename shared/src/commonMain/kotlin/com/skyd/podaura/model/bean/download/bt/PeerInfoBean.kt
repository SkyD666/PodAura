package com.skyd.podaura.model.bean.download.bt

import com.skyd.podaura.model.bean.BaseBean
import kotlinx.serialization.Serializable

@Serializable
data class PeerInfoBean(
    var client: String? = null,
    var totalDownload: Long = 0,
    var totalUpload: Long = 0,
    var flags: Int = 0,
    var source: Byte = 0,
    var upSpeed: Int = 0,
    var downSpeed: Int = 0,
    var progress: Float = 0f,
    var progressPpm: Int = 0,
    var ip: String? = null,
) : BaseBean {
    companion object
}