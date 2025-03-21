package com.skyd.anivu.model.repository

import com.skyd.anivu.appContext
import com.skyd.anivu.base.BaseRepository
import com.skyd.anivu.config.Const
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.deleteRecursivelyExclude
import com.skyd.anivu.ext.getOrDefault
import com.skyd.anivu.model.db.dao.ArticleDao
import com.skyd.anivu.model.db.dao.FeedDao
import com.skyd.anivu.model.db.dao.MediaPlayHistoryDao
import com.skyd.anivu.model.preference.data.delete.KeepFavoriteArticlesPreference
import com.skyd.anivu.model.preference.data.delete.KeepPlaylistArticlesPreference
import com.skyd.anivu.model.preference.data.delete.KeepUnreadArticlesPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class DataRepository @Inject constructor(
    private val feedDao: FeedDao,
    private val articleDao: ArticleDao,
    private val mediaPlayHistoryDao: MediaPlayHistoryDao,
) : BaseRepository() {
    fun requestClearCache(): Flow<Long> = flow {
        var size: Long = 0
        Const.TEMP_TORRENT_DIR.deleteRecursivelyExclude(hook = {
            if (!it.canWrite()) return@deleteRecursivelyExclude false
            if (it.isFile) size += it.length()
            true
        })
        Const.FEED_ICON_DIR.walkBottomUp().forEach {
            if (it.path != Const.FEED_ICON_DIR.path) {
                val contains = feedDao.containsByCustomIcon(it.path)
                if (contains == 0) {
                    val s = it.length()
                    if (it.delete()) {
                        size += s
                    }
                }
            }
        }
        emit(size)
    }.flowOn(Dispatchers.IO)

    fun requestDeletePlayHistory(): Flow<Int> = flow {
        emit(mediaPlayHistoryDao.deleteAllMediaPlayHistory())
    }.flowOn(Dispatchers.IO)

    fun requestDeleteArticleBefore(timestamp: Long): Flow<Int> = flow {
        val count = with(appContext.dataStore) {
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