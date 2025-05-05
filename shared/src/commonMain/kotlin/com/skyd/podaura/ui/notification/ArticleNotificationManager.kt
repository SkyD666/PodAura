package com.skyd.podaura.ui.notification

import com.skyd.podaura.di.get
import com.skyd.podaura.ext.onSubList
import com.skyd.podaura.model.bean.ArticleNotificationRuleBean
import com.skyd.podaura.model.db.dao.ArticleDao
import com.skyd.podaura.model.db.dao.ArticleNotificationRuleDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

object ArticleNotificationManager {
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
        val articleNotificationRuleDao = get<ArticleNotificationRuleDao>()
        val articleDao = get<ArticleDao>()
        while (isActive) {
            val articleIds = mutableListOf<List<String>>()

            if (isActive) articleIds += channel.receive()  // Suspend here when no data
            while (isActive) {
                articleIds += withTimeoutOrNull(20.seconds) {
                    channel.receive()
                } ?: break
            }

            val rules = articleNotificationRuleDao.getAllArticleNotificationRules().first()
            val matchedData = mutableListOf<Pair<String, ArticleNotificationRuleBean>>()
            articleIds.flatten().onSubList { subArticleIds ->
                val data = articleDao.getArticleWithEnclosureListByIds(subArticleIds)
                matchedData += data.mapNotNull { item ->
                    val matchedRule = rules.firstOrNull { it.match(item) }
                    matchedRule?.let { item.article.articleId to matchedRule }
                }
            }
            if (matchedData.isNotEmpty()) {
                matchedData.onSubList(step = 5000) { sendNotification(it) }
            }
        }
    }

    private fun sendNotification(matchedData: List<Pair<String, ArticleNotificationRuleBean>>) {
        PlatformArticleNotification.sendNotification(matchedData)
    }
}

expect object PlatformArticleNotification {
    fun sendNotification(matchedData: List<Pair<String, ArticleNotificationRuleBean>>)
}