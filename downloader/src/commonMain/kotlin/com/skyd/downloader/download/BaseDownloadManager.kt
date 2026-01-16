package com.skyd.downloader.download

import co.touchlab.kermit.Logger
import com.skyd.downloader.UserAction
import com.skyd.downloader.db.DownloadDao
import com.skyd.downloader.db.DownloadEntity
import com.skyd.downloader.download.DownloadRequest.Companion.toDownloadRequest
import com.skyd.downloader.util.FileUtil.deleteDownloadFileIfExists
import com.skyd.fundation.di.inject
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import kotlin.uuid.Uuid

abstract class BaseDownloadManager : KoinComponent {
    companion object {
        const val TAG = "DownloadManager"
    }

    protected val log = Logger.withTag(TAG)

    protected val downloadDao by inject<DownloadDao>()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        log.i("Exception in DownloadManager Scope: ${throwable.message}")
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + exceptionHandler)

    protected abstract suspend fun download(downloadRequest: DownloadRequest)

    private suspend fun resume(id: Int) {
        val downloadEntity = downloadDao.find(id)
        if (downloadEntity != null) {
            downloadDao.update(downloadEntity.copy(userAction = UserAction.Resume.toString()))
            download(downloadEntity.toDownloadRequest())
        }
    }

    protected suspend fun pause(id: Int) {
        onPause(id)
        val downloadEntity = downloadDao.find(id)
        if (downloadEntity != null) {
            downloadDao.update(downloadEntity.copy(userAction = UserAction.Pause.toString()))
        }
    }

    abstract suspend fun onPause(id: Int)

    private suspend fun retry(id: Int) {
        val downloadEntity = downloadDao.find(id)
        if (downloadEntity != null) {
            downloadDao.update(downloadEntity.copy(userAction = UserAction.Retry.toString()))
            download(downloadEntity.toDownloadRequest())
        }
    }

    private suspend fun findDownloadEntityFromUUID(uuid: Uuid): DownloadEntity? {
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
        onClearDbAsync(id)
        val downloadEntity = downloadDao.find(id)
        val path = downloadEntity?.path
        val fileName = downloadEntity?.fileName
        if (path != null && fileName != null && deleteFile) {
            deleteDownloadFileIfExists(path, fileName)
        }
        downloadDao.remove(id)
    }

    abstract suspend fun onClearDbAsync(id: Int)

    open fun clearAllDbAsync(deleteFile: Boolean) = scope.launch {
        downloadDao.getAllEntity().forEach {
            onClearDbAsyncEach(it.id)
            val downloadEntity = downloadDao.find(it.id)
            val path = downloadEntity?.path
            val fileName = downloadEntity?.fileName
            if (path != null && fileName != null && deleteFile) {
                deleteDownloadFileIfExists(path, fileName)
            }
            DownloadEvent.sendEvent(Event.Remove(it))
        }
        downloadDao.deleteAll()
    }

    fun clearDbAsync(timeInMillis: Long, deleteFile: Boolean) = scope.launch {
        downloadDao.getEntityTillTime(timeInMillis).forEach {
            onClearDbAsyncEach(it.id)
            val downloadEntity = downloadDao.find(it.id)
            val path = downloadEntity?.path
            val fileName = downloadEntity?.fileName
            if (path != null && fileName != null && deleteFile) {
                deleteDownloadFileIfExists(path, fileName)
            }
            DownloadEvent.sendEvent(Event.Remove(it))
            downloadDao.remove(it.id)
        }
    }

    protected open suspend fun onClearDbAsyncEach(id: Int) = Unit

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
