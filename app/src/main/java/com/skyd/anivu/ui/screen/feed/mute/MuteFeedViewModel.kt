package com.skyd.anivu.ui.screen.feed.mute

import com.skyd.anivu.base.mvi.AbstractMviViewModel
import com.skyd.anivu.ext.catchMap
import com.skyd.anivu.ext.startWith
import com.skyd.anivu.model.repository.feed.FeedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.take
import javax.inject.Inject

@HiltViewModel
class MuteFeedViewModel @Inject constructor(
    private val feedRepo: FeedRepository,
) : AbstractMviViewModel<MuteFeedIntent, MuteFeedState, MuteFeedEvent>() {

    override val viewState: StateFlow<MuteFeedState>

    init {
        val initialVS = MuteFeedState.initial()

        viewState = merge(
            intentFlow.filterIsInstance<MuteFeedIntent.Init>().take(1),
            intentFlow.filterNot { it is MuteFeedIntent.Init }
        )
            .toReorderGroupPartialStateChangeFlow()
            .debugLog("MuteFeedPartialStateChange")
            .sendSingleEvent()
            .scan(initialVS) { vs, change -> change.reduce(vs) }
            .debugLog("ViewState")
            .toState(initialVS)
    }

    private fun Flow<MuteFeedPartialStateChange>.sendSingleEvent(): Flow<MuteFeedPartialStateChange> {
        return onEach { change ->
            val event = when (change) {
                is MuteFeedPartialStateChange.Mute.Failed ->
                    MuteFeedEvent.MuteResultEvent.Failed(change.msg)

                else -> return@onEach
            }
            sendEvent(event)
        }
    }

    private fun Flow<MuteFeedIntent>.toReorderGroupPartialStateChangeFlow(): Flow<MuteFeedPartialStateChange> {
        return merge(
            filterIsInstance<MuteFeedIntent.Init>().flatMapConcat {
                feedRepo.requestAllFeedList().map {
                    MuteFeedPartialStateChange.Init.Success(feeds = it)
                }.startWith(MuteFeedPartialStateChange.LoadingDialog.Show)
                    .catchMap { MuteFeedPartialStateChange.Init.Failed(it.message.orEmpty()) }
            },
            filterIsInstance<MuteFeedIntent.Mute>().flatMapConcat { intent ->
                feedRepo.muteFeed(intent.feedUrl, intent.mute).map {
                    MuteFeedPartialStateChange.Mute.Success
                }.startWith(MuteFeedPartialStateChange.LoadingDialog.Show)
                    .catchMap { MuteFeedPartialStateChange.Mute.Failed(it.message.orEmpty()) }
            },
        )
    }
}