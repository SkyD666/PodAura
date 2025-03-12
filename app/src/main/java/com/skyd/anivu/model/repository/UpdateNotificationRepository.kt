package com.skyd.anivu.model.repository

import com.skyd.anivu.base.BaseRepository
import com.skyd.anivu.model.bean.ArticleNotificationRuleBean
import com.skyd.anivu.model.db.dao.ArticleNotificationRuleDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class UpdateNotificationRepository @Inject constructor(
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