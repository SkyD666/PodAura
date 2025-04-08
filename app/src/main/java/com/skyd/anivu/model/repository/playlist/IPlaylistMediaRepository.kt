package com.skyd.anivu.model.repository.playlist

import com.skyd.anivu.model.bean.playlist.PlaylistMediaWithArticleBean
import kotlinx.coroutines.flow.Flow

interface IPlaylistMediaRepository {
    fun requestPlaylistMediaList(playlistId: String): Flow<List<PlaylistMediaWithArticleBean>>
}