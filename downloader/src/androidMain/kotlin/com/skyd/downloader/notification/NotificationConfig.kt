package com.skyd.downloader.notification

data class NotificationConfig(
    val enabled: Boolean = true,
    val channelName: String,
    val channelDescription: String,
    val intentContentActivity: String?,
    val intentContentBasePath: String?,
    val smallIcon: Int,
)
