package com.skyd.podaura.ui.screen.playlist.medialist.list

import com.skyd.podaura.ext.startWith
import com.skyd.podaura.ui.mvi.AbstractMviViewModel
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
import org.koin.android.annotation.KoinViewModel

@KoinViewModel(binds = [])
class ListViewModel : AbstractMviViewModel<ListIntent, ListState, ListEvent>() {

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
                else -> return@onEach
            }
            sendEvent(event)
        }
    }

    private fun Flow<ListIntent>.toListPartialStateChangeFlow(): Flow<ListPartialStateChange> {
        return merge(
            filterIsInstance<ListIntent.Init>().map { ListPartialStateChange.Init },
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
        )
    }
}