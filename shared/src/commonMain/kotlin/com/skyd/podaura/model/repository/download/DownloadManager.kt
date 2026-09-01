package com.skyd.podaura.model.repository.download

import co.touchlab.kermit.Logger
import com.skyd.downloader.Downloader
import com.skyd.downloader.Status
import com.skyd.downloader.db.DownloadEntity
import com.skyd.downloader.download.DownloadConstraints
import com.skyd.fundation.di.get
import com.skyd.fundation.di.inject
import com.skyd.podaura.model.db.dao.ArticleDao
import com.skyd.podaura.model.db.dao.EnclosureDao
import com.skyd.podaura.model.download.ArticleDownloadSource
import com.skyd.podaura.model.download.DownloadInfoBean
import com.skyd.podaura.model.download.decodeArticleDownloadSource
import com.skyd.podaura.model.download.encode
import com.skyd.podaura.model.repository.media.MediaRepository
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class DownloadManager private constructor() : IDownloadManager, KoinComponent {
    private val downloader: Downloader by inject()
    private val log = Logger.withTag("DownloadManager")

    val downloadInfoListFlow: Flow<List<DownloadInfoBean>> = downloader.observeDownloads()
        .map { list -> list.map { it.toDownloadInfoBean() } }

    override suspend fun download(
        url: String,
        path: String,
        fileName: String?,
        articleDownloadSource: ArticleDownloadSource?,
        constraints: DownloadConstraints,
    ): String = downloader.download(
        url = url,
        path = path,
        fileName = fileName,
        metadata = articleDownloadSource?.encode(),
        constraints = constraints,
    )

    override suspend fun getAllDownloadTasks(): List<DownloadInfoBean> =
        downloader.getAllDownloads().map { it.toDownloadInfoBean() }

    suspend fun pause(id: String) = downloader.pause(id)
    suspend fun resume(id: String) = downloader.resume(id)
    suspend fun retry(id: String) = downloader.retry(id)
    suspend fun cancel(id: String) = downloader.cancel(id)

    suspend fun delete(id: String, deleteCompletedFile: Boolean = false) {
        val entity = downloader.find(id) ?: return
        downloader.delete(
            id = id,
            deleteFile = entity.status != Status.Success.name || deleteCompletedFile,
        )
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
        articleDownloadSource = metadata.decodeArticleDownloadSource(),
    )

    private suspend fun handleCompletion(entity: DownloadEntity) {
        val source = entity.metadata.decodeArticleDownloadSource()
        val articleId = source?.articleId
            ?: get<EnclosureDao>().getMediaArticleId(entity.url)
        if (articleId != null) {
            val article = get<ArticleDao>().getArticleWithFeed(articleId).first()
            val parent = PlatformFile(entity.path)
            get<MediaRepository>().addNewFile(
                file = PlatformFile(parent, entity.fileName),
                parent = parent,
                groupName = null,
                articleId = articleId,
                displayName = article?.articleWithEnclosure?.article?.title,
            ).collect()
        }
        downloader.markCompletionHandled(entity.id)
    }

    private suspend fun handlePendingCompletions(initial: List<DownloadEntity>) {
        var pending = initial
        var retryDelayMillis = INITIAL_COMPLETION_RETRY_DELAY_MILLIS
        while (pending.isNotEmpty()) {
            var hasFailure = false
            pending.forEach { entity ->
                try {
                    handleCompletion(entity)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    hasFailure = true
                    log.e(throwable = error) {
                        "Failed to handle completed download ${entity.id}; retrying"
                    }
                }
            }
            if (!hasFailure) return

            delay(retryDelayMillis)
            retryDelayMillis = minOf(
                retryDelayMillis * 2,
                MAX_COMPLETION_RETRY_DELAY_MILLIS,
            )
            pending = downloader.getPendingCompletions()
        }
    }

    companion object {
        private const val INITIAL_COMPLETION_RETRY_DELAY_MILLIS = 1_000L
        private const val MAX_COMPLETION_RETRY_DELAY_MILLIS = 60_000L
        private val scope = CoroutineScope(Dispatchers.IO)

        fun start() = scope.launch {
            val manager = instance
            manager.downloader.initialize()
            manager.downloader.observePendingCompletions().collect { pending ->
                manager.handlePendingCompletions(pending)
            }
        }

        val instance by lazy { DownloadManager() }
    }
}
