package com.skyd.downloader.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.work.ForegroundInfo
import com.skyd.compone.component.blockString
import com.skyd.downloader.util.TextUtil
import podaura.downloader.generated.resources.Res
import podaura.downloader.generated.resources.download_cancel
import podaura.downloader.generated.resources.download_failed
import podaura.downloader.generated.resources.download_pause
import podaura.downloader.generated.resources.download_paused
import podaura.downloader.generated.resources.download_resume
import podaura.downloader.generated.resources.download_retry
import podaura.downloader.generated.resources.download_successful
import podaura.downloader.generated.resources.downloading_count
import podaura.downloader.generated.resources.downloading_title

internal class DownloadNotificationManager(
    private val context: Context,
    private val notificationConfig: NotificationConfig,
    private val taskId: String,
    private val fileName: String,
) {
    private val notificationId = notificationId(taskId)
    private val terminalNotificationId = terminalNotificationId(taskId)
    private val manager = NotificationManagerCompat.from(context)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NotificationConst.NOTIFICATION_CHANNEL_ID,
                notificationConfig.channelName,
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = notificationConfig.channelDescription }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    fun foregroundInfo(
        downloadedBytes: Long = 0,
        totalBytes: Long = 0,
        speedInBytePerMs: Float = 0f,
    ): ForegroundInfo {
        val knownLength = totalBytes > 0
        val progress = if (knownLength) {
            ((downloadedBytes * 100) / totalBytes).coerceIn(0, 100).toInt()
        } else {
            0
        }
        val notification = baseBuilder(ongoing = true, requestCode = notificationId)
            .setContentTitle(blockString(Res.string.downloading_title, fileName))
            .setContentText(progressText(downloadedBytes, totalBytes, speedInBytePerMs))
            .setProgress(100, progress, !knownLength)
            .setSubText(if (knownLength) "$progress%" else null)
            .addAction(
                -1,
                blockString(Res.string.download_pause),
                actionPendingIntent(
                    action = NotificationConst.ACTION_NOTIFICATION_PAUSE_CLICK,
                    targetNotificationId = notificationId,
                ),
            )
            .addAction(
                -1,
                blockString(Res.string.download_cancel),
                actionPendingIntent(
                    action = NotificationConst.ACTION_NOTIFICATION_CANCEL_CLICK,
                    targetNotificationId = notificationId,
                ),
            )
            .build()
        return ForegroundInfo(
            notificationId,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
    }

    fun showSuccess(totalBytes: Long) {
        showTerminal(
            title = blockString(
                Res.string.download_successful,
                TextUtil.getTotalLengthText(totalBytes),
            )
        )
    }

    fun showFailed() {
        showTerminal(
            title = blockString(Res.string.download_failed),
            actionTitle = blockString(Res.string.download_retry),
            action = NotificationConst.ACTION_NOTIFICATION_RETRY_CLICK,
        )
    }

    fun showPaused(downloadedBytes: Long, totalBytes: Long) {
        showTerminal(
            title = blockString(Res.string.download_paused),
            text = progressText(downloadedBytes, totalBytes, 0f),
            actionTitle = blockString(Res.string.download_resume),
            action = NotificationConst.ACTION_NOTIFICATION_RESUME_CLICK,
        )
    }

    @SuppressLint("MissingPermission")
    fun updateGroupSummary(activeCount: Int) {
        if (activeCount <= 0) {
            manager.cancel(NotificationConst.GROUP_SUMMARY_NOTIFICATION_ID)
            return
        }
        if (!canPostNotifications()) return
        val summary = NotificationCompat.Builder(context, NotificationConst.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(notificationConfig.smallIcon)
            .setContentTitle(blockString(Res.string.downloading_count, activeCount))
            .setContentIntent(openPendingIntent(NotificationConst.GROUP_SUMMARY_NOTIFICATION_ID))
            .setGroup(NotificationConst.NOTIFICATION_GROUP_KEY)
            .setGroupSummary(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        runCatching {
            manager.notify(NotificationConst.GROUP_SUMMARY_NOTIFICATION_ID, summary)
        }
    }

    @SuppressLint("MissingPermission")
    private fun showTerminal(
        title: String,
        text: String? = null,
        actionTitle: String? = null,
        action: String? = null,
    ) {
        if (!canPostNotifications()) return
        val builder = baseBuilder(
            ongoing = false,
            requestCode = terminalNotificationId,
        )
            .setContentTitle(title)
            .setContentText(text)
            .setProgress(0, 0, false)
            .setAutoCancel(true)
        if (actionTitle != null && action != null) {
            builder.addAction(
                -1,
                actionTitle,
                actionPendingIntent(action, terminalNotificationId),
            )
        }
        runCatching { manager.notify(terminalNotificationId, builder.build()) }
    }

    private fun baseBuilder(
        ongoing: Boolean,
        requestCode: Int,
    ): NotificationCompat.Builder =
        NotificationCompat.Builder(context, NotificationConst.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(notificationConfig.smallIcon)
            .setContentIntent(openPendingIntent(requestCode))
            .setGroup(NotificationConst.NOTIFICATION_GROUP_KEY)
            .setOnlyAlertOnce(true)
            .setOngoing(ongoing)

    private fun openPendingIntent(requestCode: Int): PendingIntent = PendingIntent.getActivity(
        context,
        requestCode,
        createOpenIntent(
            context = context,
            taskId = taskId,
            notificationContentActivity = notificationConfig.intentContentActivity,
            notificationContentBasePath = notificationConfig.intentContentBasePath,
        ),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun actionPendingIntent(
        action: String,
        targetNotificationId: Int,
    ): PendingIntent = PendingIntent.getBroadcast(
        context,
        targetNotificationId,
        Intent(context, NotificationReceiver::class.java).apply {
            this.action = action
            putExtra(NotificationConst.KEY_DOWNLOAD_TASK_ID, taskId)
            putExtra(NotificationConst.KEY_NOTIFICATION_ID, targetNotificationId)
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun progressText(downloadedBytes: Long, totalBytes: Long, speed: Float): String {
        val size = if (totalBytes > 0) {
            "${TextUtil.getTotalLengthText(downloadedBytes)} / ${
                TextUtil.getTotalLengthText(
                    totalBytes
                )
            }"
        } else {
            TextUtil.getTotalLengthText(downloadedBytes)
        }
        return if (speed > 0) "${TextUtil.getSpeedText(speed)}  $size" else size
    }

    private fun canPostNotifications(): Boolean {
        if (!notificationConfig.enabled || !manager.areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
    }

    companion object {
        fun notificationId(taskId: String): Int {
            return taskId.hashCode() and 0x3FFFFFFF
        }

        fun terminalNotificationId(taskId: String): Int =
            notificationId(taskId) or 0x40000000

        private fun createOpenIntent(
            context: Context,
            taskId: String,
            notificationContentActivity: String?,
            notificationContentBasePath: String?,
        ): Intent {
            val intent = if (notificationContentActivity == null) {
                context.packageManager.getLaunchIntentForPackage(context.packageName)
            } else {
                Intent().apply {
                    component = ComponentName(context, notificationContentActivity)
                    data = notificationContentBasePath?.toUri()
                }
            } ?: Intent()
            return intent.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(NotificationConst.KEY_DOWNLOAD_TASK_ID, taskId)
            }
        }
    }
}
