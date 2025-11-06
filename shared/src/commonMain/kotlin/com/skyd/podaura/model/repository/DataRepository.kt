package com.skyd.podaura.model.repository

import com.skyd.fundation.config.Const
import com.skyd.fundation.config.FEED_ICON_DIR
import com.skyd.fundation.ext.deleteRecursively
import com.skyd.fundation.ext.walk
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.model.db.dao.ArticleDao
import com.skyd.podaura.model.db.dao.FeedDao
import com.skyd.podaura.model.db.dao.MediaPlayHistoryDao
import com.skyd.podaura.model.preference.data.delete.KeepFavoriteArticlesPreference
import com.skyd.podaura.model.preference.data.delete.KeepPlaylistArticlesPreference
import com.skyd.podaura.model.preference.data.delete.KeepUnreadArticlesPreference
import com.skyd.podaura.model.preference.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.io.files.Path

class DataRepository(
    private val feedDao: FeedDao,
    private val articleDao: ArticleDao,
    private val mediaPlayHistoryDao: MediaPlayHistoryDao,
) : BaseRepository() {
    fun requestClearCache(): Flow<Long> = flow {
        var size: Long = 0
        Path(Const.FEED_ICON_DIR).walk().forEach {
            if (it.toString() != Const.FEED_ICON_DIR) {
                val contains = feedDao.containsByCustomIcon(it.toString())
                if (contains == 0) {
                    size += it.deleteRecursively() ?: 0L
                }
            }
        }
        emit(size)
    }.flowOn(Dispatchers.IO)

    fun requestDeletePlayHistory(): Flow<Int> = flow {
        emit(mediaPlayHistoryDao.deleteAllMediaPlayHistory())
    }.flowOn(Dispatchers.IO)

    fun requestDeleteArticleBefore(timestamp: Long): Flow<Int> = flow {
        val count = with(dataStore) {
            articleDao.deleteArticleBefore(
                timestamp = timestamp,
                keepPlaylistArticles = getOrDefault(KeepPlaylistArticlesPreference),
                keepUnread = getOrDefault(KeepUnreadArticlesPreference),
                keepFavorite = getOrDefault(KeepFavoriteArticlesPreference),
            )
        }
        emit(count)
    }.flowOn(Dispatchers.IO)
}