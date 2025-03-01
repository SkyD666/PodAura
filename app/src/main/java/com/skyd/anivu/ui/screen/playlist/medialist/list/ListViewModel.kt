package com.skyd.anivu.ui.screen.playlist.medialist.list

import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import androidx.paging.filter
import com.skyd.anivu.base.mvi.AbstractMviViewModel
import com.skyd.anivu.ext.catchMap
import com.skyd.anivu.ext.startWith
import com.skyd.anivu.model.repository.playlist.PlaylistMediaRepository
import com.skyd.anivu.model.repository.playlist.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
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
import javax.inject.Inject

@HiltViewModel
class ListViewModel @Inject constructor(
    private val playlistRepo: PlaylistRepository,
    private val playlistMediaRepo: PlaylistMediaRepository,
) : AbstractMviViewModel<ListIntent, ListState, ListEvent>() {

    override val viewState: StateFlow<ListState>

    init {
        val initialVS = ListState.initial()

        viewState = merge(
            intentFlow.filterIsInstance<ListIntent.Init>().distinctUntilChanged(),
            intentFlow.filterNot { it is ListIntent.Init }
        )
            .toListPartialStateChangeFlow()
            .debugLog("ListPartialStateChange")
            .sendSingleEvent()
            .scan(initialVS) { vs, change -> change.reduce(vs) }
            .debugLog("ViewState")
            .toState(initialVS)
    }

    private fun Flow<ListPartialStateChange>.sendSingleEvent(): Flow<ListPartialStateChange> {
        return onEach { change ->
            val event = when (change) {
                is ListPartialStateChange.AddToPlaylist.Success ->
                    ListEvent.AddToPlaylistResultEvent.Success

                is ListPartialStateChange.AddToPlaylist.Failed ->
                    ListEvent.AddToPlaylistResultEvent.Failed(change.msg)

                else -> return@onEach
            }
            sendEvent(event)
        }
    }

    private fun Flow<ListIntent>.toListPartialStateChangeFlow(): Flow<ListPartialStateChange> {
        return merge(
            filterIsInstance<ListIntent.Init>().flatMapConcat { intent ->
                flowOf(playlistRepo.requestPlaylistList().map { pagingData ->
                    pagingData.filter { it.playlist.playlistId != intent.currentPlaylistId }
                }.cachedIn(viewModelScope)).map {
                    ListPartialStateChange.Playlist.Success(playlistPagingDataFlow = it)
                }.startWith(ListPartialStateChange.Playlist.Loading).catchMap {
                    ListPartialStateChange.Playlist.Failed(it.message.toString())
                }
            },
            filterIsInstance<ListIntent.AddSelected>().flatMapConcat { intent ->
                flowOf(ListPartialStateChange.AddSelected(intent.playlistMedia))
                    .startWith(ListPartialStateChange.LoadingDialog.Show)
            },
            filterIsInstance<ListIntent.RemoveSelected>().flatMapConcat { intent ->
                flowOf(ListPartialStateChange.RemoveSelected(intent.playlistMedia))
                    .startWith(ListPartialStateChange.LoadingDialog.Show)
            },
            filterIsInstance<ListIntent.ClearSelected>().flatMapConcat {
                flowOf(ListPartialStateChange.ClearSelected)
                    .startWith(ListPartialStateChange.LoadingDialog.Show)
            },
            filterIsInstance<ListIntent.RefreshAddedPlaylist>().flatMapLatest { intent ->
                playlistMediaRepo.getCommonPlaylists(intent.medias).map {
                    ListPartialStateChange.RefreshAddedPlaylist.Success(it)
                }.startWith(ListPartialStateChange.LoadingDialog.Show).catchMap {
                    ListPartialStateChange.RefreshAddedPlaylist.Failed(it.message.toString())
                }
            },
            filterIsInstance<ListIntent.AddToPlaylist>().flatMapConcat { intent ->
                playlistMediaRepo.insertPlaylistMedias(
                    intent.playlist.playlist.playlistId,
                    intent.medias,
                ).map {
                    ListPartialStateChange.AddToPlaylist.Success
                }.startWith(ListPartialStateChange.LoadingDialog.Show).catchMap {
                    ListPartialStateChange.AddToPlaylist.Failed(it.message.toString())
                }
            },
            filterIsInstance<ListIntent.RemoveFromPlaylist>().flatMapConcat { intent ->
                playlistMediaRepo.removeMediaFromPlaylist(
                    intent.playlist.playlist.playlistId,
                    intent.medias,
                ).map {
                    ListPartialStateChange.RemoveFromPlaylist.Success
                }.startWith(ListPartialStateChange.LoadingDialog.Show).catchMap {
                    ListPartialStateChange.RemoveFromPlaylist.Failed(it.message.toString())
                }
            },
        )
    }
}