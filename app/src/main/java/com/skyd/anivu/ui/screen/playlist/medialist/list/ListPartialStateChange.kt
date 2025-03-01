package com.skyd.anivu.ui.screen.playlist.medialist.list

import androidx.paging.PagingData
import com.skyd.anivu.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.anivu.model.bean.playlist.PlaylistViewBean
import kotlinx.coroutines.flow.Flow


internal sealed interface ListPartialStateChange {
    fun reduce(oldState: ListState): ListState

    sealed interface LoadingDialog : ListPartialStateChange {
        data object Show : LoadingDialog {
            override fun reduce(oldState: ListState) =
                oldState.copy(loadingDialog = true)
        }
    }

    sealed interface Playlist : ListPartialStateChange {
        override fun reduce(oldState: ListState): ListState {
            return when (this) {
                is Success -> oldState.copy(
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
            val playlistPagingDataFlow: Flow<PagingData<PlaylistViewBean>>
        ) : Playlist

        data class Failed(val msg: String) : Playlist
        data object Loading : Playlist
    }

    data class AddSelected(val media: PlaylistMediaWithArticleBean) : ListPartialStateChange {
        override fun reduce(oldState: ListState) = oldState.copy(
            selectedItems = oldState.selectedItems + media,
            loadingDialog = false,
        )
    }

    data class RemoveSelected(val media: PlaylistMediaWithArticleBean) : ListPartialStateChange {
        override fun reduce(oldState: ListState) = oldState.copy(
            selectedItems = oldState.selectedItems - media,
            loadingDialog = false,
        )
    }

    data object ClearSelected : ListPartialStateChange {
        override fun reduce(oldState: ListState) = oldState.copy(
            selectedItems = emptyList(),
            loadingDialog = false,
        )
    }

    sealed interface RefreshAddedPlaylist : ListPartialStateChange {
        override fun reduce(oldState: ListState): ListState {
            return when (this) {
                is Success -> oldState.copy(
                    addedPlaylists = addedPlaylist,
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(loadingDialog = false)
            }
        }

        data class Success(val addedPlaylist: List<String>) : RefreshAddedPlaylist
        data class Failed(val msg: String) : RefreshAddedPlaylist
    }

    sealed interface AddToPlaylist : ListPartialStateChange {
        override fun reduce(oldState: ListState): ListState {
            return when (this) {
                is Success -> oldState.copy(loadingDialog = false)
                is Failed -> oldState.copy(loadingDialog = false)
            }
        }

        data object Success : AddToPlaylist
        data class Failed(val msg: String) : AddToPlaylist
    }

    sealed interface RemoveFromPlaylist : ListPartialStateChange {
        override fun reduce(oldState: ListState): ListState {
            return when (this) {
                is Success -> oldState.copy(loadingDialog = false)
                is Failed -> oldState.copy(loadingDialog = false)
            }
        }

        data object Success : RemoveFromPlaylist
        data class Failed(val msg: String) : RemoveFromPlaylist
    }
}