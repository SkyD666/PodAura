package com.skyd.podaura.ui.player

import androidx.lifecycle.coroutineScope
import com.skyd.fundation.di.get
import com.skyd.podaura.model.playlist.PlaylistController
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

/** Installed once by each platform's player session; survives hidden/background player UI. */
fun PlayerCoordinator.followPlaylistChanges(
    playlists: PlaylistController? = null,
): Job = lifecycle.coroutineScope.launch(Dispatchers.IO) {
    val controller by lazy { playlists ?: get<PlaylistController>() }
    val sourceChanges = playerState.map { it.playlistId to it.shuffle }
        .distinctUntilChanged().map { Unit }
    merge(sourceChanges, playlistLoads).collectLatest {
        val id = playerState.value.playlistId
        // A full reload of this source also needs its latest committed contents, even when
        // source/shuffle are unchanged. Leaving shuffle restores the saved order as before.
        if (id.isNotEmpty()) controller.followPlaylist(id, this@followPlaylistChanges)
    }
}
