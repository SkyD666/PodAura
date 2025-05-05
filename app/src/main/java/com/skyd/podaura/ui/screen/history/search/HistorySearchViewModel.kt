package com.skyd.podaura.ui.screen.history.search

import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.skyd.podaura.ext.catchMap
import com.skyd.podaura.ext.startWith
import com.skyd.podaura.model.repository.HistoryRepository
import com.skyd.podaura.ui.mvi.AbstractMviViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import org.koin.android.annotation.KoinViewModel

@KoinViewModel(binds = [])
class HistorySearchViewModel(
    private val historyRepo: HistoryRepository
) : AbstractMviViewModel<HistorySearchIntent, HistorySearchState, HistorySearchEvent>() {

    override val viewState: StateFlow<HistorySearchState>

    init {
        val initialVS = HistorySearchState.Companion.initial()

        viewState = merge(
            intentFlow.filterIsInstance<HistorySearchIntent.Query>().distinctUntilChanged(),
            intentFlow.filterNot { it is HistorySearchIntent.Query }
        )
            .toSearchPartialStateChangeFlow()
            .debugLog("HistorySearchPartialStateChange")
            .sendSingleEvent()
            .scan(initialVS) { vs, change -> change.reduce(vs) }
            .debugLog("ViewState")
            .toState(initialVS)
    }

    private fun Flow<HistorySearchPartialStateChange>.sendSingleEvent(): Flow<HistorySearchPartialStateChange> {
        return onEach { change ->
            val event = when (change) {
                is HistorySearchPartialStateChange.DeleteReadHistory.Failed ->
                    HistorySearchEvent.DeleteReadHistoryResultEvent.Failed(change.msg)

                is HistorySearchPartialStateChange.DeleteMediaPlayHistory.Failed ->
                    HistorySearchEvent.DeleteMediaPlayHistoryResultEvent.Failed(change.msg)

                else -> return@onEach
            }
            sendEvent(event)
        }
    }

    private fun Flow<HistorySearchIntent>.toSearchPartialStateChangeFlow(): Flow<HistorySearchPartialStateChange> {
        return merge(
            filterIsInstance<HistorySearchIntent.Query>().debounce(70).flatMapLatest { intent ->
                combine(
                    historyRepo.searchReadHistoryList(intent.query)
                        .map { it.flow.cachedIn(viewModelScope) },
                    historyRepo.searchMediaPlayHistoryList(intent.query)
                        .map { it.flow.cachedIn(viewModelScope) },
                ) { readHistoryList, mediaPlayHistoryList ->
                    HistorySearchPartialStateChange.SearchResult.Success(
                        readHistorySearchResult = readHistoryList,
                        mediaPlayHistorySearchResult = mediaPlayHistoryList,
                    )
                }.startWith(HistorySearchPartialStateChange.SearchResult.Loading)
                    .catchMap { HistorySearchPartialStateChange.SearchResult.Failed(it.message.toString()) }
            },
            filterIsInstance<HistorySearchIntent.DeleteReadHistory>().flatMapConcat { intent ->
                historyRepo.deleteReadHistory(intent.articleId).map {
                    HistorySearchPartialStateChange.DeleteReadHistory.Success
                }.startWith(HistorySearchPartialStateChange.LoadingDialog.Show)
                    .catchMap { HistorySearchPartialStateChange.DeleteReadHistory.Failed(it.message.toString()) }
            },
            filterIsInstance<HistorySearchIntent.DeleteMediaPlayHistory>().flatMapConcat { intent ->
                historyRepo.deleteMediaPlayHistory(intent.path).map {
                    HistorySearchPartialStateChange.DeleteMediaPlayHistory.Success
                }.startWith(HistorySearchPartialStateChange.LoadingDialog.Show)
                    .catchMap { HistorySearchPartialStateChange.DeleteMediaPlayHistory.Failed(it.message.toString()) }
            },
        )
    }
}