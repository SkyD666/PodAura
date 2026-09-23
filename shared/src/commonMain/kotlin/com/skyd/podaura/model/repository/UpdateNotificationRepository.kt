package com.skyd.podaura.model.repository

import com.skyd.podaura.model.bean.ArticleNotificationRuleBean
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.bean.group.GroupBean
import com.skyd.podaura.model.db.dao.ArticleNotificationRuleDao
import com.skyd.podaura.model.db.dao.FeedDao
import com.skyd.podaura.model.db.dao.GroupDao
import com.skyd.podaura.ui.notification.PlatformArticleNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class UpdateNotificationRepository(
    private val articleNotificationRuleDao: ArticleNotificationRuleDao,
    private val feedDao: FeedDao,
    private val groupDao: GroupDao,
) : BaseRepository() {
    fun addRule(rule: ArticleNotificationRuleBean): Flow<Unit> = flow {
        articleNotificationRuleDao.saveUserRule(rule)
        PlatformArticleNotification.requestPermission()
        emit(Unit)
    }.flowOn(Dispatchers.IO)

    fun removeRule(ruleId: Int): Flow<Int> = flow {
        emit(articleNotificationRuleDao.removeArticleNotificationRule(ruleId))
    }.flowOn(Dispatchers.IO)

    fun observeRules(): Flow<NotificationRuleData> = combine(
        articleNotificationRuleDao.getAllArticleNotificationRules(),
        feedDao.observeAllFeeds(),
        groupDao.observeAllGroups(),
    ) { rules, feeds, groups -> NotificationRuleData(rules, feeds, groups) }.flowOn(Dispatchers.IO)
}

data class NotificationRuleData(
    val rules: List<ArticleNotificationRuleBean>,
    val feeds: List<FeedBean>,
    val groups: List<GroupBean>,
)
