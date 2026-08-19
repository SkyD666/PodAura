package com.skyd.podaura.model.repository.download

import com.skyd.podaura.model.bean.article.ArticleWithFeed
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.db.dao.ArticleDao
import com.skyd.podaura.model.db.dao.FeedDao
import com.skyd.podaura.model.download.ArticleDownloadInfoBean
import com.skyd.podaura.model.download.ArticleDownloadSource
import com.skyd.podaura.model.download.DownloadInfoBean
import com.skyd.podaura.model.repository.BaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

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

        return combine(downloadTasks, articleDownloadSnapshots) { tasks, snapshot ->
            if (tasks.articleDownloadSources() != snapshot.sources) {
                null
            } else {
                tasks.map { task ->
                    task.copy(articleDownloadInfo = snapshot.infoByDownloadId[task.id])
                }
            }
        }.filterNotNull()
    }
}

internal data class ArticleDownloadSourceEntry(
    val downloadId: Int,
    val source: ArticleDownloadSource,
)

private data class ArticleDownloadSnapshot(
    val sources: List<ArticleDownloadSourceEntry>,
    val infoByDownloadId: Map<Int, ArticleDownloadInfoBean>,
)

internal fun List<DownloadInfoBean>.articleDownloadSources(): List<ArticleDownloadSourceEntry> =
    mapNotNull { task ->
        task.articleDownloadSource?.let { source ->
            ArticleDownloadSourceEntry(downloadId = task.id, source = source)
        }
    }

internal fun resolveArticleDownloadInfo(
    sources: List<ArticleDownloadSourceEntry>,
    articles: List<ArticleWithFeed>,
    feeds: List<FeedBean>,
): Map<Int, ArticleDownloadInfoBean> {
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
