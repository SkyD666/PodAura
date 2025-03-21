package com.skyd.downloader

import android.app.NotificationManager
import kotlinx.serialization.Serializable

@Serializable
data class NotificationConfig(
    val enabled: Boolean = true,
    val channelName: String,
    val channelDescription: String,
    val importance: Int = NotificationManager.IMPORTANCE_LOW,
    val smallIcon: Int,
    val pauseText: Int,
    val resumeText: Int,
    val cancelText: Int,
    val retryText: Int,
)
