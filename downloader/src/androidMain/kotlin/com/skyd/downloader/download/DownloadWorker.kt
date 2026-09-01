package com.skyd.downloader.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.skyd.downloader.ACTIVE_DOWNLOAD_STATUS_NAMES
import com.skyd.downloader.Status
import com.skyd.downloader.db.DownloadDao
import com.skyd.downloader.isActiveDownloadStatus
import com.skyd.downloader.notification.DownloadNotificationManager
import com.skyd.downloader.notification.NotificationConfig
import com.skyd.fundation.di.get
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.koin.core.component.KoinComponent
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

internal class DownloadWorker(
    context: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(context, workerParameters), KoinComponent {
    override suspend fun doWork(): Result {
        val taskId = inputData.getString(INPUT_DATA_ID_KEY)
            ?: return failure("Missing task id")
        val attemptId = inputData.getString(INPUT_DATA_ATTEMPT_ID_KEY)
            ?: return failure("Missing attempt id")
        val notificationConfig: NotificationConfig = get()
        val downloadDao: DownloadDao = get()
        val executor: DownloadExecutor = get()
        val initial = downloadDao.find(taskId)
            ?.takeIf {
                it.attemptId == attemptId && it.status.isActiveDownloadStatus()
            }
            ?: return Result.success()
        downloadDao.markInterruptedQueued(
            id = taskId,
            attemptId = attemptId,
            updatedTime = Clock.System.now().toEpochMilliseconds(),
        )
        val notifications = DownloadNotificationManager(
            context = applicationContext,
            notificationConfig = notificationConfig,
            taskId = taskId,
            fileName = initial.fileName,
        )

        try {
            try {
                setForeground(
                    notifications.foregroundInfo(
                        downloadedBytes = initial.downloadedBytes,
                        totalBytes = initial.totalBytes,
                    )
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                downloadDao.markFailed(
                    id = taskId,
                    attemptId = attemptId,
                    reason = "Unable to run download in foreground",
                    code = DownloadFailureCode.Unknown.name,
                    updatedTime = Clock.System.now().toEpochMilliseconds(),
                )
                notifications.showFailed()
                return failure("Unable to run download in foreground")
            }
            notifications.updateGroupSummary(activeDownloadCount(downloadDao))

            while (true) {
                val current = downloadDao.find(taskId)
                    ?.takeIf { it.attemptId == attemptId }
                    ?: return Result.success()
                val remainingDelay =
                    current.nextAttemptAt - Clock.System.now().toEpochMilliseconds()
                if (remainingDelay > 0) delay(remainingDelay.milliseconds)

                when (
                    val execution = executor.execute(taskId, attemptId) { progress ->
                        setForeground(
                            notifications.foregroundInfo(
                                downloadedBytes = progress.downloadedBytes,
                                totalBytes = progress.totalBytes,
                                speedInBytePerMs = progress.speedInBytePerMs,
                            )
                        )
                    }
                ) {
                    is DownloadExecutionResult.Success -> {
                        notifications.showSuccess(execution.totalBytes)
                        notifications.updateGroupSummary(activeDownloadCount(downloadDao))
                        return Result.success()
                    }

                    is DownloadExecutionResult.Retry -> continue
                    DownloadExecutionResult.Failed -> {
                        notifications.showFailed()
                        notifications.updateGroupSummary(activeDownloadCount(downloadDao))
                        return Result.failure()
                    }

                    DownloadExecutionResult.Ignored -> {
                        notifications.updateGroupSummary(activeDownloadCount(downloadDao))
                        return Result.success()
                    }
                }
            }
        } catch (error: CancellationException) {
            val current = downloadDao.find(taskId)
            if (current?.status == Status.Paused.name) {
                notifications.showPaused(current.downloadedBytes, current.totalBytes)
            }
            notifications.updateGroupSummary(activeDownloadCount(downloadDao))
            throw error
        }
    }

    private suspend fun activeDownloadCount(downloadDao: DownloadDao): Int =
        downloadDao.countInStatuses(ACTIVE_DOWNLOAD_STATUS_NAMES)

    private fun failure(message: String): Result =
        Result.failure(workDataOf(KEY_EXCEPTION to message))

    companion object {
        internal const val INPUT_DATA_ID_KEY = "id"
        internal const val INPUT_DATA_ATTEMPT_ID_KEY = "attemptId"
        private const val KEY_EXCEPTION = "exception"
    }
}
