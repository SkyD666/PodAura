package com.skyd.podaura.ui.screen.playlist.medialist

import androidx.paging.PagingData
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.model.bean.playlist.PlaylistViewBean
import kotlinx.coroutines.flow.Flow


internal sealed interface PlaylistMediaListPartialStateChange {
    fun reduce(oldState: PlaylistMediaListState): PlaylistMediaListState

    sealed interface LoadingDialog : PlaylistMediaListPartialStateChange {
        data object Show : LoadingDialog {
            override fun reduce(oldState: PlaylistMediaListState) =
                oldState.copy(loadingDialog = true)
        }
    }

    sealed interface PlaylistMediaList : PlaylistMediaListPartialStateChange {
        override fun reduce(oldState: PlaylistMediaListState): PlaylistMediaListState {
            return when (this) {
                is Success -> oldState.copy(
                    listState = ListState.Success(
                        playlistViewBean = playlistViewBean,
                        playlistMediaPagingDataFlow = playlistMediaPagingDataFlow,
                    ),
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    listState = ListState.Failed(msg = msg),
                    loadingDialog = false,
                )

                Loading -> oldState
            }
        }

        data class Success(
            val playlistViewBean: PlaylistViewBean,
            val playlistMediaPagingDataFlow: Flow<PagingData<PlaylistMediaWithArticleBean>>
        ) : PlaylistMediaList

        data class Failed(val msg: String) : PlaylistMediaList
        data object Loading : PlaylistMediaList
    }

    sealed interface DeleteMedia : PlaylistMediaListPartialStateChange {
        override fun reduce(oldState: PlaylistMediaListState): PlaylistMediaListState {
            return when (this) {
                is Success,
                is Failed -> oldState.copy(loadingDialog = false)
            }
        }

        data object Success : DeleteMedia
        data class Failed(val msg: String) : DeleteMedia
    }

    sealed interface Reorder : PlaylistMediaListPartialStateChange {
        override fun reduce(oldState: PlaylistMediaListState): PlaylistMediaListState {
            return when (this) {
                is Success,
                is Failed -> oldState.copy(loadingDialog = false)
            }
        }

        data object Success : Reorder
        data class Failed(val msg: String) : Reorder
    }
}