package com.skyd.downloader.util

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.skyd.downloader.notification.DownloadNotificationManager

internal object NotificationUtil {
    fun removeNotification(context: Context, taskId: String) {
        NotificationManagerCompat.from(context).run {
            cancel(DownloadNotificationManager.notificationId(taskId))
            cancel(DownloadNotificationManager.terminalNotificationId(taskId))
        }
    }
}
