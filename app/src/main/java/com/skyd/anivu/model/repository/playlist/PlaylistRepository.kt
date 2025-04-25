package com.skyd.anivu.model.repository.playlist

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.skyd.anivu.model.repository.BaseRepository
import com.skyd.anivu.ext.flowOf
import com.skyd.anivu.ext.getOrDefault
import com.skyd.anivu.model.bean.playlist.PlaylistBean
import com.skyd.anivu.model.bean.playlist.PlaylistViewBean
import com.skyd.anivu.model.db.dao.playlist.PlaylistDao
import com.skyd.anivu.model.db.dao.playlist.PlaylistDao.Companion.ORDER_DELTA
import com.skyd.anivu.model.db.dao.playlist.PlaylistDao.Companion.ORDER_MIN_DELTA
import com.skyd.anivu.model.db.dao.playlist.PlaylistMediaDao
import com.skyd.anivu.model.preference.behavior.playlist.BasePlaylistSortByPreference.Companion.CREATE_TIME
import com.skyd.anivu.model.preference.behavior.playlist.BasePlaylistSortByPreference.Companion.MANUAL
import com.skyd.anivu.model.preference.behavior.playlist.BasePlaylistSortByPreference.Companion.MEDIA_COUNT
import com.skyd.anivu.model.preference.behavior.playlist.BasePlaylistSortByPreference.Companion.NAME
import com.skyd.anivu.model.preference.behavior.playlist.PlaylistSortAscPreference
import com.skyd.anivu.model.preference.behavior.playlist.PlaylistSortByPreference
import com.skyd.anivu.model.preference.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Factory
import java.util.UUID

@Factory(binds = [IPlaylistRepository::class])
class PlaylistRepository(
    private val playlistDao: PlaylistDao,
    private val playlistMediaDao: PlaylistMediaDao,
    private val pagingConfig: PagingConfig,
) : BaseRepository(), IPlaylistRepository {
    private suspend fun PlaylistViewBean.updateThumbnails() = apply {
        thumbnails = playlistMediaDao.getPlaylistMediaArticles(
            playlistId = playlist.playlistId,
            count = 4,
        ).mapNotNull { it.getThumbnail() }
    }

    override fun requestPlaylist(playlistId: String): Flow<PlaylistViewBean> = flow {
        emit(playlistDao.getPlaylistView(playlistId).updateThumbnails())
    }.flowOn(Dispatchers.IO)

    override fun requestPlaylistList(): Flow<PagingData<PlaylistViewBean>> {
        return dataStore.flowOf(
            PlaylistSortByPreference, PlaylistSortAscPreference
        ).distinctUntilChanged().flatMapLatest { (sortBy, sortAsc) ->
            val sortByColumnName = when (sortBy) {
                NAME -> PlaylistBean.NAME_COLUMN
                MEDIA_COUNT -> PlaylistViewBean.ITEM_COUNT_COLUMN
                MANUAL -> PlaylistBean.ORDER_POSITION_COLUMN
                CREATE_TIME -> PlaylistBean.CREATE_TIME_COLUMN
                else -> PlaylistBean.ORDER_POSITION_COLUMN
            }
            val realSortAsc = if (sortBy == MANUAL) true else sortAsc
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
        if (dataStore.getOrDefault(PlaylistSortByPreference) != MANUAL ||
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