package com.skyd.anivu.model.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.skyd.anivu.base.BaseRepository
import com.skyd.anivu.model.bean.history.MediaPlayHistoryWithArticle
import com.skyd.anivu.model.bean.history.ReadHistoryWithArticle
import com.skyd.anivu.model.db.dao.MediaPlayHistoryDao
import com.skyd.anivu.model.db.dao.ReadHistoryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class HistoryRepository @Inject constructor(
    private val readHistoryDao: ReadHistoryDao,
    private val mediaPlayHistoryDao: MediaPlayHistoryDao,
    private val pagingConfig: PagingConfig,
) : BaseRepository() {
    fun requestReadHistoryList(): Flow<Pager<Int, ReadHistoryWithArticle>> = flow {
        emit(Pager(pagingConfig) { readHistoryDao.getReadHistoryList() })
    }.flowOn(Dispatchers.IO)

    fun requestMediaPlayHistoryList(): Flow<Pager<Int, MediaPlayHistoryWithArticle>> = flow {
        emit(Pager(pagingConfig) { mediaPlayHistoryDao.getMediaPlayHistoryList() })
    }.flowOn(Dispatchers.IO)

    fun deleteReadHistory(articleId: String): Flow<Int> = flow {
        emit(readHistoryDao.deleteReadHistory(articleId))
    }.flowOn(Dispatchers.IO)

    fun deleteMediaPlayHistory(path: String): Flow<Int> = flow {
        emit(mediaPlayHistoryDao.deleteMediaPlayHistory(path))
    }.flowOn(Dispatchers.IO)

    fun deleteAllReadHistory(): Flow<Int> = flow {
        emit(readHistoryDao.deleteAllReadHistory())
    }.flowOn(Dispatchers.IO)

    fun deleteAllMediaPlayHistory(): Flow<Int> = flow {
        emit(mediaPlayHistoryDao.deleteAllMediaPlayHistory())
    }.flowOn(Dispatchers.IO)
}