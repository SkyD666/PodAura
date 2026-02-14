package com.skyd.downloader.download

expect class DownloadManager(): BaseDownloadManager {
    override suspend fun download(downloadRequest: DownloadRequest)
    override suspend fun onPause(id: Int)
    override suspend fun onClearDbAsync(id: Int)
}
