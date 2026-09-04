package com.skyd.podaura.model.download

import com.skyd.downloader.Status
import com.skyd.podaura.model.bean.feed.FeedBean


data class DownloadInfoBean(
    val id: String,
    val url: String,
    val path: String,
    val fileName: String,
    val status: Status,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val speedInBytePerMs: Float,
    val createTime: Long,
    val failureReason: String,
    val articleDownloadSource: ArticleDownloadSource? = null,
    val articleDownloadInfo: ArticleDownloadInfoBean? = null,
    val isPlayableMedia: Boolean = false,
) {
    val displayTitle: String
        get() = articleDownloadInfo?.articleTitle?.takeIf { it.isNotBlank() } ?: fileName

    val secondaryFileName: String?
        get() = fileName.takeIf { articleDownloadInfo != null && it != displayTitle }
}

data class ArticleDownloadInfoBean(
    val articleTitle: String?,
    val episodeImage: String?,
    val articleImage: String?,
    val feed: FeedBean,
) {
    val imageCandidates: List<String>
        get() = listOfNotNull(
            episodeImage?.takeIf { it.isNotBlank() },
            articleImage?.takeIf { it.isNotBlank() },
        ).distinct()
}
