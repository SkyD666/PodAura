package com.skyd.downloader

enum class Status {
    Init,
    Queued,
    Started,
    Downloading,
    Success,
    Failed,
    Paused,
}
