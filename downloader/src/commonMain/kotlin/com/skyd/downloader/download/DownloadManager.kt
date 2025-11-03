package com.skyd.downloader.download

expect class DownloadManager(): BaseDownloadManager {
    override suspend fun download(downloadRequest: DownloadRequest)
}