package com.skyd.downloader

import com.skyd.downloader.db.DownloadDatabase
import com.skyd.downloader.db.DownloadEntity
import com.skyd.downloader.download.DownloadConstraints
import com.skyd.downloader.download.DownloadManager
import com.skyd.downloader.download.cleanCompletedDownloadFiles
import com.skyd.downloader.download.downloadTaskFiles
import com.skyd.downloader.util.FileUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.uuid.Uuid

class Downloader internal constructor(
    database: DownloadDatabase,
    private val downloadManager: DownloadManager,
) {
    private val downloadDao = database.downloadDao()
    private val commandMutex = Mutex()
    private val batches =
        DownloadBatchQueue(database = database, schedule = downloadManager::schedule)

    suspend fun download(
        url: String,
        path: String,
        fileName: String? = null,
        metadata: String? = null,
        constraints: DownloadConstraints = DownloadConstraints(),
    ): String = commandMutex.withLock {
        val target = DownloadTarget.resolve(url = url, path = path, fileName = fileName)
        val existing = target.findSource(downloadDao) ?: run {
            val destinationOwner = downloadDao.findByDestination(path, target.fileName)
            check(destinationOwner == null || destinationOwner.url == url) {
                "Another download already owns the target file"
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
            requireFileResponse = false,
        )?.also { downloadDao.update(it) }
            ?: target.queuedEntity(
                now = now,
                attemptId = attemptId,
                metadata = metadata,
                constraints = constraints,
            ).also { downloadDao.insert(it) }
        downloadManager.schedule(entity)
        entity.id
    }

    /** Each submission ID can be committed once. Follow-up submissions share the batch quota. */
    suspend fun enqueueBatch(
        batchId: String,
        submissionId: String,
        items: List<DownloadBatchItem>,
        failedKeys: Set<String> = emptySet(),
    ): DownloadBatchReceipt = commandMutex.withLock {
        batches.enqueue(
            batchId = batchId,
            submissionId = submissionId,
            items = items,
            failedKeys = failedKeys
        )
    }

    suspend fun getBatchReceipt(batchId: String, submissionId: String): DownloadBatchReceipt? =
        batches.receipt(batchId = batchId, submissionId = submissionId)

    fun observeDownloads(): Flow<List<DownloadEntity>> = downloadDao.getAllEntityFlow()

    fun observePendingCompletions(): Flow<List<DownloadEntity>> =
        downloadDao.observePendingCompletions()

    suspend fun getPendingCompletions(): List<DownloadEntity> =
        downloadDao.getPendingCompletions()

    suspend fun find(id: String): DownloadEntity? = downloadDao.find(id)

    suspend fun getAllDownloads(): List<DownloadEntity> = downloadDao.getAllEntity()

    suspend fun pause(id: String) = commandMutex.withLock {
        val entity = downloadDao.find(id) ?: return@withLock
        if (downloadDao.pauseActive(
                id = id,
                attemptId = entity.attemptId,
                updatedTime = now()
            ) > 0
        ) {
            downloadManager.cancel(id)
        }
    }

    suspend fun resume(id: String) = restart(id)

    suspend fun retry(id: String) = restart(id)

    suspend fun cancel(id: String) = commandMutex.withLock {
        if (downloadDao.find(id) == null) return@withLock
        downloadDao.markCancelled(id = id, updatedTime = now())
        downloadManager.cancel(id)
        val entity = downloadDao.find(id) ?: return@withLock
        downloadTaskFiles(entity = entity, dao = downloadDao).discardCancelled()
    }

    suspend fun delete(id: String, deleteFile: Boolean) = commandMutex.withLock {
        if (downloadDao.find(id) == null) return@withLock
        downloadManager.cancel(id)
        val entity = downloadDao.find(id) ?: return@withLock
        downloadTaskFiles(entity = entity, dao = downloadDao).delete(deleteFile)
        downloadDao.remove(id)
    }

    suspend fun markCompletionHandled(id: String) {
        downloadDao.markCompletionHandled(id, now())
    }

    suspend fun initialize() = commandMutex.withLock {
        downloadManager.reconcile()
        cleanCompletedDownloadFiles(downloadDao)
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
