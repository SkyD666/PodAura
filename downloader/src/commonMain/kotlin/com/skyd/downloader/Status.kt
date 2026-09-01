package com.skyd.downloader

enum class Status {
    Init,
    Queued,
    Started,
    Downloading,
    Success,
    Failed,
    Paused,
    Cancelled,
}

internal val ACTIVE_DOWNLOAD_STATUS_NAMES = listOf(
    Status.Queued.name,
    Status.Started.name,
    Status.Downloading.name,
)

internal fun String.isActiveDownloadStatus(): Boolean = this in ACTIVE_DOWNLOAD_STATUS_NAMES
