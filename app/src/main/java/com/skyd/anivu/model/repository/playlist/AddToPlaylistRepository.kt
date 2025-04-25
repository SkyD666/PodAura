package com.skyd.anivu.model.repository.playlist

import com.skyd.anivu.model.repository.BaseRepository
import com.skyd.anivu.model.bean.playlist.MediaUrlWithArticleIdBean
import com.skyd.anivu.model.bean.playlist.PlaylistMediaBean
import com.skyd.anivu.model.db.dao.ArticleDao
import com.skyd.anivu.model.db.dao.playlist.PlaylistDao
import com.skyd.anivu.model.db.dao.playlist.PlaylistMediaDao
import com.skyd.anivu.model.db.dao.playlist.PlaylistMediaDao.Companion.ORDER_DELTA
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.koin.core.annotation.Factory

@Factory(binds = [IAddToPlaylistRepository::class])
class AddToPlaylistRepository(
    private val articleDao: ArticleDao,
    private val playlistDao: PlaylistDao,
    private val playlistMediaDao: PlaylistMediaDao,
) : BaseRepository(), IAddToPlaylistRepository {
    override fun getCommonPlaylists(
        medias: List<MediaUrlWithArticleIdBean>
    ): Flow<List<String>> = playlistMediaDao.getCommonMediaPlaylistIdList(
        medias.map { it.url },
        medias.size
    ).flowOn(Dispatchers.IO)

    override fun insertPlaylistMedia(
        playlistId: String,
        url: String,
        articleId: String?
    ): Flow<Boolean> = flow {
        if (playlistDao.exists(playlistId) == 0 ||
            playlistMediaDao.exists(playlistId = playlistId, url = url) != 0
        ) {
            emit(false)
            return@flow
        }
        val orderPosition = playlistMediaDao.getMaxOrder(playlistId = playlistId) + ORDER_DELTA
        val realArticleId = articleId?.takeIf { articleDao.exists(it) > 0 }
        playlistMediaDao.insertPlaylistMedia(
            PlaylistMediaBean(
                playlistId = playlistId,
                url = url,
                articleId = realArticleId,
                orderPosition = orderPosition,
                createTime = System.currentTimeMillis(),
            )
        )
        emit(true)
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