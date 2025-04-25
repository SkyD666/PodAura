package com.skyd.downloader

import android.app.NotificationManager
import kotlinx.serialization.Serializable

@Serializable
data class NotificationConfig(
    val enabled: Boolean = true,
    val channelName: String,
    val channelDescription: String,
    val importance: Int = NotificationManager.IMPORTANCE_LOW,
    val intentContentActivity: String?,
    val intentContentBasePath: String?,
    val smallIcon: Int,
    val pauseText: String,
    val resumeText: String,
    val cancelText: String,
    val retryText: String,
)
