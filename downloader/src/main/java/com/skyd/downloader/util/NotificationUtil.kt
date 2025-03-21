package com.skyd.downloader.util

import android.content.Context
import androidx.core.app.NotificationManagerCompat

internal object NotificationUtil {
    fun removeNotification(context: Context, notificationId: Int) {
        // Downloading, Paused notification
        NotificationManagerCompat.from(context).cancel(notificationId)
        // Cancelled, Failed, Success notification
        NotificationManagerCompat.from(context).cancel(notificationId + 1)
    }
}
