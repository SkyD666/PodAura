package com.skyd.podaura.model.repository.download

interface IDownloadManager {
    fun download(
        url: String,
        path: String,
        fileName: String? = null,
    ): Any
}