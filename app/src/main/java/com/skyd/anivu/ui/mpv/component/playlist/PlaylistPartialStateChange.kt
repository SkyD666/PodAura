package com.skyd.anivu.ui.mpv.component.playlist


internal sealed interface PlaylistPartialStateChange {
    fun reduce(oldState: PlaylistState): PlaylistState

    sealed interface PlayList : PlaylistPartialStateChange {
        override fun reduce(oldState: PlaylistState): PlaylistState {
            return when (this) {
                is Success -> oldState.copy(listState = ListState.Success(playlist))
                is Failed -> oldState.copy(listState = ListState.Failed(msg))
                Loading -> oldState.copy(listState = ListState.Loading)
            }
        }

        data class Success(val playlist: List<PlaylistItemBean>) : PlayList
        data class Failed(val msg: String) : PlayList
        data object Loading : PlayList
    }
}