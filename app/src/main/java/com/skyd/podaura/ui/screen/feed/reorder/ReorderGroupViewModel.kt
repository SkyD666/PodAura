package com.skyd.podaura.ui.screen.feed.reorder

import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.skyd.podaura.ext.catchMap
import com.skyd.podaura.ext.startWith
import com.skyd.podaura.model.repository.feed.ReorderGroupRepository
import com.skyd.podaura.ui.mvi.AbstractMviViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.take

class ReorderGroupViewModel(
    private val reorderGroupRepo: ReorderGroupRepository,
) : AbstractMviViewModel<ReorderGroupIntent, ReorderGroupState, ReorderGroupEvent>() {

    override val viewState: StateFlow<ReorderGroupState>

    init {
        val initialVS = ReorderGroupState.initial()

        viewState = merge(
            intentFlow.filterIsInstance<ReorderGroupIntent.Init>().take(1),
            intentFlow.filterNot { it is ReorderGroupIntent.Init }
        )
            .toReorderGroupPartialStateChangeFlow()
            .debugLog("ReorderGroupPartialStateChange")
            .sendSingleEvent()
            .scan(initialVS) { vs, change -> change.reduce(vs) }
            .debugLog("ViewState")
            .toState(initialVS)
    }

    private fun Flow<ReorderGroupPartialStateChange>.sendSingleEvent(): Flow<ReorderGroupPartialStateChange> {
        return onEach { change ->
            val event = when (change) {
                is ReorderGroupPartialStateChange.Reorder.Failed ->
                    ReorderGroupEvent.ReorderResultEvent.Failed(change.msg)

                is ReorderGroupPartialStateChange.GroupList.Failed ->
                    ReorderGroupEvent.GroupListResultEvent.Failed(change.msg)

                else -> return@onEach
            }
            sendEvent(event)
        }
    }

    private fun Flow<ReorderGroupIntent>.toReorderGroupPartialStateChangeFlow(): Flow<ReorderGroupPartialStateChange> {
        return merge(
            filterIsInstance<ReorderGroupIntent.Init>().flatMapConcat {
                flowOf(reorderGroupRepo.requestGroupList().cachedIn(viewModelScope)).map {
                    ReorderGroupPartialStateChange.GroupList.Success(pagingDataFlow = it)
                }.startWith(ReorderGroupPartialStateChange.LoadingDialog.Show)
                    .catchMap { ReorderGroupPartialStateChange.GroupList.Failed(it.message.orEmpty()) }
            },
            filterIsInstance<ReorderGroupIntent.Reorder>().flatMapConcat { intent ->
                reorderGroupRepo.reorderGroup(intent.from, intent.to).map {
                    if (it > 0) ReorderGroupPartialStateChange.Reorder.Success
                    else ReorderGroupPartialStateChange.Reorder.Failed("Reorder error: $intent")
                }.catchMap { ReorderGroupPartialStateChange.Reorder.Failed(it.message.orEmpty()) }
            },
        )
    }
}