package com.skyd.downloader.download

import com.skyd.downloader.db.DownloadEntity
import com.skyd.downloader.util.FileUtil.getUniqueId


data class DownloadRequest(
    val url: String,
    val path: String,
    val fileName: String,
    val id: Int = getUniqueId(url, path, fileName),
) {
    companion object {
        internal fun DownloadEntity.toDownloadRequest() = DownloadRequest(
            url = url,
            path = path,
            fileName = fileName,
            id = id,
        )
    }
}