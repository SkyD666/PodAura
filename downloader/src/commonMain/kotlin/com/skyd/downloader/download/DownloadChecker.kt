package com.skyd.downloader.download

import co.touchlab.kermit.Logger
import com.skyd.downloader.Status
import com.skyd.downloader.UserAction
import com.skyd.downloader.db.DownloadDao
import com.skyd.downloader.util.FileUtil
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.request.head
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.concurrent.atomics.AtomicBoolean

class DownloadChecker : KoinComponent {
    companion object {
        const val TAG = "DownloadChecker"
        private val scope = CoroutineScope(Dispatchers.IO)
    }

    private val log = Logger.withTag(TAG)
    private val downloadDao: DownloadDao by inject()
    private val httpClientConfig: HttpClientConfig<*>.() -> Unit by inject()
    private val httpClient by lazy { HttpClient(httpClientConfig) }
    private val _isDownloading = AtomicBoolean(false)
    val isDownloading: Boolean = _isDownloading.load()

    suspend fun tryDownload(id: Int): Boolean {
        _isDownloading.store(true)
        val downloadEntity = downloadDao.find(id)
        if (downloadEntity == null) {
            log.w("No DownloadEntity found for id: $id")
            _isDownloading.store(false)
            return false
        }
        val id = downloadEntity.id
        val url = downloadEntity.url
        val dirPath = downloadEntity.path
        val fileName = downloadEntity.fileName

        return try {
            DownloadEvent.sendEvent(Event.Start(downloadEntity))
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
                        )?.let {
                            downloadDao.update(it)
                            DownloadEvent.sendEvent(Event.Progress(it))
                        }
                    }
                }
            )

            downloadDao.find(id)?.copy(
                totalBytes = totalLength,
                status = Status.Success.toString(),
            )?.let {
                downloadDao.update(it)
                DownloadEvent.sendEvent(Event.Success(it))
            }

            downloadDao.find(id)?.let { DownloadEvent.sendEvent(Event.Success(it)) }
            _isDownloading.store(false)
            true
        } catch (e: Exception) {
            scope.launch {
                if (e is CancellationException) {
                    var downloadEntity = downloadDao.find(id)
                    if (downloadEntity?.userAction == UserAction.Pause.toString()) {
                        downloadEntity = downloadEntity.copy(status = Status.Paused.toString())
                        downloadDao.update(downloadEntity)
                        DownloadEvent.sendEvent(Event.Paused(downloadEntity))
                    } else {
                        downloadDao.remove(id)
                        FileUtil.deleteDownloadFileIfExists(dirPath, fileName)
                        downloadEntity?.let { DownloadEvent.sendEvent(Event.Remove(it)) }
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
                        DownloadEvent.sendEvent(Event.Failed(downloadEntity))
                    }
                }
                _isDownloading.store(false)
            }
            false
        }
    }
}