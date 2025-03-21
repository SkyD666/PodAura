package com.skyd.downloader.download

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.skyd.downloader.NotificationConfig
import com.skyd.downloader.Status
import com.skyd.downloader.UserAction
import com.skyd.downloader.db.DownloadDao
import com.skyd.downloader.db.DownloadEntity
import com.skyd.downloader.download.DownloadRequest.Companion.toDownloadRequest
import com.skyd.downloader.util.FileUtil.deleteDownloadFileIfExists
import com.skyd.downloader.util.NotificationUtil.removeNotification
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID

internal class DownloadManager(
    private val context: Context,
    private val downloadDao: DownloadDao,
    private val notificationConfig: NotificationConfig,
    private val workManager: WorkManager,
) {
    companion object {
        const val TAG = "DownloadManager"
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.i(TAG, "Exception in DownloadManager Scope: ${throwable.message}")
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + exceptionHandler)

    private suspend fun download(downloadRequest: DownloadRequest) {
        val downloadWorkRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                Data.Builder()
                    .putInt(DownloadWorker.INPUT_DATA_ID_KEY, downloadRequest.id)
                    .putString(
                        DownloadWorker.INPUT_DATA_NOTIFICATION_CONFIG_KEY,
                        Json.encodeToString(notificationConfig)
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

    private suspend fun resume(id: Int) {
        val downloadEntity = downloadDao.find(id)
        if (downloadEntity != null) {
            downloadDao.update(downloadEntity.copy(userAction = UserAction.Resume.toString()))
            download(downloadEntity.toDownloadRequest())
        }
    }

    private suspend fun pause(id: Int) {
        val downloadEntity = downloadDao.find(id)
        if (downloadEntity != null) {
            downloadDao.update(downloadEntity.copy(userAction = UserAction.Pause.toString()))
        }
        workManager.cancelUniqueWork(id.toString())
    }

    private suspend fun retry(id: Int) {
        val downloadEntity = downloadDao.find(id)
        if (downloadEntity != null) {
            downloadDao.update(downloadEntity.copy(userAction = UserAction.Retry.toString()))
            download(downloadEntity.toDownloadRequest())
        }
    }

    private suspend fun findDownloadEntityFromUUID(uuid: UUID): DownloadEntity? {
        return downloadDao.getAllEntity().find { it.workerUuid == uuid.toString() }
    }

    fun resumeAsync(id: Int) = scope.launch {
        resume(id)
    }

    fun resumeAllAsync() = scope.launch {
        downloadDao.getAllEntity().forEach {
            resume(it.id)
        }
    }

    fun pauseAsync(id: Int) = scope.launch {
        pause(id)
    }

    fun pauseAllAsync() = scope.launch {
        downloadDao.getAllEntity().forEach {
            pause(it.id)
        }
    }

    fun retryAsync(id: Int) = scope.launch {
        retry(id)
    }

    fun retryAllAsync() = scope.launch {
        downloadDao.getAllEntity().forEach {
            retry(it.id)
        }
    }

    fun clearDbAsync(id: Int, deleteFile: Boolean) = scope.launch {
        workManager.cancelUniqueWork(id.toString())
        val downloadEntity = downloadDao.find(id)
        val path = downloadEntity?.path
        val fileName = downloadEntity?.fileName
        if (path != null && fileName != null && deleteFile) {
            deleteDownloadFileIfExists(path, fileName)
        }
        removeNotification(context, id)
        downloadDao.find(id)?.let { DownloadEvent.sendEvent(Event.Remove(it)) }
        downloadDao.remove(id)
    }

    fun clearAllDbAsync(deleteFile: Boolean) = scope.launch {
        downloadDao.getAllEntity().forEach {
            workManager.cancelUniqueWork(it.id.toString())
            val downloadEntity = downloadDao.find(it.id)
            val path = downloadEntity?.path
            val fileName = downloadEntity?.fileName
            if (path != null && fileName != null && deleteFile) {
                deleteDownloadFileIfExists(path, fileName)
            }
            removeNotification(context, it.id)
            DownloadEvent.sendEvent(Event.Remove(it))
        }
        downloadDao.deleteAll()
    }

    fun clearDbAsync(timeInMillis: Long, deleteFile: Boolean) = scope.launch {
        downloadDao.getEntityTillTime(timeInMillis).forEach {
            workManager.cancelUniqueWork(it.id.toString())
            val downloadEntity = downloadDao.find(it.id)
            val path = downloadEntity?.path
            val fileName = downloadEntity?.fileName
            if (path != null && fileName != null && deleteFile) {
                deleteDownloadFileIfExists(path, fileName)
            }
            removeNotification(context, it.id)
            DownloadEvent.sendEvent(Event.Remove(it))
            downloadDao.remove(it.id)
        }
    }

    fun downloadAsync(downloadRequest: DownloadRequest) = scope.launch {
        download(downloadRequest)
    }

    fun observeDownloadById(id: Int): Flow<DownloadEntity> = downloadDao
        .getEntityByIdFlow(id).filterNotNull().distinctUntilChanged()

    fun observeAllDownloads(): Flow<List<DownloadEntity>> = downloadDao.getAllEntityFlow()

    fun findAsync(id: Int, onResult: (DownloadEntity?) -> Unit) = scope.launch {
        onResult(downloadDao.find(id))
    }

    suspend fun getAllDownloads(): List<DownloadEntity> = downloadDao.getAllEntity()
}
