package com.skyd.downloader.download

import com.skyd.downloader.db.DownloadEntity

internal expect class DownloadManager() {
    suspend fun schedule(entity: DownloadEntity)
    suspend fun cancel(id: String)
    suspend fun reconcile()
}
