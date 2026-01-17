package com.skyd.downloader

import com.skyd.downloader.db.DownloadDao
import com.skyd.downloader.db.DownloadEntity
import com.skyd.downloader.download.DownloadEvent
import com.skyd.downloader.download.DownloadManager
import com.skyd.downloader.download.DownloadRequest
import com.skyd.downloader.util.FileUtil
import com.skyd.fundation.di.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class Downloader private constructor() {
    private val downloadManager = DownloadManager()

    /**
     * Download the content
     *
     * @param url Download url of the content
     * @param path Download path to store the downloaded file
     * @param fileName Name of the file to be downloaded
     * @return Unique Download ID associated with current download
     */
    fun download(
        url: String,
        path: String,
        fileName: String = FileUtil.getFileNameFromUrl(url),
    ): Int {
        require(url.isNotEmpty() && path.isNotEmpty() && fileName.isNotEmpty()) {
            "Missing ${if (url.isEmpty()) "url" else if (path.isEmpty()) "path" else "fileName"}"
        }
        val downloadRequest = DownloadRequest(
            url = url,
            path = path,
            fileName = fileName,
        )
        downloadManager.downloadAsync(downloadRequest)
        return downloadRequest.id
    }

    /**
     * Observe all downloads
     *
     * @return [kotlinx.coroutines.flow.Flow] of List of [com.skyd.downloader.db.DownloadEntity]
     */
    fun observeDownloads(): Flow<List<DownloadEntity>> {
        return downloadManager.observeAllDownloads()
    }

    /**
     * Observe download with given [id]
     *
     * @param id Unique Download ID of the download
     * @return [kotlinx.coroutines.flow.Flow] of List of [com.skyd.downloader.db.DownloadEntity]
     */
    fun observeDownloadById(id: Int): Flow<DownloadEntity> {
        return downloadManager.observeDownloadById(id)
    }

    /**
     * Pause download with given [id]
     *
     * @param id Unique Download ID of the download
     */
    fun pause(id: Int) {
        downloadManager.pauseAsync(id)
    }

    /**
     * Pause all the downloads
     *
     */
    fun pauseAll() {
        downloadManager.pauseAllAsync()
    }

    /**
     * Resume download with given [id]
     *
     * @param id Unique Download ID of the download
     */
    fun resume(id: Int) {
        downloadManager.resumeAsync(id)
    }

    /**
     * Resume all the downloads
     *
     */
    fun resumeAll() {
        downloadManager.resumeAllAsync()
    }

    /**
     * Retry download with given [id]
     *
     * @param id Unique Download ID of the download
     */
    fun retry(id: Int) {
        downloadManager.retryAsync(id)
    }

    /**
     * Retry all the downloads
     *
     */
    fun retryAll() {
        downloadManager.retryAllAsync()
    }

    /**
     * Clear all entries from database and delete all the files
     *
     * @param deleteFile delete the actual file from the system
     */
    fun clearAllDb(deleteFile: Boolean = true) {
        downloadManager.clearAllDbAsync(deleteFile)
    }

    /**
     * Clear entries from database and delete files on or before [timeInMillis]
     *
     * @param timeInMillis timestamp in millisecond
     * @param deleteFile delete the actual file from the system
     */
    fun clearDb(timeInMillis: Long, deleteFile: Boolean = true) {
        downloadManager.clearDbAsync(timeInMillis, deleteFile)
    }

    /**
     * Clear entry from database and delete file with given [id]
     *
     * @param id Unique Download ID of the download
     * @param deleteFile delete the actual file from the system
     */
    fun clearDb(id: Int, deleteFile: Boolean = true) {
        downloadManager.clearDbAsync(id, deleteFile)
    }

    fun find(id: Int, onResult: (DownloadEntity?) -> Unit) = downloadManager.findAsync(id, onResult)

    /**
     * Suspend function to get list of all Downloads
     *
     * @return List of [com.skyd.downloader.db.DownloadEntity]
     */
    suspend fun getAllDownloads() = downloadManager.getAllDownloads()

    companion object {
        fun observeEvent() = DownloadEvent.eventFlow

        private val scope = CoroutineScope(Dispatchers.IO)

        init {
            // todo needs to refactor to better implementation
            scope.launch {
                val downloadDao = get<DownloadDao>()
                val downloadEntities = downloadDao.findAllInStatuses(
                    listOf(Status.Started, Status.Downloading).map { it.toString() }
                )
                downloadEntities.forEach { entity ->
                    downloadDao.update(entity.copy(status = Status.Paused.toString()))
                }
            }
        }

        val instance: Downloader by lazy { Downloader() }
    }
}