package com.skyd.anivu.ui.mpv.component.playlist

import com.skyd.anivu.base.mvi.AbstractMviViewModel
import com.skyd.anivu.base.mvi.MviSingleEvent
import com.skyd.anivu.ext.catchMap
import com.skyd.anivu.ext.startWith
import com.skyd.anivu.model.repository.player.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistRepo: PlaylistRepository
) : AbstractMviViewModel<PlaylistIntent, PlaylistState, MviSingleEvent>() {

    override val viewState: StateFlow<PlaylistState>

    init {
        val initialVS = PlaylistState.initial()

        viewState = merge(
            intentFlow.filterIsInstance<PlaylistIntent.Init>().distinctUntilChanged(),
            intentFlow.filterNot { it is PlaylistIntent.Init }
        )
            .toPlaylistPartialStateChangeFlow()
            .debugLog("PlaylistPartialStateChange")
            .scan(initialVS) { vs, change -> change.reduce(vs) }
            .debugLog("ViewState")
            .toState(initialVS)
    }

    private fun Flow<PlaylistIntent>.toPlaylistPartialStateChangeFlow(): Flow<PlaylistPartialStateChange> {
        return merge(
            filterIsInstance<PlaylistIntent.Init>().flatMapConcat { intent ->
                playlistRepo.requestPlaylistItemBean(intent.playlist).map {
                    PlaylistPartialStateChange.PlayList.Success(it)
                }.startWith(PlaylistPartialStateChange.PlayList.Loading).catchMap {
                    PlaylistPartialStateChange.PlayList.Failed(it.message.orEmpty())
                }
            },
        )
    }
}