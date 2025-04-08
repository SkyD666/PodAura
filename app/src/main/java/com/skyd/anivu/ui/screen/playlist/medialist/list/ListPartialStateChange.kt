package com.skyd.anivu.ui.screen.playlist.medialist.list

import com.skyd.anivu.model.bean.playlist.PlaylistMediaWithArticleBean


internal sealed interface ListPartialStateChange {
    fun reduce(oldState: ListState): ListState

    sealed interface LoadingDialog : ListPartialStateChange {
        data object Show : LoadingDialog {
            override fun reduce(oldState: ListState) =
                oldState.copy(loadingDialog = true)
        }
    }

    data object Init : ListPartialStateChange {
        override fun reduce(oldState: ListState) = oldState
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
}