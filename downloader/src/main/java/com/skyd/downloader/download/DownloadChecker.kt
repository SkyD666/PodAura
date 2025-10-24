package com.skyd.downloader.download

import co.touchlab.kermit.Logger
import com.skyd.downloader.Status
import com.skyd.downloader.UserAction
import com.skyd.downloader.db.DownloadDao
import com.skyd.downloader.db.DownloadEntity
import com.skyd.downloader.util.FileUtil
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.request.head
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DownloadChecker : KoinComponent {
    companion object {
        const val TAG = "DownloadChecker"
        private val scope = CoroutineScope(Dispatchers.IO)
    }

    private val log = Logger.withTag(TAG)
    private val downloadDao: DownloadDao by inject()
    private val httpClientConfig: HttpClientConfig<*>.() -> Unit by inject()
    private val httpClient by lazy { HttpClient(httpClientConfig) }

    suspend fun tryDownload(
        id: Int,
        onStart: suspend (DownloadEntity) -> Unit,
        onProgress: suspend (downloadedBytes: Long, totalBytes: Long, speedInBPerMs: Float) -> Unit,
        onPaused: suspend (downloadedBytes: Long, totalBytes: Long) -> Unit,
        onCanceled: suspend () -> Unit,
        onSuccess: suspend (totalBytes: Long) -> Unit,
        onFailed: suspend (downloadedBytes: Long) -> Unit,
    ): Boolean {
        val downloadRequest = downloadDao.find(id)
        if (downloadRequest == null) {
            log.w("No DownloadEntity found for id: $id")
            return false
        }
        val id = downloadRequest.id
        val url = downloadRequest.url
        val dirPath = downloadRequest.path
        val fileName = downloadRequest.fileName

        return try {
            onStart(downloadRequest)
            val latestETag = httpClient.head(url).headers[HttpHeaders.ETag].orEmpty()
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
            ).download(
                onStart = { length ->
                    downloadDao.find(id)?.copy(
                        totalBytes = length,
                        status = Status.Started.toString(),
                    )?.let { downloadDao.update(it) }
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

                    onProgress(downloadedBytes, length, speed)
                }
            )

            downloadDao.find(id)?.copy(
                totalBytes = totalLength,
                status = Status.Success.toString(),
            )?.let { downloadDao.update(it) }

            onSuccess(totalLength)

            downloadDao.find(id)?.let { DownloadEvent.sendEvent(Event.Success(it)) }
            true
        } catch (e: Exception) {
            scope.launch {
                if (e is CancellationException) {
                    var downloadEntity = downloadDao.find(id)
                    if (downloadEntity?.userAction == UserAction.Pause.toString()) {
                        downloadEntity = downloadEntity.copy(status = Status.Paused.toString())
                        downloadDao.update(downloadEntity)
                        onPaused(
                            downloadEntity.downloadedBytes,
                            downloadEntity.totalBytes,
                        )
                    } else {
                        downloadDao.remove(id)
                        FileUtil.deleteDownloadFileIfExists(dirPath, fileName)
                        onCanceled()
                    }
                } else {
                    log.e(throwable = e) { "Download failed for id: $id, url: $url" }
                    e.printStackTrace()
                    var downloadEntity = downloadDao.find(id)
                    if (downloadEntity != null) {
                        downloadEntity = downloadEntity.copy(
                            status = Status.Failed.toString(),
                            failureReason = e.message.orEmpty(),
                        )
                        downloadDao.update(downloadEntity)
                        onFailed(downloadEntity.downloadedBytes)
                    }
                }
            }
            false
        }
    }
}