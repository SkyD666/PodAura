package com.skyd.podaura.model.repository.download

import com.skyd.downloader.Status
import com.skyd.podaura.model.bean.article.ArticleWithFeed
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.db.dao.ArticleDao
import com.skyd.podaura.model.db.dao.FeedDao
import com.skyd.podaura.model.download.ArticleDownloadInfoBean
import com.skyd.podaura.model.download.ArticleDownloadSource
import com.skyd.podaura.model.download.DownloadInfoBean
import com.skyd.podaura.model.repository.BaseRepository
import com.skyd.podaura.util.media.detectLocalMediaKind
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.mimeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest

class DownloadRepository(
    private val articleDao: ArticleDao,
    private val feedDao: FeedDao,
) : BaseRepository() {
    fun requestDownloadTasksList(): Flow<List<DownloadInfoBean>> {
        val downloadTasks = DownloadManager.instance.downloadInfoListFlow
        val articleDownloadSnapshots = downloadTasks
            .map { it.articleDownloadSources() }
            .distinctUntilChanged()
            .flatMapLatest { sources ->
                if (sources.isEmpty()) {
                    flowOf(
                        ArticleDownloadSnapshot(sources = sources, infoByDownloadId = emptyMap())
                    )
                } else {
                    combine(
                        articleDao.observeArticleWithFeedListByIds(
                            sources.map { it.source.articleId }.distinct()
                        ),
                        feedDao.observeFeeds(sources.map { it.source.feedUrl }.distinct()),
                    ) { articles, feeds ->
                        ArticleDownloadSnapshot(
                            sources = sources,
                            infoByDownloadId = resolveArticleDownloadInfo(
                                sources = sources,
                                articles = articles,
                                feeds = feeds,
                            ),
                        )
                    }
                }
            }

        val playableMediaSnapshots = downloadTasks
            .map { it.completedDownloadFiles() }
            .distinctUntilChanged()
            .mapLatest { files ->
                PlayableMediaSnapshot(
                    files = files,
                    playableDownloadIds = resolvePlayableDownloadIds(files) { file ->
                        PlatformFile(PlatformFile(file.path), file.fileName).mimeType()?.toString()
                    },
                )
            }
            .flowOn(Dispatchers.IO)

        return combine(
            downloadTasks,
            articleDownloadSnapshots,
            playableMediaSnapshots,
        ) { tasks, articleSnapshot, playableMediaSnapshot ->
            if (tasks.articleDownloadSources() != articleSnapshot.sources ||
                tasks.completedDownloadFiles() != playableMediaSnapshot.files
            ) {
                null
            } else {
                tasks.map { task ->
                    task.copy(
                        articleDownloadInfo = articleSnapshot.infoByDownloadId[task.id],
                        isPlayableMedia = task.id in playableMediaSnapshot.playableDownloadIds,
                    )
                }
            }
        }.filterNotNull()
    }
}

internal data class ArticleDownloadSourceEntry(
    val downloadId: String,
    val source: ArticleDownloadSource,
)

private data class ArticleDownloadSnapshot(
    val sources: List<ArticleDownloadSourceEntry>,
    val infoByDownloadId: Map<String, ArticleDownloadInfoBean>,
)

internal data class CompletedDownloadFileEntry(
    val downloadId: String,
    val path: String,
    val fileName: String,
)

private data class PlayableMediaSnapshot(
    val files: List<CompletedDownloadFileEntry>,
    val playableDownloadIds: Set<String>,
)

internal fun List<DownloadInfoBean>.articleDownloadSources(): List<ArticleDownloadSourceEntry> =
    mapNotNull { task ->
        task.articleDownloadSource?.let { source ->
            ArticleDownloadSourceEntry(downloadId = task.id, source = source)
        }
    }

internal fun List<DownloadInfoBean>.completedDownloadFiles(): List<CompletedDownloadFileEntry> =
    mapNotNull { task ->
        task.takeIf { it.status == Status.Success }?.let {
            CompletedDownloadFileEntry(
                downloadId = it.id,
                path = it.path,
                fileName = it.fileName,
            )
        }
    }

internal fun resolvePlayableDownloadIds(
    files: List<CompletedDownloadFileEntry>,
    mimeTypeOf: (CompletedDownloadFileEntry) -> String?,
): Set<String> = buildSet {
    files.forEach { file ->
        val mimeType = runCatching { mimeTypeOf(file) }.getOrNull()
        if (detectLocalMediaKind(file.fileName, mimeType).isPlayable) {
            add(file.downloadId)
        }
    }
}

internal fun resolveArticleDownloadInfo(
    sources: List<ArticleDownloadSourceEntry>,
    articles: List<ArticleWithFeed>,
    feeds: List<FeedBean>,
): Map<String, ArticleDownloadInfoBean> {
    val articleById = articles.associateBy {
        it.articleWithEnclosure.article.articleId
    }
    val feedByUrl = feeds.associateBy { it.url }

    return buildMap {
        sources.forEach { entry ->
            val feed = feedByUrl[entry.source.feedUrl] ?: return@forEach
            val article = articleById[entry.source.articleId]
            put(
                entry.downloadId,
                ArticleDownloadInfoBean(
                    articleTitle = article?.articleWithEnclosure?.article?.title,
                    episodeImage = article?.articleWithEnclosure?.media?.image,
                    articleImage = article?.articleWithEnclosure?.article?.image,
                    feed = feed,
                )
            )
        }
    }
}
