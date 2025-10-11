package com.skyd.podaura.ui.screen.settings.data

import com.skyd.mvi.AbstractMviViewModel
import com.skyd.podaura.ext.catchMap
import com.skyd.podaura.ext.fileSize
import com.skyd.podaura.ext.startWith
import com.skyd.podaura.model.repository.DataRepository
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
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.data_screen_data_cleared_size
import podaura.shared.generated.resources.data_screen_deleted_count

class DataViewModel(
    private val dataRepo: DataRepository
) : AbstractMviViewModel<DataIntent, DataState, DataEvent>() {

    override val viewState: StateFlow<DataState>

    init {
        val initialVS = DataState.initial()

        viewState = merge(
            intentFlow.filterIsInstance<DataIntent.Init>().take(1),
            intentFlow.filterNot { it is DataIntent.Init }
        )
            .toDataPartialStateChangeFlow()
            .debugLog("DataPartialStateChange")
            .sendSingleEvent()
            .scan(initialVS) { vs, change -> change.reduce(vs) }
            .debugLog("ViewState")
            .toState(initialVS)
    }

    private fun Flow<DataPartialStateChange>.sendSingleEvent(): Flow<DataPartialStateChange> {
        return onEach { change ->
            val event = when (change) {
                is DataPartialStateChange.ClearCacheResult.Success -> {
                    DataEvent.ClearCacheResultEvent.Success(
                        getString(
                            Res.string.data_screen_data_cleared_size,
                            change.deletedSize.fileSize(),
                        )
                    )
                }

                is DataPartialStateChange.ClearCacheResult.Failed -> {
                    DataEvent.ClearCacheResultEvent.Failed(change.msg)
                }

                is DataPartialStateChange.DeletePlayHistoryResult.Success -> {
                    DataEvent.DeletePlayHistoryResultEvent.Success(change.count)
                }

                is DataPartialStateChange.DeletePlayHistoryResult.Failed -> {
                    DataEvent.DeletePlayHistoryResultEvent.Failed(change.msg)
                }

                is DataPartialStateChange.DeleteArticleBeforeResult.Success -> {
                    DataEvent.DeleteArticleBeforeResultEvent.Success(
                        getPluralString(
                            Res.plurals.data_screen_deleted_count,
                            change.count,
                            change.count,
                        )
                    )
                }

                is DataPartialStateChange.DeleteArticleBeforeResult.Failed -> {
                    DataEvent.DeleteArticleBeforeResultEvent.Failed(change.msg)
                }

                else -> return@onEach
            }
            sendEvent(event)
        }
    }

    private fun Flow<DataIntent>.toDataPartialStateChangeFlow(): Flow<DataPartialStateChange> {
        return merge(
            filterIsInstance<DataIntent.Init>().map { DataPartialStateChange.Init },

            filterIsInstance<DataIntent.ClearCache>().flatMapConcat {
                dataRepo.requestClearCache().map {
                    DataPartialStateChange.ClearCacheResult.Success(deletedSize = it)
                }.startWith(DataPartialStateChange.LoadingDialog.Show)
                    .catchMap { DataPartialStateChange.ClearCacheResult.Failed(it.message.toString()) }
            },
            filterIsInstance<DataIntent.DeletePlayHistory>().flatMapConcat {
                dataRepo.requestDeletePlayHistory().map {
                    DataPartialStateChange.DeletePlayHistoryResult.Success(count = it)
                }.startWith(DataPartialStateChange.LoadingDialog.Show)
                    .catchMap { DataPartialStateChange.DeletePlayHistoryResult.Failed(it.message.toString()) }
            },
            filterIsInstance<DataIntent.DeleteArticleBefore>().flatMapConcat { intent ->
                dataRepo.requestDeleteArticleBefore(intent.timestamp).map {
                    DataPartialStateChange.DeleteArticleBeforeResult.Success(count = it)
                }.startWith(DataPartialStateChange.LoadingDialog.Show)
                    .catchMap { DataPartialStateChange.DeleteArticleBeforeResult.Failed(it.message.toString()) }
            },
        )
    }
}