package com.skyd.podaura.model.repository.download

import com.skyd.podaura.model.download.ArticleDownloadSource

interface IDownloadManager {
    fun download(
        url: String,
        path: String,
        fileName: String? = null,
        articleDownloadSource: ArticleDownloadSource? = null,
    ): Int
}
