package com.skyd.anivu.ui.mpv.component.playlist

import com.skyd.anivu.base.mvi.MviViewState

data class PlaylistState(
    val listState: ListState,
) : MviViewState {
    companion object {
        fun initial() = PlaylistState(
            listState = ListState.Init,
        )
    }
}

sealed interface ListState {
    data class Success(val playlist: List<PlaylistItemBean>) : ListState
    data object Init : ListState
    data object Loading : ListState
    data class Failed(val msg: String) : ListState
}