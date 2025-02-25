package com.skyd.anivu.model.repository.download

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import com.skyd.anivu.R
import com.skyd.anivu.appContext
import com.skyd.anivu.model.bean.download.DownloadInfoBean
import com.skyd.anivu.model.db.dao.ArticleDao
import com.skyd.anivu.model.db.dao.EnclosureDao
import com.skyd.anivu.model.repository.MediaRepository
import com.skyd.downloader.Downloader
import com.skyd.downloader.NotificationConfig
import com.skyd.downloader.Status
import com.skyd.downloader.db.DownloadEntity
import com.skyd.downloader.download.Event
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File

class DownloadManager private constructor(context: Context) {
    private val downloader = Downloader.init(
        context.applicationContext as Application,
        NotificationConfig(
            channelName = context.getString(R.string.download_channel_name),
            channelDescription = context.getString(R.string.download_channel_description),
            smallIcon = R.drawable.ic_icon_24,
            importance = NotificationManager.IMPORTANCE_LOW,
            pauseText = R.string.download_pause,
            resumeText = R.string.download_resume,
            cancelText = R.string.download_cancel,
            retryText = R.string.download_retry,
        )
    )
    val downloadInfoListFlow: Flow<List<DownloadInfoBean>> = downloader.observeDownloads()
        .map { list -> list.map { it.toDownloadInfoBean() } }

    fun download(
        url: String,
        path: String,
        fileName: String? = null,
    ): Any {
        return if (fileName == null) {
            downloader.download(url = url, path = path)
        } else {
            downloader.download(url = url, fileName = fileName, path = path)
        }
    }

    fun pause(id: Int) = downloader.pause(id)
    fun resume(id: Int) = downloader.resume(id)
    fun retry(id: Int) = downloader.retry(id)
    fun delete(id: Int) {
        downloader.find(id) { entity ->
            downloader.clearDb(
                id,
                deleteFile = entity != null && Status.valueOf(entity.status) != Status.Success,
            )
        }
    }

    private fun DownloadEntity.toDownloadInfoBean() = DownloadInfoBean(
        id = id,
        url = url,
        path = path,
        fileName = fileName,
        status = Status.valueOf(status),
        totalBytes = totalBytes,
        downloadedBytes = downloadedBytes,
        speedInBytePerMs = speedInBytePerMs,
        createTime = createTime,
        failureReason = failureReason,
    )

    companion object {
        private val scope = CoroutineScope(Dispatchers.IO)

        @EntryPoint
        @InstallIn(SingletonComponent::class)
        interface WorkerEntryPoint {
            val enclosureDao: EnclosureDao
            val articleDao: ArticleDao
            val mediaRepository: MediaRepository
        }

        private val hiltEntryPoint = EntryPointAccessors.fromApplication(
            appContext, WorkerEntryPoint::class.java
        )

        fun listenDownloadEvent() = scope.launch {
            Downloader.observeEvent().collect { event ->
                when (event) {
                    is Event.Remove -> Unit
                    is Event.Success -> with(hiltEntryPoint) {
                        val articleId = enclosureDao.getMediaArticleId(event.entity.url)
                        if (articleId != null) {
                            val article = articleDao.getArticleWithFeed(articleId).first()
                            mediaRepository.addNewFile(
                                file = File(event.entity.path, event.entity.fileName),
                                groupName = null,
                                articleId = articleId,
                                displayName = article?.articleWithEnclosure?.article?.title
                            ).collect()
                        }
                    }
                }
            }
        }

        @Volatile
        private var instance: DownloadManager? = null

        fun getInstance(context: Context): DownloadManager {
            if (instance == null) {
                synchronized(DownloadManager) {
                    if (instance == null) {
                        instance = DownloadManager(context)
                    }
                }
            }
            return instance!!
        }
    }
}