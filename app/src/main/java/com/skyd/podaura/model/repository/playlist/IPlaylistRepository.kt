package com.skyd.podaura.model.repository.playlist

import androidx.paging.PagingData
import com.skyd.podaura.model.bean.playlist.PlaylistViewBean
import kotlinx.coroutines.flow.Flow

interface IPlaylistRepository {
    fun requestPlaylist(playlistId: String): Flow<PlaylistViewBean>
    fun requestPlaylistList(): Flow<PagingData<PlaylistViewBean>>
}