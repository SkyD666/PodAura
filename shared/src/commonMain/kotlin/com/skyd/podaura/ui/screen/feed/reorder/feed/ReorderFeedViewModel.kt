package com.skyd.podaura.ui.screen.feed.reorder.feed

import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.skyd.mvi.AbstractMviViewModel
import com.skyd.podaura.ext.catchMap
import com.skyd.podaura.ext.startWith
import com.skyd.podaura.model.repository.feed.ReorderFeedRepository
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

class ReorderFeedViewModel(
    private val reorderFeedRepo: ReorderFeedRepository,
) : AbstractMviViewModel<ReorderFeedIntent, ReorderFeedState, ReorderFeedEvent>() {

    override val viewState: StateFlow<ReorderFeedState>

    init {
        val initialVS = ReorderFeedState.initial()

        viewState = merge(
            intentFlow.filterIsInstance<ReorderFeedIntent.Init>().take(1),
            intentFlow.filterNot { it is ReorderFeedIntent.Init }
        )
            .toReorderFeedPartialStateChangeFlow()
            .debugLog("ReorderFeedPartialStateChange")
            .sendSingleEvent()
            .scan(initialVS) { vs, change -> change.reduce(vs) }
            .debugLog("ViewState")
            .toState(initialVS)
    }

    private fun Flow<ReorderFeedPartialStateChange>.sendSingleEvent(): Flow<ReorderFeedPartialStateChange> {
        return onEach { change ->
            val event = when (change) {
                is ReorderFeedPartialStateChange.Reorder.Failed ->
                    ReorderFeedEvent.ReorderResultEvent.Failed(change.msg)

                is ReorderFeedPartialStateChange.FeedList.Failed ->
                    ReorderFeedEvent.FeedListResultEvent.Failed(change.msg)

                else -> return@onEach
            }
            sendEvent(event)
        }
    }

    private fun Flow<ReorderFeedIntent>.toReorderFeedPartialStateChangeFlow(): Flow<ReorderFeedPartialStateChange> {
        return merge(
            filterIsInstance<ReorderFeedIntent.Init>().flatMapConcat { intent ->
                flowOf(
                    reorderFeedRepo.requestFeedList(intent.groupId)
                        .cachedIn(viewModelScope)
                ).map {
                    ReorderFeedPartialStateChange.FeedList.Success(pagingDataFlow = it)
                }.startWith(ReorderFeedPartialStateChange.LoadingDialog.Show)
                    .catchMap { ReorderFeedPartialStateChange.FeedList.Failed(it.message.orEmpty()) }
            },
            filterIsInstance<ReorderFeedIntent.Reorder>().flatMapConcat { intent ->
                reorderFeedRepo.reorderFeed(intent.groupId, intent.from, intent.to).map {
                    if (it > 0) ReorderFeedPartialStateChange.Reorder.Success
                    else ReorderFeedPartialStateChange.Reorder.Failed("Reorder error: $intent")
                }.catchMap { ReorderFeedPartialStateChange.Reorder.Failed(it.message.orEmpty()) }
            },
        )
    }
}