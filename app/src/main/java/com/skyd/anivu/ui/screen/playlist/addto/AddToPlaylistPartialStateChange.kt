package com.skyd.anivu.ui.screen.playlist.addto

import androidx.paging.PagingData
import com.skyd.anivu.model.bean.playlist.PlaylistViewBean
import kotlinx.coroutines.flow.Flow


internal sealed interface AddToPlaylistPartialStateChange {
    fun reduce(oldState: AddToPlaylistState): AddToPlaylistState

    sealed interface LoadingDialog : AddToPlaylistPartialStateChange {
        data object Show : LoadingDialog {
            override fun reduce(oldState: AddToPlaylistState) =
                oldState.copy(loadingDialog = true)
        }
    }

    sealed interface Playlist : AddToPlaylistPartialStateChange {
        override fun reduce(oldState: AddToPlaylistState): AddToPlaylistState {
            return when (this) {
                is Success -> oldState.copy(
                    addedPlaylists = addedPlaylists,
                    playlistState = PlaylistState.Success(
                        playlistPagingDataFlow = playlistPagingDataFlow,
                    ),
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    playlistState = PlaylistState.Failed(msg = msg),
                    loadingDialog = false,
                )

                Loading -> oldState
            }
        }

        data class Success(
            val addedPlaylists: List<String>,
            val playlistPagingDataFlow: Flow<PagingData<PlaylistViewBean>>
        ) : Playlist

        data class Failed(val msg: String) : Playlist
        data object Loading : Playlist
    }

    sealed interface AddToPlaylist : AddToPlaylistPartialStateChange {
        override fun reduce(oldState: AddToPlaylistState): AddToPlaylistState {
            return when (this) {
                is Success -> oldState.copy(loadingDialog = false)
                is Failed -> oldState.copy(loadingDialog = false)
            }
        }

        data object Success : AddToPlaylist
        data class Failed(val msg: String) : AddToPlaylist
    }

    sealed interface RemoveFromPlaylist : AddToPlaylistPartialStateChange {
        override fun reduce(oldState: AddToPlaylistState): AddToPlaylistState {
            return when (this) {
                is Success -> oldState.copy(loadingDialog = false)
                is Failed -> oldState.copy(loadingDialog = false)
            }
        }

        data object Success : RemoveFromPlaylist
        data class Failed(val msg: String) : RemoveFromPlaylist
    }
}