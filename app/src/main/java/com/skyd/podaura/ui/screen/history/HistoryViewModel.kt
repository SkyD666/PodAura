package com.skyd.podaura.ui.screen.history

import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.skyd.podaura.ext.catchMap
import com.skyd.podaura.ext.startWith
import com.skyd.podaura.model.repository.HistoryRepository
import com.skyd.podaura.ui.mvi.AbstractMviViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.take
import org.koin.android.annotation.KoinViewModel

@KoinViewModel(binds = [])
class HistoryViewModel(
    private val historyRepo: HistoryRepository
) : AbstractMviViewModel<HistoryIntent, HistoryState, HistoryEvent>() {

    override val viewState: StateFlow<HistoryState>

    init {
        val initialVS = HistoryState.initial()

        viewState = merge(
            intentFlow.filterIsInstance<HistoryIntent.Init>().take(1),
            intentFlow.filterNot { it is HistoryIntent.Init }
        )
            .toReadPartialStateChangeFlow()
            .debugLog("HistoryPartialStateChange")
            .sendSingleEvent()
            .scan(initialVS) { vs, change -> change.reduce(vs) }
            .debugLog("ViewState")
            .toState(initialVS)
    }

    private fun Flow<HistoryPartialStateChange>.sendSingleEvent(): Flow<HistoryPartialStateChange> {
        return onEach { change ->
            val event = when (change) {
                is HistoryPartialStateChange.DeleteReadHistory.Failed ->
                    HistoryEvent.DeleteReadHistory.Failed(change.msg)

                is HistoryPartialStateChange.DeleteMediaPlayHistory.Failed ->
                    HistoryEvent.DeleteMediaPlayHistory.Failed(change.msg)

                else -> return@onEach
            }
            sendEvent(event)
        }
    }

    private fun Flow<HistoryIntent>.toReadPartialStateChangeFlow(): Flow<HistoryPartialStateChange> {
        return merge(
            filterIsInstance<HistoryIntent.Init>().flatMapConcat {
                combine(
                    historyRepo.requestReadHistoryList().map {
                        it.flow.cachedIn(viewModelScope)
                    },
                    historyRepo.requestMediaPlayHistoryList().map {
                        it.flow.cachedIn(viewModelScope)
                    },
                ) { readHistoryList, mediaPlayHistoryList ->
                    HistoryPartialStateChange.HistoryListResult.Success(
                        readHistoryList = readHistoryList,
                        mediaPlayHistoryList = mediaPlayHistoryList,
                    )
                }.startWith(HistoryPartialStateChange.HistoryListResult.Loading)
                    .catchMap { HistoryPartialStateChange.HistoryListResult.Failed(it.message.toString()) }
            },
            filterIsInstance<HistoryIntent.DeleteReadHistory>().flatMapConcat { intent ->
                historyRepo.deleteReadHistory(articleId = intent.articleId).map {
                    HistoryPartialStateChange.DeleteReadHistory.Success
                }.startWith(HistoryPartialStateChange.LoadingDialog).catchMap {
                    HistoryPartialStateChange.DeleteReadHistory.Failed(it.message.orEmpty())
                }
            },
            filterIsInstance<HistoryIntent.DeleteMediaPlayHistory>().flatMapConcat { intent ->
                historyRepo.deleteMediaPlayHistory(path = intent.path).map {
                    HistoryPartialStateChange.DeleteMediaPlayHistory.Success
                }.startWith(HistoryPartialStateChange.LoadingDialog).catchMap {
                    HistoryPartialStateChange.DeleteMediaPlayHistory.Failed(it.message.orEmpty())
                }
            },
            filterIsInstance<HistoryIntent.DeleteAllReadHistory>().flatMapConcat {
                historyRepo.deleteAllReadHistory().map {
                    HistoryPartialStateChange.DeleteReadHistory.Success
                }.startWith(HistoryPartialStateChange.LoadingDialog).catchMap {
                    HistoryPartialStateChange.DeleteReadHistory.Failed(it.message.orEmpty())
                }
            },
            filterIsInstance<HistoryIntent.DeleteAllMediaPlayHistory>().flatMapConcat {
                historyRepo.deleteAllMediaPlayHistory().map {
                    HistoryPartialStateChange.DeleteMediaPlayHistory.Success
                }.startWith(HistoryPartialStateChange.LoadingDialog).catchMap {
                    HistoryPartialStateChange.DeleteMediaPlayHistory.Failed(it.message.orEmpty())
                }
            },
        )
    }
}