package com.skyd.podaura.model.repository

import com.skyd.podaura.model.bean.article.ArticleWithFeed
import com.skyd.podaura.model.bean.history.ReadHistoryBean
import com.skyd.podaura.model.db.dao.ArticleDao
import com.skyd.podaura.model.db.dao.ReadHistoryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlin.time.Clock

class ReadRepository(
    private val articleDao: ArticleDao,
    private val readHistoryDao: ReadHistoryDao,
) : BaseRepository() {
    fun requestArticleWithFeed(
        articleId: String,
    ): Flow<ArticleWithFeed?> = articleDao.getArticleWithFeed(articleId = articleId)
        .filterNotNull()
        .onEach {
            readHistoryDao.updateReadHistory(
                ReadHistoryBean(
                    articleId = articleId,
                    lastTime = Clock.System.now().toEpochMilliseconds(),
                )
            )
        }
        .flowOn(Dispatchers.IO)
}
