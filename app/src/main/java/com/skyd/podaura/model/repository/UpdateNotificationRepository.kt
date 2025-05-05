package com.skyd.podaura.model.repository

import com.skyd.podaura.model.bean.ArticleNotificationRuleBean
import com.skyd.podaura.model.db.dao.ArticleNotificationRuleDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.koin.core.annotation.Factory

@Factory(binds = [])
class UpdateNotificationRepository(
    private val articleNotificationRuleDao: ArticleNotificationRuleDao,
) : BaseRepository() {
    fun getAllRules(): Flow<List<ArticleNotificationRuleBean>> =
        articleNotificationRuleDao.getAllArticleNotificationRules()
            .flowOn(Dispatchers.IO)

    fun addRule(rule: ArticleNotificationRuleBean): Flow<Unit> = flow {
        emit(articleNotificationRuleDao.setArticleNotificationRule(rule))
    }.flowOn(Dispatchers.IO)

    fun removeRule(ruleId: Int): Flow<Int> = flow {
        emit(articleNotificationRuleDao.removeArticleNotificationRule(ruleId))
    }.flowOn(Dispatchers.IO)
}