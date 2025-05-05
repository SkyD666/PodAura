package com.skyd.podaura.ui.screen.playlist.addto

import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import androidx.paging.filter
import com.skyd.podaura.ext.catchMap
import com.skyd.podaura.ext.startWith
import com.skyd.podaura.model.repository.playlist.AddToPlaylistRepository
import com.skyd.podaura.model.repository.playlist.IPlaylistRepository
import com.skyd.podaura.ui.mvi.AbstractMviViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import org.koin.android.annotation.KoinViewModel

@KoinViewModel(binds = [])
class AddToPlaylistViewModel(
    private val playlistRepo: IPlaylistRepository,
    private val addToPlaylistRepo: AddToPlaylistRepository,
) : AbstractMviViewModel<AddToPlaylistIntent, AddToPlaylistState, AddToPlaylistEvent>() {

    override val viewState: StateFlow<AddToPlaylistState>

    init {
        val initialVS = AddToPlaylistState.Companion.initial()

        viewState = merge(
            intentFlow.filterIsInstance<AddToPlaylistIntent.Init>().distinctUntilChanged(),
            intentFlow.filterNot { it is AddToPlaylistIntent.Init }
        )
            .toListPartialStateChangeFlow()
            .debugLog("AddToPlaylistPartialStateChange")
            .sendSingleEvent()
            .scan(initialVS) { vs, change -> change.reduce(vs) }
            .debugLog("ViewState")
            .toState(initialVS)
    }

    private fun Flow<AddToPlaylistPartialStateChange>.sendSingleEvent(): Flow<AddToPlaylistPartialStateChange> {
        return onEach { change ->
            val event = when (change) {
                is AddToPlaylistPartialStateChange.AddToPlaylist.Success ->
                    AddToPlaylistEvent.AddToPlaylistResultEvent.Success

                is AddToPlaylistPartialStateChange.AddToPlaylist.Failed ->
                    AddToPlaylistEvent.AddToPlaylistResultEvent.Failed(change.msg)

                else -> return@onEach
            }
            sendEvent(event)
        }
    }

    private fun Flow<AddToPlaylistIntent>.toListPartialStateChangeFlow(): Flow<AddToPlaylistPartialStateChange> {
        return merge(
            filterIsInstance<AddToPlaylistIntent.Init>().flatMapLatest { intent ->
                combine(
                    flowOf(playlistRepo.requestPlaylistList().map { pagingData ->
                        pagingData.filter { it.playlist.playlistId != intent.currentPlaylistId }
                    }.cachedIn(viewModelScope)),
                    addToPlaylistRepo.getCommonPlaylists(intent.medias),
                ) { playlistPagingDataFlow, addedPlaylists ->
                    AddToPlaylistPartialStateChange.Playlist.Success(
                        playlistPagingDataFlow = playlistPagingDataFlow,
                        addedPlaylists = addedPlaylists,
                    )
                }.startWith(AddToPlaylistPartialStateChange.Playlist.Loading).catchMap {
                    AddToPlaylistPartialStateChange.Playlist.Failed(it.message.toString())
                }
            },
            filterIsInstance<AddToPlaylistIntent.AddTo>().flatMapConcat { intent ->
                addToPlaylistRepo.insertPlaylistMedias(
                    intent.playlist.playlist.playlistId,
                    intent.medias,
                ).map {
                    AddToPlaylistPartialStateChange.AddToPlaylist.Success
                }.startWith(AddToPlaylistPartialStateChange.LoadingDialog.Show).catchMap {
                    AddToPlaylistPartialStateChange.AddToPlaylist.Failed(it.message.toString())
                }
            },
            filterIsInstance<AddToPlaylistIntent.RemoveFromPlaylist>().flatMapConcat { intent ->
                addToPlaylistRepo.removeMediaFromPlaylist(
                    intent.playlist.playlist.playlistId,
                    intent.medias,
                ).map {
                    AddToPlaylistPartialStateChange.RemoveFromPlaylist.Success
                }.startWith(AddToPlaylistPartialStateChange.LoadingDialog.Show).catchMap {
                    AddToPlaylistPartialStateChange.RemoveFromPlaylist.Failed(it.message.toString())
                }
            },
        )
    }
}