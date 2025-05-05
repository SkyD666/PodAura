package com.skyd.podaura.model.bean.download.bt


fun PeerInfoBean.Companion.from(peerInfo: org.libtorrent4j.PeerInfo): PeerInfoBean {
    return PeerInfoBean(
        client = peerInfo.client(),
        totalDownload = peerInfo.totalDownload(),
        totalUpload = peerInfo.totalUpload(),
        flags = peerInfo.flags(),
        source = peerInfo.source(),
        upSpeed = peerInfo.upSpeed(),
        downSpeed = peerInfo.downSpeed(),
        progress = peerInfo.progress(),
        progressPpm = peerInfo.progressPpm(),
        ip = peerInfo.ip()
    )
}