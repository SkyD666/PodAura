package com.skyd.podaura.ui.screen.playlist

import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.skyd.mvi.AbstractMviViewModel
import com.skyd.podaura.ext.catchMap
import com.skyd.podaura.ext.startWith
import com.skyd.podaura.model.repository.playlist.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan

class PlaylistViewModel(
    private val playlistRepo: PlaylistRepository,
) : AbstractMviViewModel<PlaylistIntent, PlaylistState, PlaylistEvent>() {

    override val viewState: StateFlow<PlaylistState>

    init {
        val initialVS = PlaylistState.initial()

        viewState = merge(
            intentFlow.filterIsInstance<PlaylistIntent.Init>().distinctUntilChanged(),
            intentFlow.filterNot { it is PlaylistIntent.Init }
        )
            .toPlaylistPartialStateChangeFlow()
            .debugLog("PlaylistPartialStateChange")
            .sendSingleEvent()
            .scan(initialVS) { vs, change -> change.reduce(vs) }
            .debugLog("ViewState")
            .toState(initialVS)
    }

    private fun Flow<PlaylistPartialStateChange>.sendSingleEvent(): Flow<PlaylistPartialStateChange> {
        return onEach { change ->
            val event = when (change) {
                is PlaylistPartialStateChange.CreatePlaylist.Failed -> {
                    PlaylistEvent.CreateResultEvent.Failed(change.msg)
                }

                is PlaylistPartialStateChange.DeletePlaylist.Failed -> {
                    PlaylistEvent.DeleteResultEvent.Failed(change.msg)
                }

                is PlaylistPartialStateChange.RenamePlaylist.Failed -> {
                    PlaylistEvent.RenameResultEvent.Failed(change.msg)
                }

                else -> return@onEach
            }
            sendEvent(event)
        }
    }

    private fun Flow<PlaylistIntent>.toPlaylistPartialStateChangeFlow(): Flow<PlaylistPartialStateChange> {
        return merge(
            filterIsInstance<PlaylistIntent.Init>().flatMapConcat {
                flowOf(playlistRepo.requestPlaylistList().cachedIn(viewModelScope)).map {
                    PlaylistPartialStateChange.PlayList.Success(playlistPagingDataFlow = it)
                }.startWith(PlaylistPartialStateChange.PlayList.Loading).catchMap {
                    PlaylistPartialStateChange.PlayList.Failed(it.message.toString())
                }
            },
            filterIsInstance<PlaylistIntent.CreatePlaylist>().flatMapConcat { intent ->
                playlistRepo.createPlaylist(intent.name).map {
                    PlaylistPartialStateChange.CreatePlaylist.Success
                }.startWith(PlaylistPartialStateChange.LoadingDialog.Show).catchMap {
                    PlaylistPartialStateChange.CreatePlaylist.Failed(it.message.toString())
                }
            },
            filterIsInstance<PlaylistIntent.Delete>().flatMapConcat { intent ->
                playlistRepo.deletePlaylist(intent.playlistId).map {
                    PlaylistPartialStateChange.DeletePlaylist.Success
                }.startWith(PlaylistPartialStateChange.LoadingDialog.Show).catchMap {
                    PlaylistPartialStateChange.DeletePlaylist.Failed(it.message.toString())
                }
            },
            filterIsInstance<PlaylistIntent.Rename>().flatMapConcat { intent ->
                playlistRepo.renamePlaylist(intent.playlistId, intent.newName).map {
                    PlaylistPartialStateChange.RenamePlaylist.Success
                }.startWith(PlaylistPartialStateChange.LoadingDialog.Show).catchMap {
                    PlaylistPartialStateChange.RenamePlaylist.Failed(it.message.toString())
                }
            },
            filterIsInstance<PlaylistIntent.Reorder>().flatMapConcat { intent ->
                playlistRepo.reorderPlaylist(intent.fromIndex, intent.toIndex).map { count ->
                    if (count == 0) {
                        PlaylistPartialStateChange.Reorder.Failed("No media moved")
                    } else {
                        PlaylistPartialStateChange.Reorder.Success
                    }
                }.catchMap {
                    PlaylistPartialStateChange.Reorder.Failed(it.message.toString())
                }
            },
        )
    }
}