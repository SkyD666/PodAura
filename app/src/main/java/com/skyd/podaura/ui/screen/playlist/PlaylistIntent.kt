package com.skyd.podaura.ui.screen.playlist

import com.skyd.podaura.ui.mvi.MviIntent

sealed interface PlaylistIntent : MviIntent {
    data object Init : PlaylistIntent
    data class CreatePlaylist(val name: String) : PlaylistIntent
    data class Delete(val playlistId: String) : PlaylistIntent
    data class Rename(val playlistId: String, val newName: String) : PlaylistIntent
    data class Reorder(val fromIndex: Int, val toIndex: Int) : PlaylistIntent
}