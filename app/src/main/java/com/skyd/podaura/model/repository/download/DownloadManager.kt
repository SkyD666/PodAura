package com.skyd.podaura.model.repository.download

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import com.skyd.downloader.Downloader
import com.skyd.downloader.NotificationConfig
import com.skyd.downloader.Status
import com.skyd.downloader.db.DownloadEntity
import com.skyd.downloader.download.Event
import com.skyd.podaura.R
import com.skyd.podaura.di.get
import com.skyd.podaura.ext.getString
import com.skyd.podaura.model.bean.download.DownloadInfoBean
import com.skyd.podaura.model.db.dao.ArticleDao
import com.skyd.podaura.model.db.dao.EnclosureDao
import com.skyd.podaura.model.repository.media.MediaRepository
import com.skyd.podaura.ui.activity.MainActivity
import com.skyd.podaura.ui.screen.download.DownloadRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.download_cancel
import podaura.shared.generated.resources.download_channel_description
import podaura.shared.generated.resources.download_channel_name
import podaura.shared.generated.resources.download_pause
import podaura.shared.generated.resources.download_resume
import podaura.shared.generated.resources.download_retry

class DownloadManager private constructor(context: Context) {
    private val downloader = Downloader.init(
        context.applicationContext as Application,
        NotificationConfig(
            channelName = context.getString(Res.string.download_channel_name),
            channelDescription = context.getString(Res.string.download_channel_description),
            smallIcon = R.drawable.ic_icon_24,
            importance = NotificationManager.IMPORTANCE_LOW,
            intentContentActivity = MainActivity::class.qualifiedName,
            intentContentBasePath = DownloadRoute.BASE_PATH,
            pauseText = context.getString(Res.string.download_pause),
            resumeText = context.getString(Res.string.download_resume),
            cancelText = context.getString(Res.string.download_cancel),
            retryText = context.getString(Res.string.download_retry),
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

        fun listenDownloadEvent() = scope.launch {
            Downloader.observeEvent().collect { event ->
                when (event) {
                    is Event.Remove -> Unit
                    is Event.Success -> {
                        val articleId = get<EnclosureDao>().getMediaArticleId(event.entity.url)
                        if (articleId != null) {
                            val article = get<ArticleDao>().getArticleWithFeed(articleId).first()
                            get<MediaRepository>().addNewFile(
                                file = Path(event.entity.path, event.entity.fileName),
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