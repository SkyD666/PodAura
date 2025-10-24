package com.skyd.downloader.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.skyd.downloader.NotificationConfig
import com.skyd.downloader.notification.DownloadNotificationManager
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent

internal class DownloadWorker(
    private val context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters), KoinComponent {

    companion object {
        internal const val INPUT_DATA_ID_KEY = "id"
        internal const val INPUT_DATA_NOTIFICATION_CONFIG_KEY = "notificationConfig"

        const val KEY_EXCEPTION = "keyException"
        const val EXCEPTION_NO_ENTITY = "No DownloadEntity"

        const val KEY_STATE = "keyState"
        const val KEY_DOWNLOADED_BYTES = "keyDownloadedBytes"
        const val DOWNLOADING_STATE = "downloading"
        const val STARTED_STATE = "started"
    }

    private var notificationManager: DownloadNotificationManager? = null

    override suspend fun doWork(): Result {
        val entityId = inputData.keyValueMap[INPUT_DATA_ID_KEY] as Int
        val notificationConfig: NotificationConfig = runCatching {
            Json.decodeFromString<NotificationConfig>(
                inputData.getString(INPUT_DATA_NOTIFICATION_CONFIG_KEY).orEmpty()
            )
        }.getOrNull() ?: return Result.failure(workDataOf(KEY_EXCEPTION to EXCEPTION_NO_ENTITY))

        val result = DownloadChecker().tryDownload(
            id = entityId,
            onStart = {
                notificationManager = DownloadNotificationManager(
                    context = context,
                    notificationConfig = notificationConfig,
                    requestId = it.id,
                    fileName = it.fileName
                )
                notificationManager?.sendUpdateNotification()?.let { setForeground(it) }
                setProgress(workDataOf(KEY_STATE to STARTED_STATE))
            },
            onProgress = { downloadedBytes: Long, totalBytes: Long, speedInBPerMs: Float ->
                setProgress(
                    workDataOf(
                        KEY_STATE to DOWNLOADING_STATE,
                        KEY_DOWNLOADED_BYTES to downloadedBytes
                    )
                )
                notificationManager?.sendUpdateNotification(
                    downloadedBytes = downloadedBytes,
                    speedInBPerMs = speedInBPerMs,
                    totalBytes = totalBytes,
                )?.let { setForeground(it) }
            },
            onPaused = { downloadedBytes: Long, totalBytes: Long ->
                notificationManager?.sendDownloadPausedNotification(
                    downloadedBytes = downloadedBytes,
                    totalBytes = totalBytes,
                )
            },
            onCanceled = {
                notificationManager?.sendDownloadCancelledNotification()
            },
            onSuccess = {
                notificationManager?.sendDownloadSuccessNotification(it)
            },
            onFailed = {
                notificationManager?.sendDownloadFailedNotification(downloadedBytes = it)
            },
        )
        return if (result) {
            Result.success()
        } else {
            Result.failure()
        }
    }
}
