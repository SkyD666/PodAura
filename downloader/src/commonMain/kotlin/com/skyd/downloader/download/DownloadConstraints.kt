package com.skyd.downloader.download

data class DownloadConstraints(
    val requireUnmetered: Boolean = false,
    val requiresCharging: Boolean = false,
    val requiresBatteryNotLow: Boolean = false,
)
