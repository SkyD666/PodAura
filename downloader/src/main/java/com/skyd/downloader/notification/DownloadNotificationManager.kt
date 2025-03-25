package com.skyd.downloader.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.work.ForegroundInfo
import com.skyd.downloader.NotificationConfig
import com.skyd.downloader.R
import com.skyd.downloader.download.DownloadWorker
import com.skyd.downloader.util.TextUtil

/**
 * Download notification manager: Responsible for showing the in progress notification for each downloads.
 * Whenever the download is cancelled or paused or failed (terminating state), WorkManager cancels the
 * ongoing notification and this class sends the broadcast to show terminating state notifications.
 *
 * Notification ID = Download request ID for each download.
 *
 * @property context Application context
 * @property notificationConfig [NotificationConfig]
 * @property requestId Unique ID for current download
 * @property fileName File name of the download
 * @constructor Create empty Download notification manager
 */
internal class DownloadNotificationManager(
    private val context: Context,
    private val notificationConfig: NotificationConfig,
    private val requestId: Int,
    private val fileName: String
) {

    private var foregroundInfo: ForegroundInfo? = null
    private lateinit var notificationBuilder: NotificationCompat.Builder

    private val notificationId = requestId // notification id is same as request id

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        initNotificationBuilder()
    }

    private fun initNotificationBuilder() {
        // Open Application (Send the unique download request id in intent)
        val intentOpen = createOpenIntent(
            context = context,
            requestId = requestId,
            notificationContentActivity = notificationConfig.intentContentActivity,
            notificationContentBasePath = notificationConfig.intentContentBasePath,
        )

        // Dismiss Notification
        val intentDismiss = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationConst.ACTION_NOTIFICATION_DISMISSED
            putExtra(NotificationConst.KEY_NOTIFICATION_ID, notificationId)
        }

        // Pause Notification
        val intentPause = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationConst.ACTION_NOTIFICATION_PAUSE_CLICK
            putExtra(NotificationConst.KEY_NOTIFICATION_ID, notificationId)
            putExtra(NotificationConst.KEY_DOWNLOAD_REQUEST_ID, requestId)
        }

        // Cancel Notification
        val intentCancel = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationConst.ACTION_NOTIFICATION_CANCEL_CLICK
            putExtra(NotificationConst.KEY_NOTIFICATION_ID, notificationId)
            putExtra(NotificationConst.KEY_DOWNLOAD_REQUEST_ID, requestId)
        }

        notificationBuilder = NotificationCompat.Builder(
            context, NotificationConst.NOTIFICATION_CHANNEL_ID
        )
            .setSmallIcon(notificationConfig.smallIcon).setOnlyAlertOnce(true)
            .setOngoing(true)
            .addAction(
                -1,
                context.getString(notificationConfig.pauseText),
                getBroadcastPendingIntent(intentPause),
            )
            .addAction(
                -1,
                context.getString(notificationConfig.cancelText),
                getBroadcastPendingIntent(intentCancel),
            )
            .setContentIntent(
                PendingIntent.getActivity(
                    context.applicationContext,
                    notificationId,
                    intentOpen,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .setDeleteIntent(getBroadcastPendingIntent(intentDismiss))
    }

    private fun getBroadcastPendingIntent(intent: Intent) = PendingIntent.getBroadcast(
        context.applicationContext,
        notificationId,
        intent,
        PendingIntent.FLAG_IMMUTABLE
    )

    /**
     * Send update notification: Shows the current in progress download notification, which cannot be dismissed
     *
     * @param downloadedBytes current downloaded bytes
     * @param speedInBPerMs current download speed in byte per second
     * @param totalBytes current length of download file in bytes
     * @return ForegroundInfo to be set in Worker
     */
    fun sendUpdateNotification(
        downloadedBytes: Long = 0L,
        speedInBPerMs: Float = 0F,
        totalBytes: Long = 0L,
    ): ForegroundInfo? {
        val progress = if (totalBytes != 0L) {
            ((downloadedBytes * 100) / totalBytes).toInt()
        } else {
            0
        }

        foregroundInfo = ForegroundInfo(
            notificationId,
            notificationBuilder
                .setContentTitle(context.getString(R.string.downloading_title, fileName))
                .setProgress(NotificationConst.MAX_VALUE_PROGRESS, progress, false)
                .setContentText(
                    setContentTextNotification(speedInBPerMs, downloadedBytes, totalBytes)
                )
                .setSubText("$progress%")
                .build(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
        )
        return foregroundInfo
    }

    /**
     * Set content text notification
     *
     * @param speedInBPerMs speed in byte per second of download
     * @param downloadedBytes current downloaded bytes
     * @param length total size of progress
     * @return Return the text to be displayed on in-progress download notification
     */
    private fun setContentTextNotification(
        speedInBPerMs: Float,
        downloadedBytes: Long,
        length: Long
    ): String {
        val speedText = TextUtil.getSpeedText(speedInBPerMs)
        val downloadedText = TextUtil.getTotalLengthText(downloadedBytes)
        val lengthText = TextUtil.getTotalLengthText(length)

        val parts = mutableListOf<String>()

        if (speedText.isNotEmpty()) {
            parts.add(speedText)
        }
        if (lengthText.isNotEmpty()) {
            parts.add("$downloadedText / $lengthText")
        }

        return parts.joinToString("  ")
    }

    /**
     * Send broadcast to show download success notification
     *
     * @param totalLength
     */
    fun sendDownloadSuccessNotification(totalLength: Long) {
        context.applicationContext.sendBroadcast(
            Intent(context, NotificationReceiver::class.java).apply {
                putExtrasForReceiver()
                putExtra(NotificationConst.KEY_TOTAL_BYTES, totalLength)
                action = NotificationConst.ACTION_DOWNLOAD_COMPLETED
            }
        )
    }

    /**
     * Send broadcast to show download failed notification
     *
     * @param downloadedBytes current downloaded bytes
     */
    fun sendDownloadFailedNotification(downloadedBytes: Long) {
        context.applicationContext.sendBroadcast(
            Intent(context, NotificationReceiver::class.java).apply {
                putExtrasForReceiver()
                putExtra(DownloadWorker.KEY_DOWNLOADED_BYTES, downloadedBytes)
                action = NotificationConst.ACTION_DOWNLOAD_FAILED
            }
        )
    }

    /**
     * Send broadcast to show download cancelled notification
     *
     */
    fun sendDownloadCancelledNotification() {
        context.applicationContext.sendBroadcast(
            Intent(context, NotificationReceiver::class.java).apply {
                putExtrasForReceiver()
                action = NotificationConst.ACTION_DOWNLOAD_CANCELLED
            }
        )
    }

    /**
     * Send broadcast to show download paused notification
     *
     * @param downloadedBytes current downloaded bytes
     * @param totalBytes total bytes
     */
    fun sendDownloadPausedNotification(
        downloadedBytes: Long,
        totalBytes: Long,
    ) {
        context.applicationContext.sendBroadcast(
            Intent(context, NotificationReceiver::class.java).apply {
                putExtrasForReceiver()
                putExtra(DownloadWorker.KEY_DOWNLOADED_BYTES, downloadedBytes)
                putExtra(NotificationConst.KEY_TOTAL_BYTES, totalBytes)
                action = NotificationConst.ACTION_DOWNLOAD_PAUSED
            }
        )
    }

    private fun Intent.putExtrasForReceiver() = apply {
        putExtra(
            NotificationConst.KEY_NOTIFICATION_CHANNEL_NAME,
            notificationConfig.channelName
        )
        putExtra(
            NotificationConst.KEY_NOTIFICATION_CHANNEL_IMPORTANCE,
            notificationConfig.importance
        )
        putExtra(
            NotificationConst.KEY_NOTIFICATION_CONTENT_ACTIVITY,
            notificationConfig.intentContentActivity
        )
        putExtra(
            NotificationConst.KEY_NOTIFICATION_CONTENT_BASE_PATH,
            notificationConfig.intentContentBasePath
        )
        putExtra(
            NotificationConst.KEY_NOTIFICATION_CHANNEL_DESCRIPTION,
            notificationConfig.channelDescription
        )
        putExtra(
            NotificationConst.KEY_NOTIFICATION_SMALL_ICON,
            notificationConfig.smallIcon
        )
        putExtra(NotificationConst.KEY_FILE_NAME, fileName)
        putExtra(NotificationConst.KEY_DOWNLOAD_REQUEST_ID, requestId)
        putExtra(NotificationConst.KEY_NOTIFICATION_ID, notificationId)
    }

    /**
     * Create notification channel for File downloads
     *
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NotificationConst.NOTIFICATION_CHANNEL_ID,
            notificationConfig.channelName,
            notificationConfig.importance
        )
        channel.description = notificationConfig.channelDescription
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        fun createOpenIntent(
            context: Context,
            requestId: Int,
            notificationContentActivity: String?,
            notificationContentBasePath: String?,
        ): Intent? {
            // Open Application (Send the unique download request id in intent)
            return if (notificationContentActivity == null) {
                context.packageManager.getLaunchIntentForPackage(context.packageName)?.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                )
            } else {
                Intent().apply {
                    component = ComponentName(context, notificationContentActivity)
                    data = notificationContentBasePath?.toUri()
                }
            }?.apply {
                putExtra(NotificationConst.KEY_DOWNLOAD_REQUEST_ID, requestId)
            }
        }
    }
}
