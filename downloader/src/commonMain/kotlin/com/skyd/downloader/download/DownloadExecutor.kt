package com.skyd.downloader.download

import co.touchlab.kermit.Logger
import com.skyd.downloader.Status
import com.skyd.downloader.db.DownloadDao
import com.skyd.downloader.util.FileUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.time.Clock

internal class DownloadExecutor(
    private val downloadDao: DownloadDao,
    private val transferEngine: DownloadTransferEngine,
) {
    private val log = Logger.withTag("DownloadExecutor")

    suspend fun execute(
        id: String,
        attemptId: String,
        onProgress: suspend (TransferProgress) -> Unit = {},
    ): DownloadExecutionResult = executionSlots.withPermit {
        val initial = downloadDao.find(id)
            ?.takeIf { it.attemptId == attemptId }
            ?: return@withPermit DownloadExecutionResult.Ignored
        if (downloadDao.markStarted(id, attemptId, now()) == 0) {
            return@withPermit DownloadExecutionResult.Ignored
        }

        try {
            val result = transferEngine.transfer(
                entity = initial,
                onResponse = { response ->
                    val current = downloadDao.find(id)
                        ?.takeIf { it.attemptId == attemptId }
                        ?: throw CancellationException("Download attempt is no longer current")
                    val resolvedName = if (current.fileNameResolved) {
                        current.fileName
                    } else {
                        FileUtil.sanitizeFileName(
                            response.suggestedFileName
                                ?: FileUtil.getFileNameFromUrl(response.finalUrl)
                        )
                    }
                    FileUtil.validateTarget(current.path, resolvedName)
                    if (
                        downloadDao.claimResponseMetadata(
                            id = id,
                            attemptId = attemptId,
                            path = current.path,
                            fileName = resolvedName,
                            eTag = response.eTag,
                            lastModified = response.lastModified,
                            finalUrl = response.finalUrl,
                            totalBytes = response.totalBytes,
                            updatedTime = now(),
                        ) == 0
                    ) {
                        val latest = downloadDao.find(id)
                        if (latest?.attemptId == attemptId &&
                            (latest.status == Status.Started.name ||
                                    latest.status == Status.Downloading.name)
                        ) {
                            throw DownloadFailure(
                                DownloadFailureCode.DestinationConflict,
                                "Another download already owns the target file",
                                retryable = false,
                            )
                        }
                        throw CancellationException("Download attempt is no longer current")
                    }
                    resolvedName
                },
                onProgress = { progress ->
                    if (
                        downloadDao.updateProgress(
                            id = id,
                            attemptId = attemptId,
                            downloadedBytes = progress.downloadedBytes,
                            totalBytes = progress.totalBytes,
                            speedInBytePerMs = progress.speedInBytePerMs,
                            updatedTime = now(),
                        ) == 0
                    ) {
                        throw CancellationException("Download attempt is no longer current")
                    }
                    onProgress(progress)
                },
            )
            if (downloadDao.markSuccess(id, attemptId, result.totalBytes, now()) == 0) {
                return@withPermit DownloadExecutionResult.Ignored
            }
            DownloadExecutionResult.Success(result.totalBytes)
        } catch (error: CancellationException) {
            withContext(NonCancellable) {
                downloadDao.markInterruptedQueued(id, attemptId, now())
            }
            throw error
        } catch (failure: DownloadFailure) {
            handleFailure(initial, attemptId, failure)
        } catch (error: Throwable) {
            log.e { "Unexpected download failure for task $id" }
            handleFailure(
                entity = initial,
                attemptId = attemptId,
                failure = DownloadFailure(
                    code = DownloadFailureCode.Unknown,
                    message = "Unexpected download failure",
                    retryable = false,
                    cause = error,
                )
            )
        }
    }

    private suspend fun handleFailure(
        entity: com.skyd.downloader.db.DownloadEntity,
        attemptId: String,
        failure: DownloadFailure,
    ): DownloadExecutionResult {
        val current = downloadDao.find(entity.id)
            ?.takeIf { it.attemptId == attemptId }
            ?: return DownloadExecutionResult.Ignored
        if (failure.retryable && current.autoRetryCount < MAX_AUTO_RETRIES) {
            val retryCount = current.autoRetryCount + 1
            val delay = max(
                RETRY_DELAYS_MILLIS[(retryCount - 1).coerceAtMost(RETRY_DELAYS_MILLIS.lastIndex)],
                failure.retryAfterMillis ?: 0,
            )
            val updated = downloadDao.markRetryQueued(
                id = current.id,
                attemptId = attemptId,
                retryCount = retryCount,
                nextAttemptAt = now() + delay,
                reason = failure.message,
                code = failure.code.name,
                updatedTime = now(),
            )
            return if (updated > 0) {
                DownloadExecutionResult.Retry(delay)
            } else {
                DownloadExecutionResult.Ignored
            }
        }
        val updated = downloadDao.markFailed(
            id = current.id,
            attemptId = attemptId,
            reason = failure.message,
            code = failure.code.name,
            updatedTime = now(),
        )
        return if (updated > 0) {
            DownloadExecutionResult.Failed
        } else {
            DownloadExecutionResult.Ignored
        }
    }

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()

    companion object {
        private const val MAX_AUTO_RETRIES = 5
        private val RETRY_DELAYS_MILLIS = longArrayOf(
            30_000,
            120_000,
            600_000,
            1_800_000,
            1_800_000,
        )
        private val executionSlots = Semaphore(permits = 3)
    }
}
