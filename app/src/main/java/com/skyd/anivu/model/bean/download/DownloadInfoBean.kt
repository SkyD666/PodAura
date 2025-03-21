package com.skyd.anivu.model.bean.download

import com.skyd.downloader.Status


data class DownloadInfoBean(
    val id: Int,
    val url: String,
    val path: String,
    val fileName: String,
    val status: Status,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val speedInBytePerMs: Float,
    val createTime: Long,
    val failureReason: String
)