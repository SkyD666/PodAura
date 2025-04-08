package com.skyd.anivu.ui.screen.playlist.medialist

import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.skyd.anivu.base.mvi.AbstractMviViewModel
import com.skyd.anivu.ext.catchMap
import com.skyd.anivu.ext.startWith
import com.skyd.anivu.model.repository.playlist.IAddToPlaylistRepository
import com.skyd.anivu.model.repository.playlist.IPlaylistRepository
import com.skyd.anivu.model.repository.playlist.PlaylistMediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import javax.inject.Inject

@HiltViewModel
class PlaylistMediaListViewModel @Inject constructor(
    private val playlistRepo: IPlaylistRepository,
    private val playlistMediaRepo: PlaylistMediaRepository,
    private val addToPlaylist: IAddToPlaylistRepository,
) : AbstractMviViewModel<PlaylistMediaListIntent, PlaylistMediaListState, PlaylistMediaListEvent>() {

    override val viewState: StateFlow<PlaylistMediaListState>

    init {
        val initialVS = PlaylistMediaListState.initial()

        viewState = merge(
            intentFlow.filterIsInstance<PlaylistMediaListIntent.Init>().distinctUntilChanged(),
            intentFlow.filterNot { it is PlaylistMediaListIntent.Init }
        )
            .toPlaylistMediaListPartialStateChangeFlow()
            .debugLog("PlaylistMediaListPartialStateChange")
            .sendSingleEvent()
            .scan(initialVS) { vs, change -> change.reduce(vs) }
            .debugLog("ViewState")
            .toState(initialVS)
    }

    private fun Flow<PlaylistMediaListPartialStateChange>.sendSingleEvent(): Flow<PlaylistMediaListPartialStateChange> {
        return onEach { change ->
            val event = when (change) {
                is PlaylistMediaListPartialStateChange.DeleteMedia.Failed ->
                    PlaylistMediaListEvent.DeleteResultEvent.Failed(change.msg)

                else -> return@onEach
            }
            sendEvent(event)
        }
    }

    private fun Flow<PlaylistMediaListIntent>.toPlaylistMediaListPartialStateChangeFlow(): Flow<PlaylistMediaListPartialStateChange> {
        return merge(
            filterIsInstance<PlaylistMediaListIntent.Init>().flatMapConcat { intent ->
                combine(
                    playlistRepo.requestPlaylist(intent.playlistId),
                    flowOf(
                        playlistMediaRepo.requestPlaylistMediaListPaging(intent.playlistId)
                            .cachedIn(viewModelScope)
                    ),
                ) { playlistViewBean, mediaList ->
                    PlaylistMediaListPartialStateChange.PlaylistMediaList.Success(
                        playlistViewBean = playlistViewBean,
                        playlistMediaPagingDataFlow = mediaList,
                    )
                }.startWith(PlaylistMediaListPartialStateChange.PlaylistMediaList.Loading)
                    .catchMap {
                        PlaylistMediaListPartialStateChange.PlaylistMediaList.Failed(it.message.toString())
                    }
            },
            filterIsInstance<PlaylistMediaListIntent.Delete>().flatMapConcat { intent ->
                addToPlaylist.removeMediaFromPlaylist(intent.playlistId, intent.deletes).map {
                    PlaylistMediaListPartialStateChange.DeleteMedia.Success
                }.startWith(PlaylistMediaListPartialStateChange.LoadingDialog.Show).catchMap {
                    PlaylistMediaListPartialStateChange.DeleteMedia.Failed(it.message.toString())
                }
            },
            filterIsInstance<PlaylistMediaListIntent.Reorder>().flatMapConcat { intent ->
                playlistMediaRepo.reorderPlaylistMedia(
                    playlistId = intent.playlistId,
                    fromIndex = intent.fromIndex,
                    toIndex = intent.toIndex,
                ).map { count ->
                    if (count == 0) {
                        PlaylistMediaListPartialStateChange.Reorder.Failed("No media moved")
                    } else {
                        PlaylistMediaListPartialStateChange.Reorder.Success
                    }
                }.catchMap {
                    PlaylistMediaListPartialStateChange.Reorder.Failed(it.message.toString())
                }
            },
        )
    }
}