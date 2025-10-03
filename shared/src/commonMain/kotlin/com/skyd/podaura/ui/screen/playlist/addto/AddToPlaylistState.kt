package com.skyd.podaura.ui.screen.playlist.addto

import androidx.paging.PagingData
import com.skyd.mvi.MviViewState
import com.skyd.podaura.model.bean.playlist.PlaylistViewBean
import kotlinx.coroutines.flow.Flow

data class AddToPlaylistState(
    val addedPlaylists: List<String>,
    val playlistState: PlaylistState,
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = AddToPlaylistState(
            addedPlaylists = listOf(),
            playlistState = PlaylistState.Init,
            loadingDialog = false,
        )
    }
}

sealed interface PlaylistState {
    data class Success(
        val playlistPagingDataFlow: Flow<PagingData<PlaylistViewBean>>,
    ) : PlaylistState

    data object Init : PlaylistState
    data class Failed(val msg: String) : PlaylistState
}