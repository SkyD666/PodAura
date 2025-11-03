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
import com.skyd.compone.component.blockString
import com.skyd.downloader.Downloader
import com.skyd.downloader.notification.DownloadNotificationManager.Companion.KEY_DOWNLOADED_BYTES
import com.skyd.downloader.notification.DownloadNotificationManager.Companion.createOpenIntent
import com.skyd.downloader.util.TextUtil
import com.skyd.fundation.di.get
import podaura.downloader.generated.resources.Res
import podaura.downloader.generated.resources.download_cancel
import podaura.downloader.generated.resources.download_cancelled
import podaura.downloader.generated.resources.download_failed
import podaura.downloader.generated.resources.download_paused
import podaura.downloader.generated.resources.download_resume
import podaura.downloader.generated.resources.download_retry
import podaura.downloader.generated.resources.download_successful

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
        val downloader: Downloader = get()

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
                        (((extras?.getLong(KEY_DOWNLOADED_BYTES) ?: 0) * 100) / totalBytes).toInt()
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
                                NotificationConst.ACTION_DOWNLOAD_COMPLETED -> blockString(
                                    Res.string.download_successful,
                                    TextUtil.getTotalLengthText(totalBytes)
                                )

                                NotificationConst.ACTION_DOWNLOAD_FAILED ->
                                    blockString(Res.string.download_failed)

                                NotificationConst.ACTION_DOWNLOAD_PAUSED ->
                                    blockString(Res.string.download_paused)

                                else -> blockString(Res.string.download_cancelled)
                            }
                        )
                        .setContentTitle(fileName)
                        .setContentIntent(pendingIntentOpen)
                        .setOnlyAlertOnce(true)
                        .setOngoing(false)
                        .setAutoCancel(true)

                    // add retry and cancel button for failed download
                    val retryText = blockString(Res.string.download_retry)
                    val cancelText = blockString(Res.string.download_cancel)
                    val resumeText = blockString(Res.string.download_resume)
                    if (intent.action == NotificationConst.ACTION_DOWNLOAD_FAILED) {
                        notificationBuilder = notificationBuilder
                            .addAction(-1, retryText, pendingIntentRetry)
                            .setProgress(
                                NotificationConst.MAX_VALUE_PROGRESS,
                                currentProgress,
                                false
                            )
                            .addAction(-1, cancelText, pendingIntentCancel)
                            .setSubText("$currentProgress%")
                    }
                    // add resume and cancel button for paused download
                    if (intent.action == NotificationConst.ACTION_DOWNLOAD_PAUSED) {
                        notificationBuilder = notificationBuilder
                            .addAction(-1, resumeText, pendingIntentResume)
                            .setProgress(
                                NotificationConst.MAX_VALUE_PROGRESS,
                                currentProgress,
                                false
                            )
                            .addAction(-1, cancelText, pendingIntentCancel)
                            .setSubText("$currentProgress%")
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
