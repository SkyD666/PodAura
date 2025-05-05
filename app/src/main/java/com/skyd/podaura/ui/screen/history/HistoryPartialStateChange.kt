package com.skyd.podaura.ui.screen.history

import androidx.paging.PagingData
import com.skyd.podaura.model.bean.history.MediaPlayHistoryWithArticle
import com.skyd.podaura.model.bean.history.ReadHistoryWithArticle
import kotlinx.coroutines.flow.Flow


internal sealed interface HistoryPartialStateChange {
    fun reduce(oldState: HistoryState): HistoryState

    data object LoadingDialog : HistoryPartialStateChange {
        override fun reduce(oldState: HistoryState) =
            oldState.copy(loadingDialog = true)
    }

    sealed interface HistoryListResult : HistoryPartialStateChange {
        override fun reduce(oldState: HistoryState): HistoryState {
            return when (this) {
                is Success -> oldState.copy(
                    historyListState = HistoryListState.Success(
                        readHistoryList = readHistoryList,
                        mediaPlayHistoryList = mediaPlayHistoryList,
                    ),
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    historyListState = HistoryListState.Failed(msg = msg),
                    loadingDialog = false,
                )

                Loading -> oldState.copy(
                    historyListState = HistoryListState.Loading,
                    loadingDialog = false,
                )
            }
        }

        data class Success(
            val readHistoryList: Flow<PagingData<ReadHistoryWithArticle>>,
            val mediaPlayHistoryList: Flow<PagingData<MediaPlayHistoryWithArticle>>,
        ) : HistoryListResult

        data class Failed(val msg: String) : HistoryListResult
        data object Loading : HistoryListResult
    }

    sealed interface DeleteReadHistory : HistoryPartialStateChange {
        override fun reduce(oldState: HistoryState): HistoryState {
            return when (this) {
                is Success,
                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data object Success : DeleteReadHistory
        data class Failed(val msg: String) : DeleteReadHistory
    }

    sealed interface DeleteMediaPlayHistory : HistoryPartialStateChange {
        override fun reduce(oldState: HistoryState): HistoryState {
            return when (this) {
                is Success,
                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data object Success : DeleteMediaPlayHistory
        data class Failed(val msg: String) : DeleteMediaPlayHistory
    }
}
