package com.skyd.downloader

import com.skyd.downloader.db.DownloadDao
import com.skyd.downloader.db.DownloadEntity
import com.skyd.downloader.download.DownloadConstraints
import com.skyd.downloader.download.DownloadManager
import com.skyd.downloader.util.FileUtil
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.uuid.Uuid

class Downloader internal constructor(
    private val downloadDao: DownloadDao,
    private val downloadManager: DownloadManager,
) {
    private val commandMutex = Mutex()

    suspend fun download(
        url: String,
        path: String,
        fileName: String? = null,
        metadata: String? = null,
        constraints: DownloadConstraints = DownloadConstraints(),
    ): String = commandMutex.withLock {
        val parsedUrl = runCatching { Url(url) }.getOrElse {
            throw IllegalArgumentException("Invalid download URL")
        }
        require(parsedUrl.protocol == URLProtocol.HTTP || parsedUrl.protocol == URLProtocol.HTTPS) {
            "Only HTTP and HTTPS downloads are supported"
        }
        val explicitFileName = fileName?.let(FileUtil::sanitizeFileName)
        val resolvedFileName = explicitFileName ?: FileUtil.getFileNameFromUrl(url)
        val requestedFileName = explicitFileName.orEmpty()
        FileUtil.validateTarget(path, resolvedFileName)

        val sourceTask = downloadDao.findBySourceAndTarget(
            url = url,
            path = path,
            requestedFileName = requestedFileName,
        )
        val existing = if (sourceTask != null) {
            sourceTask
        } else {
            val destinationOwner = downloadDao.findByDestination(path, resolvedFileName)
            if (destinationOwner != null && destinationOwner.url != url) {
                throw IllegalStateException("Another download already owns the target file")
            }
            destinationOwner
        }
        if (existing != null && existing.status.isActiveDownloadStatus()) return@withLock existing.id
        if (existing != null && existing.status == Status.Success.name &&
            FileUtil.finalFileExists(existing.path, existing.fileName)
        ) {
            return@withLock existing.id
        }

        val now = now()
        val attemptId = Uuid.random().toString()
        val entity = existing?.copy(
            status = Status.Queued.name,
            timeQueued = now,
            updatedTime = now,
            attemptId = attemptId,
            autoRetryCount = 0,
            nextAttemptAt = 0,
            speedInBytePerMs = 0f,
            failureReason = "",
            failureCode = "",
            metadata = metadata ?: existing.metadata,
            requireUnmetered = constraints.requireUnmetered,
            requiresCharging = constraints.requiresCharging,
            requiresBatteryNotLow = constraints.requiresBatteryNotLow,
        )?.also { downloadDao.update(it) }
            ?: DownloadEntity(
                id = Uuid.random().toString(),
                url = url,
                path = path,
                fileName = resolvedFileName,
                requestedFileName = requestedFileName,
                fileNameResolved = explicitFileName != null,
                timeQueued = now,
                status = Status.Queued.name,
                attemptId = attemptId,
                createTime = now,
                updatedTime = now,
                metadata = metadata,
                requireUnmetered = constraints.requireUnmetered,
                requiresCharging = constraints.requiresCharging,
                requiresBatteryNotLow = constraints.requiresBatteryNotLow,
            ).also { downloadDao.insert(it) }
        downloadManager.schedule(entity)
        entity.id
    }

    fun observeDownloads(): Flow<List<DownloadEntity>> = downloadDao.getAllEntityFlow()

    fun observePendingCompletions(): Flow<List<DownloadEntity>> =
        downloadDao.observePendingCompletions()

    suspend fun getPendingCompletions(): List<DownloadEntity> =
        downloadDao.getPendingCompletions()

    suspend fun find(id: String): DownloadEntity? = downloadDao.find(id)

    suspend fun getAllDownloads(): List<DownloadEntity> = downloadDao.getAllEntity()

    suspend fun pause(id: String) = commandMutex.withLock {
        val entity = downloadDao.find(id) ?: return@withLock
        if (!entity.status.isActiveDownloadStatus()) return@withLock
        downloadDao.update(
            entity.copy(
                status = Status.Paused.name,
                attemptId = "",
                speedInBytePerMs = 0f,
                updatedTime = now(),
            )
        )
        downloadManager.cancel(id)
    }

    suspend fun resume(id: String) = restart(id)

    suspend fun retry(id: String) = restart(id)

    suspend fun cancel(id: String) = commandMutex.withLock {
        val entity = downloadDao.find(id) ?: return@withLock
        downloadDao.update(
            entity.copy(
                status = Status.Cancelled.name,
                attemptId = "",
                speedInBytePerMs = 0f,
                updatedTime = now(),
            )
        )
        downloadManager.cancel(id)
        FileUtil.deletePartIfExists(entity.path, entity.fileName)
    }

    suspend fun delete(id: String, deleteFile: Boolean) = commandMutex.withLock {
        val entity = downloadDao.find(id) ?: return@withLock
        downloadManager.cancel(id)
        FileUtil.deletePartIfExists(entity.path, entity.fileName)
        if (deleteFile) FileUtil.deleteFinalIfExists(entity.path, entity.fileName)
        downloadDao.remove(id)
    }

    suspend fun markCompletionHandled(id: String) {
        downloadDao.markCompletionHandled(id, now())
    }

    suspend fun initialize() {
        downloadManager.reconcile()
    }

    private suspend fun restart(id: String) = commandMutex.withLock {
        val entity = downloadDao.find(id) ?: return@withLock
        if (entity.status.isActiveDownloadStatus()) return@withLock
        val restarted = entity.copy(
            status = Status.Queued.name,
            attemptId = Uuid.random().toString(),
            autoRetryCount = 0,
            nextAttemptAt = 0,
            speedInBytePerMs = 0f,
            failureReason = "",
            failureCode = "",
            timeQueued = now(),
            updatedTime = now(),
        )
        downloadDao.update(restarted)
        downloadManager.schedule(restarted)
    }

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()
}
