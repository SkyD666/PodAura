package com.skyd.podaura.model.repository.playlist

import com.skyd.podaura.model.bean.playlist.MediaUrlWithArticleIdBean
import com.skyd.podaura.model.db.dao.playlist.PlaylistDao
import com.skyd.podaura.model.db.dao.playlist.PlaylistMediaDao
import com.skyd.podaura.model.repository.BaseRepository
import com.skyd.podaura.model.repository.BatchProgress
import com.skyd.podaura.model.repository.processBatch
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn

class AddToPlaylistRepository(
    private val playlistDao: PlaylistDao,
    private val playlistMediaDao: PlaylistMediaDao,
) : BaseRepository(), IAddToPlaylistRepository {
    override fun getCommonPlaylists(
        medias: List<MediaUrlWithArticleIdBean>
    ): Flow<List<String>> {
        val urls = medias.map { it.url }.distinct()
        if (urls.isEmpty()) return flowOf(emptyList())
        return combine(urls.chunked(900).map { chunk ->
            playlistMediaDao.getCommonMediaPlaylistIdList(chunk, chunk.size)
        }) { chunks ->
            chunks.map { it.toSet() }.reduce { common, ids -> common.intersect(ids) }.toList()
        }.flowOn(Dispatchers.IO)
    }

    fun insertSelectedPlaylistMedias(
        playlistId: String,
        medias: List<MediaUrlWithArticleIdBean>,
    ): Flow<BatchProgress> = processBatch(medias.distinctBy { it.url }) {
        // Foreign keys reject deleted playlists; IGNORE reports existing media as skipped.
        playlistMediaDao.appendPlaylistMedia(
            playlistId, it.url, it.articleId, Clock.System.now().toEpochMilliseconds(),
        ) != -1L
    }.flowOn(Dispatchers.IO)

    override fun insertPlaylistMedia(
        playlistId: String,
        url: String,
        articleId: String?
    ): Flow<Boolean> = flow {
        if (playlistDao.exists(playlistId) == 0) {
            emit(false)
            return@flow
        }
        emit(
            playlistMediaDao.appendPlaylistMedia(
                playlistId, url, articleId, Clock.System.now().toEpochMilliseconds(),
            ) != -1L
        )
    }.flowOn(Dispatchers.IO)

    override fun insertPlaylistMedias(
        toPlaylistId: String,
        medias: List<MediaUrlWithArticleIdBean>
    ): Flow<Unit> = flow {
        if (playlistDao.exists(toPlaylistId) == 0) {
            emit(Unit)
            return@flow
        }
        medias.forEach {
            insertPlaylistMedia(
                playlistId = toPlaylistId,
                url = it.url,
                articleId = it.articleId,
            ).collect()
        }
        emit(Unit)
    }.flowOn(Dispatchers.IO)

    override fun removeMediaFromPlaylist(
        playlistId: String,
        mediaList: List<MediaUrlWithArticleIdBean>,
    ): Flow<Int> = flow {
        emit(playlistMediaDao.deletePlaylistMedia(playlistId = playlistId, mediaList = mediaList))
    }.flowOn(Dispatchers.IO)
}
