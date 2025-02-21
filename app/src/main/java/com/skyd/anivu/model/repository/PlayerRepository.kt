package com.skyd.anivu.model.repository

import com.skyd.anivu.base.BaseRepository
import com.skyd.anivu.model.bean.history.MediaPlayHistoryBean
import com.skyd.anivu.model.db.dao.MediaPlayHistoryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class PlayerRepository @Inject constructor(
    private val mediaPlayHistoryDao: MediaPlayHistoryDao,
) : BaseRepository() {
    fun insertPlayHistory(path: String, articleId: String?): Flow<Unit> {
        return flow {
            val old = mediaPlayHistoryDao.getMediaPlayHistory(path)
            val currentHistory = old?.copy(
                lastTime = System.currentTimeMillis(),
                articleId = articleId,
            ) ?: MediaPlayHistoryBean(
                path = path,
                lastPlayPosition = 0,
                lastTime = System.currentTimeMillis(),
                articleId = articleId,
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
}