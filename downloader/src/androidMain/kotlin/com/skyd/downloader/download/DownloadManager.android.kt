package com.skyd.downloader.download

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.skyd.downloader.Status
import com.skyd.downloader.UserAction
import com.skyd.downloader.db.DownloadEntity
import com.skyd.downloader.notification.NotificationConfig
import com.skyd.downloader.util.FileUtil.deleteDownloadFileIfExists
import com.skyd.downloader.util.NotificationUtil.removeNotification
import com.skyd.fundation.di.get
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual class DownloadManager : BaseDownloadManager(), KoinComponent {
    private val context by inject<Context>()
    private val workManager by lazy { WorkManager.getInstance(context) }

    actual override suspend fun download(downloadRequest: DownloadRequest) {
        val downloadWorkRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                Data.Builder()
                    .putInt(DownloadWorker.INPUT_DATA_ID_KEY, downloadRequest.id)
                    .putString(
                        DownloadWorker.INPUT_DATA_NOTIFICATION_CONFIG_KEY,
                        Json.encodeToString(get<NotificationConfig>())
                    )
                    .build()
            )
            .build()
        var oldDownloadEntity = downloadDao.find(downloadRequest.id)
        // Checks if download id already present in database
        if (oldDownloadEntity != null) {
            oldDownloadEntity = oldDownloadEntity.copy(userAction = UserAction.Start.toString())
            downloadDao.update(oldDownloadEntity)

            // In case new download request is generated for already existing id in database
            // and work is not in progress, replace the uuid in database
            if (oldDownloadEntity.workerUuid != downloadWorkRequest.id.toString() &&
                oldDownloadEntity.status != Status.Queued.toString() &&
                oldDownloadEntity.status != Status.Downloading.toString() &&
                oldDownloadEntity.status != Status.Started.toString()
            ) {
                downloadDao.update(
                    oldDownloadEntity.copy(
                        workerUuid = downloadWorkRequest.id.toString(),
                        status = Status.Queued.toString(),
                    )
                )
            }
        } else {
            downloadDao.insert(
                DownloadEntity(
                    url = downloadRequest.url,
                    path = downloadRequest.path,
                    fileName = downloadRequest.fileName,
                    id = downloadRequest.id,
                    timeQueued = System.currentTimeMillis(),
                    status = Status.Queued.toString(),
                    workerUuid = downloadWorkRequest.id.toString(),
                    userAction = UserAction.Start.toString(),
                )
            )

            deleteDownloadFileIfExists(downloadRequest.path, downloadRequest.fileName)
        }

        workManager.enqueueUniqueWork(
            downloadRequest.id.toString(),
            ExistingWorkPolicy.KEEP,
            downloadWorkRequest
        )
    }

    override suspend fun onPause(id: Int) {
        workManager.cancelUniqueWork(id.toString())
    }

    override suspend fun onClearDbAsync(id: Int) {
        workManager.cancelUniqueWork(id.toString())
        removeNotification(context, id)
    }

    override suspend fun onClearDbAsyncEach(id: Int) {
        workManager.cancelUniqueWork(id.toString())
        removeNotification(context, id)
    }
}