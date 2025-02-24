package com.skyd.anivu.model.repository.player

import android.graphics.Bitmap
import com.skyd.anivu.base.BaseRepository
import com.skyd.anivu.ext.isLocalFile
import com.skyd.anivu.model.db.dao.ArticleDao
import com.skyd.anivu.ui.mpv.component.playlist.PlaylistItemBean
import com.skyd.anivu.ui.mpv.getMediaMetadata
import com.skyd.anivu.ui.mpv.service.PlaylistBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class PlaylistRepository @Inject constructor(
    private val articleDao: ArticleDao,
) : BaseRepository() {
    fun requestPlaylistItemBean(
        playlist: List<PlaylistBean>,
    ): Flow<List<PlaylistItemBean>> = flow {
        val articleIds = playlist.mapNotNull { it.customMediaData.articleId }
        val articleMap = articleDao.getArticleListByIds(articleIds).associateBy {
            it.articleWithEnclosure.article.articleId
        }
        val localFiles = playlist.filter { it.path.isLocalFile() }.map { it.path }
        val metadata = getMediaMetadata(localFiles)
        val result = playlist.map { playlistBean ->
            val playlistBeanArticleId = playlistBean.customMediaData.articleId
            if (playlistBeanArticleId == null) {
                PlaylistItemBean(
                    playlistBean = playlistBean,
                    article = null,
                    _title = metadata[playlistBean.path]?.get("title") as? String,
                    thumbnailBitmap = metadata[playlistBean.path]?.get("albumArt") as? Bitmap,
                    _artist = metadata[playlistBean.path]?.get("artist") as? String,
                    _duration = metadata[playlistBean.path]?.get("duration") as? Long,
                )
            } else {
                PlaylistItemBean(
                    playlistBean = playlistBean,
                    article = articleMap[playlistBeanArticleId],
                )
            }
        }
        emit(result)
    }.flowOn(Dispatchers.IO)
}