package com.skyd.anivu.ui.mpv.component.playlist

import android.graphics.Bitmap
import com.skyd.anivu.model.bean.article.ArticleWithFeed
import com.skyd.anivu.ui.mpv.service.PlaylistBean

data class PlaylistItemBean(
    val playlistBean: PlaylistBean,
    private val article: ArticleWithFeed?,
    private val _title: String? = null,
    val thumbnailBitmap: Bitmap? = null,
    private val _artist: String? = null,
    private val _duration: Long? = null,
) {
    val path = playlistBean.path
    val title = _title ?: playlistBean.customMediaData.title ?: path.substringAfterLast("/")
    val thumbnail = playlistBean.customMediaData.thumbnail
    val artist = _artist ?: article?.articleWithEnclosure?.article?.author.orEmpty().ifEmpty {
        article?.feed?.title
    }
    val duration = (_duration ?: article?.articleWithEnclosure?.media?.duration)?.div(1000)
}