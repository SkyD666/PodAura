package com.skyd.downloader.download

import com.skyd.downloader.Status
import com.skyd.downloader.UserAction
import com.skyd.downloader.db.DownloadEntity
import com.skyd.downloader.util.FileUtil.deleteDownloadFileIfExists
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

actual class DownloadManager : BaseDownloadManager() {
    companion object {
        private val mutex = Mutex()
        private val downloadQueue = mutableMapOf<Int, Pair<DownloadChecker, Job>>()
        private val scope = CoroutineScope(Dispatchers.IO)
    }

    actual override suspend fun download(downloadRequest: DownloadRequest) {
        var oldDownloadEntity = downloadDao.find(downloadRequest.id)
        // Checks if download id already present in database
        if (oldDownloadEntity != null) {
            oldDownloadEntity = oldDownloadEntity.copy(userAction = UserAction.Start.toString())
            downloadDao.update(oldDownloadEntity)

            // In case new download request is generated for already existing id in database
            // and work is not in progress, replace the uuid in database
            if (oldDownloadEntity.status != Status.Queued.toString() &&
                oldDownloadEntity.status != Status.Downloading.toString() &&
                oldDownloadEntity.status != Status.Started.toString()
            ) {
                downloadDao.update(oldDownloadEntity.copy(status = Status.Queued.toString()))
            }
        } else {
            downloadDao.insert(
                DownloadEntity(
                    url = downloadRequest.url,
                    path = downloadRequest.path,
                    fileName = downloadRequest.fileName,
                    id = downloadRequest.id,
                    timeQueued = Clock.System.now().toEpochMilliseconds(),
                    status = Status.Queued.toString(),
                    userAction = UserAction.Start.toString(),
                )
            )

            deleteDownloadFileIfExists(downloadRequest.path, downloadRequest.fileName)
        }

        mutex.withLock {
            val oldDownload = downloadQueue[downloadRequest.id]
            if (oldDownload == null) {
                val downloadChecker = DownloadChecker()
                log.i("Starting new download for id: ${downloadRequest.id}")
                downloadQueue[downloadRequest.id] = downloadChecker to scope.launch {
                    downloadChecker.tryDownload(id = downloadRequest.id)
                }
            } else {
                val (oldDownloadChecker, job) = oldDownload
                if (!oldDownloadChecker.isDownloading || !job.isActive) {
                    log.i("Resuming download for id: ${downloadRequest.id}")
                    downloadQueue[downloadRequest.id] = oldDownloadChecker to scope.launch {
                        oldDownloadChecker.tryDownload(id = downloadRequest.id)
                    }
                } else {
                    log.i("Download already in progress for id: ${downloadRequest.id}")
                }
            }
        }
    }

    actual override suspend fun onPause(id: Int) {
        removeFromDownloadQueue(id)
    }

    actual override suspend fun onClearDbAsync(id: Int) {
        removeFromDownloadQueue(id)
    }

    private suspend fun removeFromDownloadQueue(id: Int) {
        mutex.withLock {
            val oldDownload = downloadQueue[id]
            if (oldDownload != null) {
                val (downloadChecker, job) = oldDownload
                if (downloadChecker.isDownloading || job.isActive) {
                    job.cancel()
                }
                downloadQueue.remove(id)
            } else {
                log.w("No download in progress to pause for id: $id")
            }
        }
    }
}
