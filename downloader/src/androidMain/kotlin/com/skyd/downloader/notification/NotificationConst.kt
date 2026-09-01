package com.skyd.downloader.notification

internal object NotificationConst {
    const val NOTIFICATION_CHANNEL_ID = "downloadChannel"
    const val NOTIFICATION_GROUP_KEY = "com.skyd.podaura.DOWNLOADS"
    const val GROUP_SUMMARY_NOTIFICATION_ID = Int.MAX_VALUE

    const val KEY_NOTIFICATION_ID = "notificationId"
    const val KEY_DOWNLOAD_TASK_ID = "downloadTaskId"

    const val ACTION_NOTIFICATION_RESUME_CLICK = "com.skyd.podaura.download.RESUME"
    const val ACTION_NOTIFICATION_RETRY_CLICK = "com.skyd.podaura.download.RETRY"
    const val ACTION_NOTIFICATION_PAUSE_CLICK = "com.skyd.podaura.download.PAUSE"
    const val ACTION_NOTIFICATION_CANCEL_CLICK = "com.skyd.podaura.download.CANCEL"
}
