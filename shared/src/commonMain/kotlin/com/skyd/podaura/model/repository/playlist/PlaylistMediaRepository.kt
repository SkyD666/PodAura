package com.skyd.podaura.model.repository.playlist

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room3.withWriteTransaction
import com.skyd.podaura.ext.flowOf
import com.skyd.podaura.model.bean.playlist.PlaylistMediaBean
import com.skyd.podaura.model.bean.playlist.MediaUrlWithArticleIdBean
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.model.bean.playlist.mergePlaylistOrder
import com.skyd.podaura.model.bean.playlist.movePlaylistItem
import com.skyd.podaura.model.bean.playlist.updateLocalMediaMetadata
import com.skyd.podaura.model.db.AppDatabase
import com.skyd.podaura.model.db.dao.ArticleDao
import com.skyd.podaura.model.db.dao.EnclosureDao
import com.skyd.podaura.model.db.dao.playlist.PlaylistMediaDao.Companion.ORDER_DELTA
import com.skyd.podaura.model.preference.behavior.playlist.BasePlaylistSortByPreference.Companion.CREATE_TIME
import com.skyd.podaura.model.preference.behavior.playlist.BasePlaylistSortByPreference.Companion.MANUAL
import com.skyd.podaura.model.preference.behavior.playlist.PlaylistMediaSortAscPreference
import com.skyd.podaura.model.preference.behavior.playlist.PlaylistMediaSortByPreference
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.repository.BaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class PlaylistMediaRepository(
    private val articleDao: ArticleDao,
    private val enclosureDao: EnclosureDao,
    private val database: AppDatabase,
    private val pagingConfig: PagingConfig,
) : BaseRepository(), IPlaylistMediaRepository {
    private val playlistMediaDao = database.playlistItemDao()
    private val playlistDao = database.playlistDao()

    private suspend fun initializeSort() {
        if (playlistDao.hasLegacyMediaSort()) {
            val (sortBy, ascending) = dataStore.flowOf(
                PlaylistMediaSortByPreference, PlaylistMediaSortAscPreference,
            ).first()
            playlistDao.initializeMediaSort(sortBy, ascending)
        }
    }

    suspend fun setSort(playlistId: String, sortBy: String, ascending: Boolean) {
        require(sortBy == MANUAL || sortBy == CREATE_TIME)
        initializeSort()
        check(playlistDao.setMediaSort(playlistId, sortBy, ascending) == 1)
    }

    fun observePlaylistMediaList(playlistId: String): Flow<List<PlaylistMediaWithArticleBean>> = flow {
        initializeSort()
        emitAll(playlistMediaDao.observePlaylistMediaList(playlistId).distinctUntilChanged())
    }.flowOn(Dispatchers.IO)

    suspend fun removeMedia(playlistId: String, urls: List<String>) {
        require(playlistId.isNotEmpty())
        playlistMediaDao.deletePlaylistMedia(playlistId, urls.map { MediaUrlWithArticleIdBean(it, null) })
    }

    private suspend fun PlaylistMediaWithArticleBean.updateMediaMetadata(): PlaylistMediaWithArticleBean {
        var result = this
        var realArticleId = playlistMediaBean.articleId
        if (realArticleId != null) {
            val oldArticleIdNotExists = articleDao.exists(realArticleId) == 0
            if (oldArticleIdNotExists) {
                realArticleId = enclosureDao.getMediaArticleId(playlistMediaBean.url)
                if (realArticleId != null) {
                    playlistMediaDao.updatePlaylistMediaArticleId(
                        playlistId = playlistMediaBean.playlistId,
                        url = playlistMediaBean.url,
                        articleId = realArticleId
                    )
                }
            }
            if (oldArticleIdNotExists) {
                result = copy(
                    article = realArticleId?.let { articleDao.getArticleWithFeed(it).first() },
                )
                // todo use worker to update articleId
            }
            result.playlistMediaBean.duration = article?.articleWithEnclosure?.media?.duration
            result.playlistMediaBean.thumbnail = article?.getThumbnail()
        } else {
            if (playlistMediaBean.isLocalFile) {
                playlistMediaBean.updateLocalMediaMetadata()
            }
        }
        return result
    }

    fun requestPlaylistMediaListPaging(playlistId: String): Flow<PagingData<PlaylistMediaWithArticleBean>> =
        flow {
            initializeSort()
            emitAll(
                playlistDao.observePlaylist(playlistId)
                .map { it?.let { it.mediaSortBy to it.mediaSortAsc } ?: (MANUAL to true) }
                .distinctUntilChanged()
                .flatMapLatest { (sortBy, sortAsc) ->
                    Pager(pagingConfig) {
                        playlistMediaDao.getPlaylistMediaListPaging(
                            playlistId = playlistId,
                            orderByColumnName = if (sortBy == CREATE_TIME) PlaylistMediaBean.CREATE_TIME_COLUMN
                            else PlaylistMediaBean.ORDER_POSITION_COLUMN,
                            asc = sortBy != CREATE_TIME || sortAsc,
                        )
                    }.flow.map { data ->
                        data.map { withContext(Dispatchers.IO) { it.updateMediaMetadata() } }
                    }
                })
        }.flowOn(Dispatchers.IO)

    override fun requestPlaylistMediaList(
        playlistId: String
    ): Flow<List<PlaylistMediaWithArticleBean>> = flow {
        initializeSort()
        val playlist = sortedMedia(playlistId)
        emit(playlist.map { it.updateMediaMetadata() })
    }.flowOn(Dispatchers.IO)

    private suspend fun sortedMedia(playlistId: String): List<PlaylistMediaWithArticleBean> {
        val settings =
            checkNotNull(playlistDao.getPlaylist(playlistId)) { "Playlist no longer exists" }
        val items = playlistMediaDao.getPlaylistMediaList(playlistId)
        return if (settings.mediaSortBy == CREATE_TIME) {
            // Stable ties match the paged SQL order in either time direction.
            items.sortedWith(compareBy<PlaylistMediaWithArticleBean> {
                it.playlistMediaBean.createTime
            }.let { if (settings.mediaSortAsc) it else it.reversed() }
                .thenBy { it.playlistMediaBean.orderPosition }.thenBy { it.playlistMediaBean.url })
        } else items
    }

    /** One transaction per completed drag, including the switch from time to manual sorting. */
    suspend fun reorderPlaylistMedia(
        playlistId: String,
        fromUrl: String,
        toUrl: String,
        displayedOrder: List<String>? = null,
        fromQueue: Boolean = false,
        applyOrder: (List<String>) -> Unit = {},
    ) {
        if (fromUrl == toUrl) return
        initializeSort()
        database.withWriteTransaction {
            val settings =
                checkNotNull(playlistDao.getPlaylist(playlistId)) { "Playlist no longer exists" }
            val existing = sortedMedia(playlistId).map { it.playlistMediaBean.url }
            if (!fromQueue) require(fromUrl in existing && toUrl in existing) {
                "The playlist changed during the drag"
            }
            val moved = movePlaylistItem(displayedOrder ?: existing, fromUrl, toUrl)
            // Paged snapshots contain loaded rows only. Other rows keep their slots, including
            // anything added while dragging. Queue-only rows never get inserted into the source.
            val saved = mergePlaylistOrder(existing, moved)
            saved.forEachIndexed { index, url ->
                playlistMediaDao.reorderPlaylistMedia(playlistId, url, (index + 1) * ORDER_DELTA)
            }
            playlistDao.setMediaSort(playlistId, MANUAL, settings.mediaSortAsc)
            applyOrder(if (fromQueue) moved else saved)
        }
    }
}
