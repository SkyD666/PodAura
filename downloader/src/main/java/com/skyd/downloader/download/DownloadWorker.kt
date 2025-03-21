package com.skyd.downloader.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.skyd.downloader.NotificationConfig
import com.skyd.downloader.Status
import com.skyd.downloader.UserAction
import com.skyd.downloader.db.DatabaseInstance
import com.skyd.downloader.net.RetrofitInstance
import com.skyd.downloader.notification.DownloadNotificationManager
import com.skyd.downloader.util.FileUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

internal class DownloadWorker(
    private val context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    companion object {
        internal const val INPUT_DATA_ID_KEY = "id"
        internal const val INPUT_DATA_NOTIFICATION_CONFIG_KEY = "notificationConfig"

        const val KEY_EXCEPTION = "keyException"
        const val EXCEPTION_NO_ENTITY = "No DownloadEntity"

        const val KEY_STATE = "keyState"
        const val KEY_DOWNLOADED_BYTES = "keyDownloadedBytes"
        const val DOWNLOADING_STATE = "downloading"
        const val STARTED_STATE = "started"

        private val scope = CoroutineScope(Dispatchers.IO)
    }

    private var downloadNotificationManager: DownloadNotificationManager? = null
    private val downloadDao = DatabaseInstance.getInstance(context).downloadDao()

    override suspend fun doWork(): Result {
        val entityId = inputData.keyValueMap[INPUT_DATA_ID_KEY] as Int
        val downloadRequest = downloadDao.find(entityId) ?: return Result.failure(
            workDataOf(KEY_EXCEPTION to EXCEPTION_NO_ENTITY)
        )

        val notificationConfig: NotificationConfig = runCatching {
            Json.decodeFromString<NotificationConfig>(
                inputData.getString(INPUT_DATA_NOTIFICATION_CONFIG_KEY).orEmpty()
            )
        }.getOrNull() ?: return Result.failure(workDataOf(KEY_EXCEPTION to EXCEPTION_NO_ENTITY))

        val id = downloadRequest.id
        val url = downloadRequest.url
        val dirPath = downloadRequest.path
        val fileName = downloadRequest.fileName

        downloadNotificationManager = DownloadNotificationManager(
            context = context,
            notificationConfig = notificationConfig,
            requestId = id,
            fileName = fileName
        )

        val downloadService = RetrofitInstance.getDownloadService()

        return try {
            downloadNotificationManager?.sendUpdateNotification()?.let { setForeground(it) }

            val latestETag = downloadService.getHeadersOnly(url).headers()
                .get(DownloadTask.ETAG_HEADER).orEmpty()
            val existingETag = downloadDao.find(id)?.eTag.orEmpty()
            if (latestETag != existingETag) {
                FileUtil.deleteDownloadFileIfExists(path = dirPath, name = fileName)
                downloadDao.find(id)?.copy(eTag = latestETag)?.let { downloadDao.update(it) }
            }

            var progressPercentage = 0

            val totalLength = DownloadTask(
                url = url,
                path = dirPath,
                fileName = fileName,
                downloadService = downloadService
            ).download(
                onStart = { length ->
                    downloadDao.find(id)?.copy(
                        totalBytes = length,
                        status = Status.Started.toString(),
                    )?.let { downloadDao.update(it) }

                    setProgress(workDataOf(KEY_STATE to STARTED_STATE))
                },
                onProgress = { downloadedBytes, length, speed ->
                    val progress = if (length != 0L) {
                        ((downloadedBytes * 100) / length).toInt()
                    } else {
                        0
                    }

                    if (progressPercentage != progress) {
                        progressPercentage = progress
                        downloadDao.find(id)?.copy(
                            downloadedBytes = downloadedBytes,
                            speedInBytePerMs = speed,
                            status = Status.Downloading.toString(),
                        )?.let { downloadDao.update(it) }
                    }

                    setProgress(
                        workDataOf(
                            KEY_STATE to DOWNLOADING_STATE,
                            KEY_DOWNLOADED_BYTES to downloadedBytes
                        )
                    )
                    downloadNotificationManager?.sendUpdateNotification(
                        downloadedBytes = downloadedBytes,
                        speedInBPerMs = speed,
                        totalBytes = length,
                    )?.let { setForeground(it) }
                }
            )

            downloadDao.find(id)?.copy(
                totalBytes = totalLength,
                status = Status.Success.toString(),
            )?.let { downloadDao.update(it) }

            downloadNotificationManager?.sendDownloadSuccessNotification(totalLength)

            downloadDao.find(id)?.let { DownloadEvent.sendEvent(Event.Success(it)) }
            Result.success()
        } catch (e: Exception) {
            scope.launch {
                if (e is CancellationException) {
                    var downloadEntity = downloadDao.find(id)
                    if (downloadEntity?.userAction == UserAction.Pause.toString()) {
                        downloadEntity = downloadEntity.copy(status = Status.Paused.toString())
                        downloadDao.update(downloadEntity)
                        downloadNotificationManager?.sendDownloadPausedNotification(
                            downloadedBytes = downloadEntity.downloadedBytes,
                            totalBytes = downloadEntity.totalBytes,
                        )
                    } else {
                        downloadDao.remove(id)
                        FileUtil.deleteDownloadFileIfExists(dirPath, fileName)
                        downloadNotificationManager?.sendDownloadCancelledNotification()
                    }
                } else {
                    e.printStackTrace()
                    var downloadEntity = downloadDao.find(id)
                    if (downloadEntity != null) {
                        downloadEntity = downloadEntity.copy(
                            status = Status.Failed.toString(),
                            failureReason = e.message.orEmpty(),
                        )
                        downloadDao.update(downloadEntity)
                        downloadNotificationManager?.sendDownloadFailedNotification(
                            downloadedBytes = downloadEntity.downloadedBytes
                        )
                    }
                }
            }
            Result.failure(workDataOf(KEY_EXCEPTION to e.message))
        }
    }
}
