package com.skyd.anivu.model.repository.playlist

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.skyd.anivu.appContext
import com.skyd.anivu.base.BaseRepository
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.getOrDefault
import com.skyd.anivu.model.bean.playlist.PlaylistBean
import com.skyd.anivu.model.bean.playlist.PlaylistViewBean
import com.skyd.anivu.model.db.dao.playlist.PlaylistDao
import com.skyd.anivu.model.db.dao.playlist.PlaylistDao.Companion.ORDER_DELTA
import com.skyd.anivu.model.db.dao.playlist.PlaylistDao.Companion.ORDER_MIN_DELTA
import com.skyd.anivu.model.db.dao.playlist.PlaylistMediaDao
import com.skyd.anivu.model.preference.behavior.playlist.BasePlaylistSortByPreference.Companion.CreateTime
import com.skyd.anivu.model.preference.behavior.playlist.BasePlaylistSortByPreference.Companion.Manual
import com.skyd.anivu.model.preference.behavior.playlist.BasePlaylistSortByPreference.Companion.MediaCount
import com.skyd.anivu.model.preference.behavior.playlist.BasePlaylistSortByPreference.Companion.Name
import com.skyd.anivu.model.preference.behavior.playlist.PlaylistSortAscPreference
import com.skyd.anivu.model.preference.behavior.playlist.PlaylistSortByPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val playlistMediaDao: PlaylistMediaDao,
    private val pagingConfig: PagingConfig,
) : BaseRepository() {
    private suspend fun PlaylistViewBean.updateThumbnails() = apply {
        thumbnails = playlistMediaDao.getPlaylistMediaArticles(
            playlistId = playlist.playlistId,
            count = 4,
        ).mapNotNull { it.getThumbnail() }
    }

    fun requestPlaylist(playlistId: String): Flow<PlaylistViewBean> = flow {
        emit(playlistDao.getPlaylistView(playlistId).updateThumbnails())
    }.flowOn(Dispatchers.IO)

    fun requestPlaylistList(): Flow<PagingData<PlaylistViewBean>> {
        return appContext.dataStore.data.map {
            Pair(
                it[PlaylistSortByPreference.key] ?: PlaylistSortByPreference.default,
                it[PlaylistSortAscPreference.key] ?: PlaylistSortAscPreference.default,
            )
        }.distinctUntilChanged().flatMapLatest { (sortBy, sortAsc) ->
            val sortByColumnName = when (sortBy) {
                Name -> PlaylistBean.NAME_COLUMN
                MediaCount -> PlaylistViewBean.ITEM_COUNT_COLUMN
                Manual -> PlaylistBean.ORDER_POSITION_COLUMN
                CreateTime -> PlaylistBean.CREATE_TIME_COLUMN
                else -> PlaylistBean.ORDER_POSITION_COLUMN
            }
            val realSortAsc = if (sortBy == Manual) true else sortAsc
            Pager(pagingConfig) {
                playlistDao.getPlaylistList(
                    orderByColumnName = sortByColumnName,
                    asc = realSortAsc,
                )
            }.flow.map { pagingData ->
                pagingData.map { playlistView -> playlistView.updateThumbnails() }
            }
        }.flowOn(Dispatchers.IO)
    }

    fun createPlaylist(name: String): Flow<Unit> = flow {
        val orderPosition = playlistDao.getMaxOrder() + ORDER_DELTA
        playlistDao.createPlaylist(
            PlaylistBean(
                playlistId = UUID.randomUUID().toString(),
                name = name,
                orderPosition = orderPosition,
                createTime = System.currentTimeMillis(),
                deleteMediaOnFinish = false,
            )
        )
        emit(Unit)
    }.flowOn(Dispatchers.IO)

    fun deletePlaylist(playlistId: String): Flow<Int> = flow {
        emit(playlistDao.deletePlaylist(playlistId = playlistId))
    }.flowOn(Dispatchers.IO)

    fun renamePlaylist(playlistId: String, newName: String): Flow<Int> = flow {
        emit(playlistDao.renamePlaylist(playlistId = playlistId, name = newName))
    }.flowOn(Dispatchers.IO)

    fun reorderPlaylist(
        fromIndex: Int,
        toIndex: Int,
    ): Flow<Int> = flow {
        if (appContext.dataStore.getOrDefault(PlaylistSortByPreference) != Manual ||
            fromIndex == toIndex
        ) {
            emit(0)
            return@flow
        }
        val fromPlaylist = playlistDao.getNth(fromIndex)
        if (fromPlaylist == null) {
            emit(0)
            return@flow
        }
        val prevPlaylist = if (fromIndex < toIndex) {
            playlistDao.getNth(toIndex)
        } else {
            if (toIndex - 1 >= 0) playlistDao.getNth(toIndex - 1) else null
        }

        val prevOrder: Double
        val nextOrder: Double

        if (prevPlaylist == null) {  // Insert to first
            val minOrder = playlistDao.getMinOrder()
            prevOrder = minOrder - ORDER_DELTA * 2
            nextOrder = minOrder
        } else {
            val nextPlaylist = playlistDao.getNth(
                index = if (fromIndex < toIndex) toIndex + 1 else toIndex,
            )
            if (nextPlaylist == null) {
                val maxOrder = playlistDao.getMaxOrder()
                prevOrder = maxOrder
                nextOrder = maxOrder + ORDER_DELTA * 2
            } else {
                prevOrder = prevPlaylist.orderPosition
                nextOrder = nextPlaylist.orderPosition
            }
        }
        if (nextOrder - prevOrder < ORDER_MIN_DELTA * 2) {
            playlistDao.reindexOrders()
            emit(reorderPlaylist(fromIndex = fromIndex, toIndex = toIndex).first())
            return@flow
        } else {
            emit(
                playlistDao.reorderPlaylist(
                    playlistId = fromPlaylist.playlistId,
                    orderPosition = (prevOrder + nextOrder) / 2.0,
                )
            )
        }
    }.flowOn(Dispatchers.IO)
}