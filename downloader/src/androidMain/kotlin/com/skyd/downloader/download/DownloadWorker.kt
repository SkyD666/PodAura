package com.skyd.downloader.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import co.touchlab.kermit.Logger
import com.skyd.downloader.Downloader
import com.skyd.downloader.notification.DownloadNotificationManager
import com.skyd.downloader.notification.NotificationConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent

internal class DownloadWorker(
    private val context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters), KoinComponent {

    companion object {
        const val TAG = "DownloadWorker"
        internal const val INPUT_DATA_ID_KEY = "id"
        internal const val INPUT_DATA_NOTIFICATION_CONFIG_KEY = "notificationConfig"

        const val KEY_EXCEPTION = "keyException"
        const val EXCEPTION_NO_ENTITY = "No DownloadEntity"
    }

    private val log = Logger.withTag(TAG)
    private val scope = CoroutineScope(Dispatchers.Main)
    private var notificationManager: DownloadNotificationManager? = null
    private lateinit var notificationConfig: NotificationConfig
    private var id: Int? = null
    private var eventListenJob: Job? = null

    private suspend fun ensureNotificationManager(event: Event): DownloadNotificationManager {
        return notificationManager ?: DownloadNotificationManager(
            context = context,
            notificationConfig = notificationConfig,
            requestId = event.entity.id,
            fileName = event.entity.fileName
        ).apply {
            sendUpdateNotification()?.let { foregroundInfo ->
                runCatching {
                    setForeground(foregroundInfo)
                }.onFailure { e ->
                    log.e(
                        throwable = e,
                        message = { "Failed to setForeground in DownloadWorker" },
                    )
                }
            }
            notificationManager = this
        }
    }

    private suspend fun listenDownloadEvent() {
        Downloader.observeEvent().filter { id != null && it.entity.id == id }.collect { event ->
            when (event) {
                is Event.Failed -> {
                    ensureNotificationManager(event)
                        .sendDownloadFailedNotification(event.entity.downloadedBytes)
                }

                is Event.Paused -> {
                    ensureNotificationManager(event).sendDownloadPausedNotification(
                        downloadedBytes = event.entity.downloadedBytes,
                        totalBytes = event.entity.totalBytes,
                    )
                }

                is Event.Progress -> {
                    ensureNotificationManager(event).sendUpdateNotification(
                        downloadedBytes = event.entity.downloadedBytes,
                        speedInBPerMs = event.entity.speedInBytePerMs,
                        totalBytes = event.entity.totalBytes,
                    )?.let { foregroundInfo ->
                        runCatching {
                            setForeground(foregroundInfo)
                        }.onFailure { e ->
                            log.e(
                                throwable = e,
                                message = { "Failed to setForeground in DownloadWorker" },
                            )
                        }
                    }
                }

                is Event.Remove -> {
                    ensureNotificationManager(event).sendDownloadCancelledNotification()
                }

                is Event.Start -> {
                    ensureNotificationManager(event)
                }

                is Event.Success -> {
                    ensureNotificationManager(event)
                        .sendDownloadSuccessNotification(event.entity.totalBytes)
                }
            }
        }
    }

    override suspend fun doWork(): Result {
        val entityId = inputData.keyValueMap[INPUT_DATA_ID_KEY] as Int
        this.id = entityId
        this.notificationConfig = runCatching {
            Json.decodeFromString<NotificationConfig>(
                inputData.getString(INPUT_DATA_NOTIFICATION_CONFIG_KEY).orEmpty()
            )
        }.getOrNull() ?: return Result.failure(workDataOf(KEY_EXCEPTION to EXCEPTION_NO_ENTITY))

        eventListenJob = scope.launch {
            listenDownloadEvent()
        }
        val result = DownloadChecker().tryDownload(id = entityId)
        eventListenJob?.cancel()
        return if (result) {
            Result.success()
        } else {
            Result.failure()
        }
    }
}