package com.skyd.downloader.notification

import kotlinx.serialization.Serializable

@Serializable
data class NotificationConfig(
    val enabled: Boolean = true,
    val channelName: String,
    val channelDescription: String,
    val intentContentActivity: String?,
    val intentContentBasePath: String?,
    val smallIcon: Int,
)