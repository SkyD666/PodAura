package com.skyd.anivu.ui.screen.settings.data

import com.skyd.anivu.R
import com.skyd.anivu.appContext
import com.skyd.anivu.base.mvi.AbstractMviViewModel
import com.skyd.anivu.ext.catchMap
import com.skyd.anivu.ext.fileSize
import com.skyd.anivu.ext.startWith
import com.skyd.anivu.model.repository.DataRepository
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
class DataViewModel @Inject constructor(
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
                        appContext.getString(
                            R.string.data_screen_data_cleared_size,
                            change.deletedSize.fileSize(appContext),
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
                        appContext.resources.getQuantityString(
                            R.plurals.data_screen_deleted_count,
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