package com.skyd.downloader.notification

import android.app.NotificationManager

object NotificationConst {
    const val NOTIFICATION_CHANNEL_ID = "downloadChannel"
    const val KEY_NOTIFICATION_CHANNEL_NAME = "keyNotificationChannelName"
    const val DEFAULT_VALUE_NOTIFICATION_CHANNEL_NAME = "File Download"
    const val KEY_NOTIFICATION_CHANNEL_DESCRIPTION = "keyNotificationChannelDescription"
    const val DEFAULT_VALUE_NOTIFICATION_CHANNEL_DESCRIPTION = "Notify file download status"
    const val KEY_NOTIFICATION_CHANNEL_IMPORTANCE = "keyNotificationChannelImportance"
    const val DEFAULT_VALUE_NOTIFICATION_CHANNEL_IMPORTANCE = NotificationManager.IMPORTANCE_LOW
    const val KEY_NOTIFICATION_CONTENT_ACTIVITY = "keyNotificationContentActivity"
    const val KEY_NOTIFICATION_CONTENT_BASE_PATH = "keyNotificationContentBasePath"
    const val KEY_NOTIFICATION_SMALL_ICON = "keySmallNotificationIcon"
    const val DEFAULT_VALUE_NOTIFICATION_SMALL_ICON = -1
    const val KEY_NOTIFICATION_ID = "keyNotificationId"
    const val KEY_DOWNLOAD_REQUEST_ID = "keyDownloadRequestId"
    const val KEY_FILE_NAME = "keyFileName"
    const val KEY_TOTAL_BYTES = "keyTotalBytes"

    const val MAX_VALUE_PROGRESS = 100

    // Actions
    const val ACTION_NOTIFICATION_DISMISSED = "ACTION_NOTIFICATION_DISMISSED"
    const val ACTION_DOWNLOAD_COMPLETED = "ACTION_DOWNLOAD_COMPLETED"
    const val ACTION_DOWNLOAD_FAILED = "ACTION_DOWNLOAD_FAILED"
    const val ACTION_DOWNLOAD_CANCELLED = "ACTION_DOWNLOAD_CANCELLED"
    const val ACTION_DOWNLOAD_PAUSED = "ACTION_NOTIFICATION_PAUSED"
    const val ACTION_NOTIFICATION_RESUME_CLICK = "ACTION_NOTIFICATION_RESUME_CLICK"
    const val ACTION_NOTIFICATION_RETRY_CLICK = "ACTION_NOTIFICATION_RETRY_CLICK"
    const val ACTION_NOTIFICATION_PAUSE_CLICK = "ACTION_NOTIFICATION_PAUSE_CLICK"
    const val ACTION_NOTIFICATION_CANCEL_CLICK = "ACTION_NOTIFICATION_CANCEL_CLICK"
}
