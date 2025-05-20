package com.skyd.podaura.ui.notification

import com.skyd.podaura.di.get
import com.skyd.podaura.ext.onSubList
import com.skyd.podaura.model.bean.ArticleNotificationRuleBean
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.db.dao.ArticleDao
import com.skyd.podaura.model.db.dao.ArticleNotificationRuleDao
import com.skyd.podaura.model.db.dao.download.AutoDownloadRuleDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

object ArticleUpdatedManager {
    const val CHANNEL_ID = "articleNotification"

    private val scope = CoroutineScope(Dispatchers.IO)
    private val channel = Channel<List<String>>(capacity = Channel.UNLIMITED)

    init {
        onBatch()
    }

    fun send(articleIds: List<String>) = scope.launch {
        channel.send(articleIds)
    }

    private fun onBatch() = scope.launch {
        while (isActive) {
            val articleIds = mutableListOf<List<String>>()

            if (isActive) articleIds += channel.receive()  // Suspend here when no data
            while (isActive) {
                articleIds += withTimeoutOrNull(20.seconds) {
                    channel.receive()
                } ?: break
            }

            val flattenArticleIds = articleIds.flatten()

            sendNotification(flattenArticleIds)
            autoDownload(flattenArticleIds)
        }
    }

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
        val matchedData = mutableListOf<Pair<String, ArticleNotificationRuleBean>>()
        articleIds.onSubList { subArticleIds ->
            val data = articleDao.getArticleWithEnclosureListByIds(subArticleIds)
            matchedData += data.mapNotNull { item ->
                val matchedRule = rules.firstOrNull { it.match(item) }
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

expect object PlatformArticleNotification {
    fun sendNotification(matchedData: List<Pair<String, ArticleNotificationRuleBean>>)
}

expect object PlatformAutoDownload {
    suspend fun autoDownload(data: Map<String, List<ArticleBean>>)
}