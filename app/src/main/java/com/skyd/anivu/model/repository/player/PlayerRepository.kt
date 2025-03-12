package com.skyd.anivu.model.repository.player

import android.net.Uri
import com.skyd.anivu.appContext
import com.skyd.anivu.base.BaseRepository
import com.skyd.anivu.model.bean.history.MediaPlayHistoryBean
import com.skyd.anivu.model.bean.playlist.PlaylistMediaBean
import com.skyd.anivu.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.anivu.model.db.dao.ArticleDao
import com.skyd.anivu.model.db.dao.EnclosureDao
import com.skyd.anivu.model.db.dao.MediaPlayHistoryDao
import com.skyd.anivu.ui.mpv.resolveUri
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
    fun insertPlayHistory(path: String, duration: Long, articleId: String?): Flow<Unit> = flow {
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

    fun updateLastPlayPosition(path: String, lastPlayPosition: Long): Flow<Unit> = flow {
        mediaPlayHistoryDao.updateLastPlayPosition(path, lastPlayPosition)
        emit(Unit)
    }.flowOn(Dispatchers.IO)

    fun requestLastPlayPosition(path: String): Flow<Long> = flow {
        emit(mediaPlayHistoryDao.getMediaPlayHistory(path)?.lastPlayPosition ?: 0L)
    }.flowOn(Dispatchers.IO)

    fun requestPlaylistByArticleId(articleId: String): List<PlaylistMediaWithArticleBean> =
        articleDao.getArticlesForPlaylist(articleId).map { articleWithFeed ->
            val enclosures = articleWithFeed.articleWithEnclosure.enclosures
            enclosures.mapIndexed { index, enclosure ->
                PlaylistMediaWithArticleBean(
                    playlistMediaBean = PlaylistMediaBean(
                        playlistId = "",
                        url = enclosure.url,
                        articleId = articleWithFeed.articleWithEnclosure.article.articleId,
                        orderPosition = index.toDouble(),
                        createTime = System.currentTimeMillis(),
                    ),
                    article = articleWithFeed,
                )
            }
        }.flatten()

    suspend fun requestPlaylistByMediaLibraryList(
        files: List<PlayDataMode.MediaLibraryList.PlayMediaListItem>,
    ): List<PlaylistMediaWithArticleBean> {
        val articleMap =
            articleDao.getArticleListByIds(files.mapNotNull { it.articleId })
                .associateBy { it.articleWithEnclosure.article.articleId }
        return files.mapIndexed { index, playMediaListItem ->
            PlaylistMediaWithArticleBean(
                playlistMediaBean = PlaylistMediaBean(
                    playlistId = "",
                    url = playMediaListItem.path,
                    articleId = articleMap[playMediaListItem.articleId]?.articleWithEnclosure?.article?.articleId,
                    orderPosition = index.toDouble(),
                    createTime = System.currentTimeMillis(),
                ).apply { updateLocalMediaMetadata() },
                article = articleMap[playMediaListItem.articleId],
            )
        }
    }

    fun requestPlaylistByUri(externalUri: Uri): List<PlaylistMediaWithArticleBean>? =
        externalUri.resolveUri(appContext)?.let { path ->
            listOf(
                PlaylistMediaWithArticleBean.fromUrl(
                    playlistId = "",
                    url = path,
                    orderPosition = 1.0,
                )
            )
        }
}