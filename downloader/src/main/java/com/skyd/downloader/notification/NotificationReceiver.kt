package com.skyd.downloader.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.skyd.downloader.Downloader
import com.skyd.downloader.R
import com.skyd.downloader.download.DownloadWorker
import com.skyd.downloader.notification.DownloadNotificationManager.Companion.createOpenIntent
import com.skyd.downloader.util.TextUtil

/**
 * Notification receiver: Responsible for showing the terminating state notification (paused, cancelled, failed)
 * It also handles the user action from notification (Pause, Resume, Cancel, Retry)
 *
 * Notification ID = (Unique Download Request ID + 1) for each download
 *
 * @constructor Create empty Notification receiver
 */
internal class NotificationReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val downloader = Downloader.instance()

        val extras = intent.extras
        when (intent.action) {
            // Resume the download and dismiss the notification
            NotificationConst.ACTION_NOTIFICATION_RESUME_CLICK -> {
                val requestId = extras?.getInt(NotificationConst.KEY_DOWNLOAD_REQUEST_ID)
                val nId = extras?.getInt(NotificationConst.KEY_NOTIFICATION_ID)
                if (nId != null) NotificationManagerCompat.from(context).cancel(nId)
                if (requestId != null) {
                    downloader.resume(requestId)
                }
                return
            }

            // Retry the download and dismiss the notification
            NotificationConst.ACTION_NOTIFICATION_RETRY_CLICK -> {
                val requestId = extras?.getInt(NotificationConst.KEY_DOWNLOAD_REQUEST_ID)
                val nId = extras?.getInt(NotificationConst.KEY_NOTIFICATION_ID)
                if (nId != null) NotificationManagerCompat.from(context).cancel(nId)
                if (requestId != null) {
                    downloader.retry(requestId)
                }
                return
            }

            // Pause the download and dismiss the notification
            NotificationConst.ACTION_NOTIFICATION_PAUSE_CLICK -> {
                val requestId = extras?.getInt(NotificationConst.KEY_DOWNLOAD_REQUEST_ID)
                val nId = extras?.getInt(NotificationConst.KEY_NOTIFICATION_ID)
                if (nId != null) NotificationManagerCompat.from(context).cancel(nId)
                if (requestId != null) {
                    downloader.pause(requestId)
                }
                return
            }

            // Cancel the download and dismiss the notification
            NotificationConst.ACTION_NOTIFICATION_CANCEL_CLICK -> {
                val requestId = extras?.getInt(NotificationConst.KEY_DOWNLOAD_REQUEST_ID)
                val nId = extras?.getInt(NotificationConst.KEY_NOTIFICATION_ID)
                if (nId != null) NotificationManagerCompat.from(context).cancel(nId)
                if (requestId != null) {
                    downloader.clearDb(requestId, deleteFile = true)
                }
                return
            }

            // List of actions when notification gets triggered
            else -> {
                val notificationActionList = listOf(
                    NotificationConst.ACTION_DOWNLOAD_COMPLETED,
                    NotificationConst.ACTION_DOWNLOAD_FAILED,
                    NotificationConst.ACTION_DOWNLOAD_CANCELLED,
                    NotificationConst.ACTION_DOWNLOAD_PAUSED
                )

                if (intent.action in notificationActionList) {
                    val notificationChannelName =
                        extras?.getString(NotificationConst.KEY_NOTIFICATION_CHANNEL_NAME)
                            ?: NotificationConst.DEFAULT_VALUE_NOTIFICATION_CHANNEL_NAME
                    val notificationImportance =
                        extras?.getInt(NotificationConst.KEY_NOTIFICATION_CHANNEL_IMPORTANCE)
                            ?: NotificationConst.DEFAULT_VALUE_NOTIFICATION_CHANNEL_IMPORTANCE
                    val notificationContentActivity =
                        extras?.getString(NotificationConst.KEY_NOTIFICATION_CONTENT_ACTIVITY)
                    val notificationContentBasePath =
                        extras?.getString(NotificationConst.KEY_NOTIFICATION_CONTENT_BASE_PATH)
                    val notificationChannelDescription =
                        extras?.getString(NotificationConst.KEY_NOTIFICATION_CHANNEL_DESCRIPTION)
                            ?: NotificationConst.DEFAULT_VALUE_NOTIFICATION_CHANNEL_DESCRIPTION
                    val notificationSmallIcon =
                        extras?.getInt(NotificationConst.KEY_NOTIFICATION_SMALL_ICON)
                            ?: NotificationConst.DEFAULT_VALUE_NOTIFICATION_SMALL_ICON
                    val fileName = extras?.getString(NotificationConst.KEY_FILE_NAME).orEmpty()
                    val totalBytes = extras?.getLong(NotificationConst.KEY_TOTAL_BYTES) ?: 0L
                    val currentProgress = if (totalBytes != 0L) {
                        (((extras?.getLong(DownloadWorker.KEY_DOWNLOADED_BYTES)
                            ?: 0) * 100) / totalBytes).toInt()
                    } else 0
                    val requestId = extras?.getInt(NotificationConst.KEY_DOWNLOAD_REQUEST_ID) ?: -1

                    val notificationId = requestId + 1 // unique id for the notification

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        createNotificationChannel(
                            context = context,
                            notificationChannelName = notificationChannelName,
                            notificationImportance = notificationImportance,
                            notificationChannelDescription = notificationChannelDescription
                        )
                    }

                    // Open Application (Send the unique download request id in intent)
                    val pendingIntentOpen = PendingIntent.getActivity(
                        context,
                        notificationId,
                        createOpenIntent(
                            context = context,
                            requestId = requestId,
                            notificationContentActivity = notificationContentActivity,
                            notificationContentBasePath = notificationContentBasePath,
                        ),
                        PendingIntent.FLAG_IMMUTABLE
                    )

                    // Resume Notification
                    val intentResume = Intent(context, NotificationReceiver::class.java).apply {
                        action = NotificationConst.ACTION_NOTIFICATION_RESUME_CLICK
                    }
                    intentResume.putExtra(NotificationConst.KEY_NOTIFICATION_ID, notificationId)
                    intentResume.putExtra(NotificationConst.KEY_DOWNLOAD_REQUEST_ID, requestId)
                    val pendingIntentResume = PendingIntent.getBroadcast(
                        context.applicationContext,
                        notificationId,
                        intentResume,
                        PendingIntent.FLAG_IMMUTABLE
                    )

                    // Retry Notification
                    val intentRetry = Intent(context, NotificationReceiver::class.java).apply {
                        action = NotificationConst.ACTION_NOTIFICATION_RETRY_CLICK
                    }
                    intentRetry.putExtra(NotificationConst.KEY_NOTIFICATION_ID, notificationId)
                    intentRetry.putExtra(NotificationConst.KEY_DOWNLOAD_REQUEST_ID, requestId)
                    val pendingIntentRetry = PendingIntent.getBroadcast(
                        context.applicationContext,
                        notificationId,
                        intentRetry,
                        PendingIntent.FLAG_IMMUTABLE
                    )

                    // Cancel Notification
                    val intentCancel = Intent(context, NotificationReceiver::class.java).apply {
                        action = NotificationConst.ACTION_NOTIFICATION_CANCEL_CLICK
                    }
                    intentCancel.putExtra(NotificationConst.KEY_NOTIFICATION_ID, notificationId)
                    intentCancel.putExtra(NotificationConst.KEY_DOWNLOAD_REQUEST_ID, requestId)
                    val pendingIntentCancel = PendingIntent.getBroadcast(
                        context.applicationContext,
                        notificationId,
                        intentCancel,
                        PendingIntent.FLAG_IMMUTABLE
                    )

                    var notificationBuilder = NotificationCompat.Builder(
                        context,
                        NotificationConst.NOTIFICATION_CHANNEL_ID
                    )
                        .setSmallIcon(notificationSmallIcon)
                        .setContentText(
                            when (intent.action) {
                                NotificationConst.ACTION_DOWNLOAD_COMPLETED -> context.getString(
                                    R.string.download_successful,
                                    TextUtil.getTotalLengthText(totalBytes)
                                )

                                NotificationConst.ACTION_DOWNLOAD_FAILED ->
                                    context.getString(R.string.download_failed)

                                NotificationConst.ACTION_DOWNLOAD_PAUSED ->
                                    context.getString(R.string.download_paused)

                                else -> context.getString(R.string.download_cancelled)
                            }
                        )
                        .setContentTitle(fileName)
                        .setContentIntent(pendingIntentOpen)
                        .setOnlyAlertOnce(true)
                        .setOngoing(false)
                        .setAutoCancel(true)

                    // add retry and cancel button for failed download
                    if (intent.action == NotificationConst.ACTION_DOWNLOAD_FAILED) {
                        notificationBuilder = notificationBuilder.addAction(
                            -1,
                            downloader.notificationConfig.retryText,
                            pendingIntentRetry
                        ).setProgress(
                            NotificationConst.MAX_VALUE_PROGRESS,
                            currentProgress,
                            false
                        ).addAction(
                            -1,
                            downloader.notificationConfig.cancelText,
                            pendingIntentCancel
                        ).setSubText("$currentProgress%")
                    }
                    // add resume and cancel button for paused download
                    if (intent.action == NotificationConst.ACTION_DOWNLOAD_PAUSED) {
                        notificationBuilder = notificationBuilder.addAction(
                            -1,
                            downloader.notificationConfig.resumeText,
                            pendingIntentResume,
                        ).setProgress(
                            NotificationConst.MAX_VALUE_PROGRESS,
                            currentProgress,
                            false
                        ).addAction(
                            -1,
                            downloader.notificationConfig.cancelText,
                            pendingIntentCancel
                        ).setSubText("$currentProgress%")
                    }

                    val notification = notificationBuilder.build()
                    NotificationManagerCompat.from(context).notify(notificationId, notification)
                }
            }
        }
    }

    /**
     * Create notification channel for File downloads
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(
        context: Context,
        notificationChannelName: String,
        notificationImportance: Int,
        notificationChannelDescription: String
    ) {
        val channel = NotificationChannel(
            NotificationConst.NOTIFICATION_CHANNEL_ID,
            notificationChannelName,
            notificationImportance
        )
        channel.description = notificationChannelDescription
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
