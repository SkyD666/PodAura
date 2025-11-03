package com.skyd.podaura.model.repository.download

import com.skyd.downloader.Downloader
import com.skyd.downloader.Status
import com.skyd.downloader.db.DownloadEntity
import com.skyd.downloader.download.Event
import com.skyd.fundation.di.get
import com.skyd.fundation.di.inject
import com.skyd.podaura.model.db.dao.ArticleDao
import com.skyd.podaura.model.db.dao.EnclosureDao
import com.skyd.podaura.model.download.DownloadInfoBean
import com.skyd.podaura.model.repository.media.MediaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import org.koin.core.component.KoinComponent

class DownloadManager private constructor() : IDownloadManager, KoinComponent {
    private val downloader: Downloader by inject()
    val downloadInfoListFlow: Flow<List<DownloadInfoBean>> = downloader.observeDownloads()
        .map { list -> list.map { it.toDownloadInfoBean() } }

    override fun download(
        url: String,
        path: String,
        fileName: String?,
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

                    is Event.Remove,
                    is Event.Failed,
                    is Event.Paused,
                    is Event.Progress,
                    is Event.Start -> Unit
                }
            }
        }

        val instance by lazy { DownloadManager() }
    }
}