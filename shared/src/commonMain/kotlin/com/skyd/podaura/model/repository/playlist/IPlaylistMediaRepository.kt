package com.skyd.podaura.model.repository.playlist

import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import kotlinx.coroutines.flow.Flow

interface IPlaylistMediaRepository {
    fun requestPlaylistMediaList(playlistId: String): Flow<List<PlaylistMediaWithArticleBean>>
}