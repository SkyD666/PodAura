package com.skyd.anivu.model.repository.player

import com.skyd.anivu.base.BaseRepository
import com.skyd.anivu.model.bean.article.ArticleWithFeed
import com.skyd.anivu.model.bean.history.MediaPlayHistoryBean
import com.skyd.anivu.model.db.dao.ArticleDao
import com.skyd.anivu.model.db.dao.EnclosureDao
import com.skyd.anivu.model.db.dao.MediaPlayHistoryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class PlayerRepository @Inject constructor(
    private val mediaPlayHistoryDao: MediaPlayHistoryDao,
    private val articleDao: ArticleDao,
    private val enclosureDao: EnclosureDao,
) : BaseRepository() {
    fun insertPlayHistory(path: String, duration: Long, articleId: String?): Flow<Unit> {
        return flow {
            val realArticleId = articleId?.takeIf {
                articleDao.exists(it) > 0
            } ?: enclosureDao.getMediaArticleId(path)

            val old = mediaPlayHistoryDao.getMediaPlayHistory(path)
            val currentHistory = old?.copy(
                duration = duration,
                lastTime = System.currentTimeMillis(),
                articleId = realArticleId,
            ) ?: MediaPlayHistoryBean(
                path = path,
                duration = duration,
                lastPlayPosition = 0L,
                lastTime = System.currentTimeMillis(),
                articleId = realArticleId,
            )
            mediaPlayHistoryDao.updateMediaPlayHistory(currentHistory)
            emit(Unit)
        }.flowOn(Dispatchers.IO)
    }

    fun updateLastPlayPosition(path: String, lastPlayPosition: Long): Flow<Unit> {
        return flow {
            mediaPlayHistoryDao.updateLastPlayPosition(path, lastPlayPosition)
            emit(Unit)
        }.flowOn(Dispatchers.IO)
    }

    fun requestLastPlayPosition(path: String): Flow<Long> {
        return flow {
            emit(mediaPlayHistoryDao.getMediaPlayHistory(path)?.lastPlayPosition ?: 0L)
        }.flowOn(Dispatchers.IO)
    }

    fun requestPlaylist(articleId: String): List<ArticleWithFeed> {
        return articleDao.getArticlesForPlaylist(articleId)
    }
}