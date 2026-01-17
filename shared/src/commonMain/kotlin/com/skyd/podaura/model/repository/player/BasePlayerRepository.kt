package com.skyd.podaura.model.repository.player

import com.skyd.podaura.model.bean.history.MediaPlayHistoryBean
import com.skyd.podaura.model.bean.playlist.PlaylistMediaBean
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.model.bean.playlist.updateLocalMediaMetadata
import com.skyd.podaura.model.db.dao.ArticleDao
import com.skyd.podaura.model.db.dao.EnclosureDao
import com.skyd.podaura.model.db.dao.MediaPlayHistoryDao
import com.skyd.podaura.model.repository.BaseRepository
import com.skyd.podaura.ui.player.jumper.PlayDataMode
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

abstract class BasePlayerRepository(
    private val mediaPlayHistoryDao: MediaPlayHistoryDao,
    private val articleDao: ArticleDao,
    private val enclosureDao: EnclosureDao,
) : BaseRepository(), IPlayerRepository {
    override fun insertPlayHistory(path: String, duration: Long, articleId: String?): Flow<Unit> =
        flow {
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

    override fun updateLastPlayPosition(path: String, lastPlayPosition: Long): Flow<Unit> = flow {
        mediaPlayHistoryDao.updateLastPlayPosition(path, lastPlayPosition)
        emit(Unit)
    }.flowOn(Dispatchers.IO)

    override fun requestLastPlayPosition(path: String): Flow<Long> = flow {
        emit(mediaPlayHistoryDao.getMediaPlayHistory(path)?.lastPlayPosition ?: 0L)
    }.flowOn(Dispatchers.IO)

    suspend fun requestPlaylistByArticleId(
        articleId: String,
        reverse: Boolean = true,
    ): List<PlaylistMediaWithArticleBean> {
        return articleDao.getArticlesForPlaylist(articleId).flatMap { articleWithFeed ->
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
        }.run { if (reverse) reversed() else this }
    }

    suspend fun requestPlaylistByMediaLibraryList(
        files: List<PlayDataMode.MediaLibraryList.PlayMediaListItem>,
    ): List<PlaylistMediaWithArticleBean> {
        val articleMap =
            articleDao.getArticleWithFeedListByIds(files.mapNotNull { it.articleId })
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

    abstract fun requestPlaylistByPlatformFile(file: PlatformFile): List<PlaylistMediaWithArticleBean>?
}
