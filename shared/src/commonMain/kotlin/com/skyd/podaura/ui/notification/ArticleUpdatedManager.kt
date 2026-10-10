package com.skyd.podaura.ui.notification

import com.skyd.fundation.di.get
import com.skyd.podaura.ext.onSubList
import com.skyd.podaura.model.bean.ArticleNotificationRuleBean
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.db.dao.ArticleDao
import com.skyd.podaura.model.db.dao.ArticleNotificationRuleDao
import com.skyd.podaura.model.db.dao.FeedDao
import com.skyd.podaura.model.db.dao.download.AutoDownloadRuleDao
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first

object ArticleUpdatedManager {
    const val CHANNEL_ID = "articleNotification"

    private val scope = CoroutineScope(Dispatchers.IO)
    private val batcher = ArticleUpdateBatcher(scope) { articleIds ->
        deliverArticleUpdates(articleIds, ::sendNotification, ::autoDownload)
    }

    suspend fun send(articleIds: List<String>) = batcher.send(articleIds)

    suspend fun flush() = batcher.flush()

    internal suspend fun flush(delivery: ArticleUpdateDelivery) = batcher.flush(delivery)

    fun setImmediateDelivery(enabled: Boolean) = batcher.setImmediateDelivery(enabled)

    private suspend fun autoDownload(articleIds: List<String>) {
        val articleDao = get<ArticleDao>()
        val autoDownloadRuleDao = get<AutoDownloadRuleDao>()
        val feedUrlWithArticles = mutableMapOf<String, List<ArticleBean>>()

        articleIds.onSubList { subArticleIds ->
            val data = articleDao.getArticleListByIds(subArticleIds).groupBy { it.feedUrl }
            data.forEach { (k, v) ->
                val rule = autoDownloadRuleDao.getRuleByFeedUrl(k).first()
                if (rule == null || !rule.enabled) return@forEach

                val pattern = rule.filterPattern
                val matchesArticles = mutableListOf<ArticleBean>()
                if (pattern != null) {
                    val regex = runCatching { Regex(pattern) }.getOrNull()
                    if (regex != null) {
                        matchesArticles += v.filter {
                            regex.matches(it.title.orEmpty()) ||
                                    regex.matches(it.description.orEmpty()) ||
                                    regex.matches(it.content.orEmpty())
                        }
                    }
                } else {
                    matchesArticles += v
                }
                feedUrlWithArticles[k] = (feedUrlWithArticles[k].orEmpty() + matchesArticles)
                    .sortedBy { -(it.updateAt ?: 0L) }
                    .take(rule.maxDownloadCount.coerceAtLeast(0))
            }
        }

        if (feedUrlWithArticles.isNotEmpty()) {
            PlatformAutoDownload.autoDownload(feedUrlWithArticles)
        }
    }

    private suspend fun sendNotification(articleIds: List<String>) {
        val articleNotificationRuleDao = get<ArticleNotificationRuleDao>()
        val articleDao = get<ArticleDao>()

        val rules = articleNotificationRuleDao.getAllArticleNotificationRules().first()
            .mapNotNull { rule -> rule.compileMatcher()?.let { rule to it } }
        if (rules.isEmpty()) return
        val matchedData = mutableListOf<Pair<String, ArticleNotificationRuleBean>>()
        articleIds.onSubList { subArticleIds ->
            val data = articleDao.getArticleWithEnclosureListByIds(subArticleIds)
            val feeds = get<FeedDao>().observeFeeds(data.map { it.article.feedUrl }.distinct())
                .first().associateBy { it.url }
            matchedData += data.mapNotNull { item ->
                val feed = feeds[item.article.feedUrl] ?: return@mapNotNull null
                val matchedRule = rules.firstOrNull { (_, matches) ->
                    matches(item, feed.groupId)
                }?.first
                matchedRule?.let { item.article.articleId to matchedRule }
            }
        }
        if (matchedData.isNotEmpty()) {
            matchedData.onSubList(step = 5000) {
                PlatformArticleNotification.sendNotification(it)
            }
        }
    }
}

internal suspend fun deliverArticleUpdates(
    articleIds: List<String>,
    notify: suspend (List<String>) -> Unit,
    download: suspend (List<String>) -> Unit,
) {
    var failure: Exception? = null
    for (deliver in listOf(notify, download)) {
        try {
            deliver(articleIds)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (failure == null) failure = error else failure.addSuppressed(error)
        }
    }
    failure?.let { throw it }
}

expect object PlatformArticleNotification {
    suspend fun requestPermission(showSettingsIfDenied: Boolean = true)
    suspend fun sendNotification(matchedData: List<Pair<String, ArticleNotificationRuleBean>>)
}

expect object PlatformAutoDownload {
    suspend fun autoDownload(data: Map<String, List<ArticleBean>>)
}
