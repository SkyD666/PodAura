package com.skyd.podaura

import com.skyd.podaura.model.repository.download.DownloadManager

fun onAppStart() {
    DownloadManager.listenDownloadEvent()
}