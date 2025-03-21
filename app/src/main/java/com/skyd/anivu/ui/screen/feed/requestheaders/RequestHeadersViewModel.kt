package com.skyd.anivu.ui.screen.feed.requestheaders

import com.skyd.anivu.base.mvi.AbstractMviViewModel
import com.skyd.anivu.ext.catchMap
import com.skyd.anivu.ext.startWith
import com.skyd.anivu.model.repository.feed.RequestHeadersRepository
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
class RequestHeadersViewModel @Inject constructor(
    private val requestHeadersRepo: RequestHeadersRepository,
) : AbstractMviViewModel<RequestHeadersIntent, RequestHeadersState, RequestHeadersEvent>() {

    override val viewState: StateFlow<RequestHeadersState>

    init {
        val initialVS = RequestHeadersState.initial()

        viewState = merge(
            intentFlow.filterIsInstance<RequestHeadersIntent.Init>().take(1),
            intentFlow.filterNot { it is RequestHeadersIntent.Init }
        )
            .toRequestHeadersPartialStateChangeFlow()
            .debugLog("RequestHeadersPartialStateChange")
            .sendSingleEvent()
            .scan(initialVS) { vs, change -> change.reduce(vs) }
            .debugLog("ViewState")
            .toState(initialVS)
    }

    private fun Flow<RequestHeadersPartialStateChange>.sendSingleEvent(): Flow<RequestHeadersPartialStateChange> {
        return onEach { change ->
            val event = when (change) {
                is RequestHeadersPartialStateChange.Header.Failed ->
                    RequestHeadersEvent.HeadersResultEvent.Failed(change.msg)

                is RequestHeadersPartialStateChange.UpdateHeaders.Failed ->
                    RequestHeadersEvent.UpdateHeadersResultEvent.Failed(change.msg)

                else -> return@onEach
            }
            sendEvent(event)
        }
    }

    private fun Flow<RequestHeadersIntent>.toRequestHeadersPartialStateChangeFlow(): Flow<RequestHeadersPartialStateChange> {
        return merge(
            filterIsInstance<RequestHeadersIntent.Init>().flatMapConcat { intent ->
                requestHeadersRepo.getFeedHeaders(intent.feedUrl).map {
                    RequestHeadersPartialStateChange.Header.Success(headers = it)
                }.startWith(RequestHeadersPartialStateChange.LoadingDialog.Show)
                    .catchMap { RequestHeadersPartialStateChange.Header.Failed(it.message.orEmpty()) }
            },
            filterIsInstance<RequestHeadersIntent.UpdateHeaders>().flatMapConcat { intent ->
                requestHeadersRepo.updateFeedHeaders(intent.feedUrl, intent.headers).map {
                    RequestHeadersPartialStateChange.UpdateHeaders.Success
                }.startWith(RequestHeadersPartialStateChange.LoadingDialog.Show)
                    .catchMap { RequestHeadersPartialStateChange.UpdateHeaders.Failed(it.message.orEmpty()) }
            },
        )
    }
}