package com.skyd.podaura.model.repository.playlist

import com.skyd.podaura.model.bean.playlist.MediaUrlWithArticleIdBean
import kotlinx.coroutines.flow.Flow

interface IAddToPlaylistRepository {
    fun getCommonPlaylists(
        medias: List<MediaUrlWithArticleIdBean>
    ): Flow<List<String>>

    fun insertPlaylistMedia(playlistId: String, url: String, articleId: String?): Flow<Boolean>

    fun insertPlaylistMedias(
        toPlaylistId: String,
        medias: List<MediaUrlWithArticleIdBean>
    ): Flow<Unit>

    fun removeMediaFromPlaylist(
        playlistId: String,
        mediaList: List<MediaUrlWithArticleIdBean>,
    ): Flow<Int>
}