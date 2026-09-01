package com.skyd.podaura.model.repository.download

import com.skyd.downloader.download.DownloadConstraints
import com.skyd.podaura.model.download.ArticleDownloadSource
import com.skyd.podaura.model.download.DownloadInfoBean

interface IDownloadManager {
    suspend fun download(
        url: String,
        path: String,
        fileName: String? = null,
        articleDownloadSource: ArticleDownloadSource? = null,
        constraints: DownloadConstraints = DownloadConstraints(),
    ): String

    suspend fun getAllDownloadTasks(): List<DownloadInfoBean>
}
