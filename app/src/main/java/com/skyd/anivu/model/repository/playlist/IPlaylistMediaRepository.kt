package com.skyd.anivu.model.repository.playlist

import com.skyd.anivu.model.bean.playlist.PlaylistMediaWithArticleBean
import kotlinx.coroutines.flow.Flow

interface IPlaylistMediaRepository {
    fun requestPlaylistMediaList(playlistId: String): Flow<List<PlaylistMediaWithArticleBean>>

    fun getCommonPlaylists(medias: List<PlaylistMediaWithArticleBean>): Flow<List<String>>

    fun insertPlaylistMedias(
        toPlaylistId: String,
        medias: List<PlaylistMediaWithArticleBean>,
    ): Flow<Unit>

    fun removeMediaFromPlaylist(mediaList: List<PlaylistMediaWithArticleBean>): Flow<Int>
}