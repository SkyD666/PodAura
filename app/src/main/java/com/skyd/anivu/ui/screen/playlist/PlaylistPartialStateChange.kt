package com.skyd.anivu.ui.screen.playlist

import androidx.paging.PagingData
import com.skyd.anivu.model.bean.playlist.PlaylistViewBean
import kotlinx.coroutines.flow.Flow


internal sealed interface PlaylistPartialStateChange {
    fun reduce(oldState: PlaylistState): PlaylistState

    sealed interface LoadingDialog : PlaylistPartialStateChange {
        data object Show : LoadingDialog {
            override fun reduce(oldState: PlaylistState) = oldState.copy(loadingDialog = true)
        }
    }

    sealed interface PlayList : PlaylistPartialStateChange {
        override fun reduce(oldState: PlaylistState): PlaylistState {
            return when (this) {
                is Success -> oldState.copy(
                    listState = ListState.Success(playlistPagingDataFlow = playlistPagingDataFlow),
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    listState = ListState.Failed(msg = msg),
                    loadingDialog = false,
                )

                Loading -> oldState
            }
        }

        data class Success(val playlistPagingDataFlow: Flow<PagingData<PlaylistViewBean>>) :
            PlayList

        data class Failed(val msg: String) : PlayList
        data object Loading : PlayList
    }

    sealed interface CreatePlaylist : PlaylistPartialStateChange {
        override fun reduce(oldState: PlaylistState): PlaylistState {
            return when (this) {
                is Success,
                is Failed -> oldState.copy(loadingDialog = false)
            }
        }

        data object Success : CreatePlaylist
        data class Failed(val msg: String) : CreatePlaylist
    }

    sealed interface DeletePlaylist : PlaylistPartialStateChange {
        override fun reduce(oldState: PlaylistState): PlaylistState {
            return when (this) {
                is Success,
                is Failed -> oldState.copy(loadingDialog = false)
            }
        }

        data object Success : DeletePlaylist
        data class Failed(val msg: String) : DeletePlaylist
    }

    sealed interface RenamePlaylist : PlaylistPartialStateChange {
        override fun reduce(oldState: PlaylistState): PlaylistState {
            return when (this) {
                is Success,
                is Failed -> oldState.copy(loadingDialog = false)
            }
        }

        data object Success : RenamePlaylist
        data class Failed(val msg: String) : RenamePlaylist
    }

    sealed interface Reorder : PlaylistPartialStateChange {
        override fun reduce(oldState: PlaylistState): PlaylistState {
            return when (this) {
                is Success,
                is Failed -> oldState.copy(loadingDialog = false)
            }
        }

        data object Success : Reorder
        data class Failed(val msg: String) : Reorder
    }
}