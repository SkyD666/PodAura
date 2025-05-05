package com.skyd.podaura.ui.screen.history.search

import androidx.paging.PagingData
import com.skyd.podaura.model.bean.history.MediaPlayHistoryWithArticle
import com.skyd.podaura.model.bean.history.ReadHistoryWithArticle
import kotlinx.coroutines.flow.Flow


internal sealed interface HistorySearchPartialStateChange {
    fun reduce(oldState: HistorySearchState): HistorySearchState

    sealed interface LoadingDialog : HistorySearchPartialStateChange {
        data object Show : LoadingDialog {
            override fun reduce(oldState: HistorySearchState) = oldState.copy(loadingDialog = true)
        }
    }

    sealed interface SearchResult : HistorySearchPartialStateChange {
        override fun reduce(oldState: HistorySearchState): HistorySearchState {
            return when (this) {
                is Success -> oldState.copy(
                    readHistorySearchResultState =
                        ReadHistorySearchResultState.Success(result = readHistorySearchResult),
                    mediaPlayHistorySearchResultState =
                        MediaPlayHistorySearchResultState.Success(result = mediaPlayHistorySearchResult),
                )

                is Failed -> oldState.copy(
                    readHistorySearchResultState = ReadHistorySearchResultState.Failed(msg),
                    mediaPlayHistorySearchResultState = MediaPlayHistorySearchResultState.Failed(msg),
                )

                Loading -> oldState.copy(
                    readHistorySearchResultState = ReadHistorySearchResultState.Loading,
                    mediaPlayHistorySearchResultState = MediaPlayHistorySearchResultState.Loading,
                )
            }
        }

        data class Success(
            val readHistorySearchResult: Flow<PagingData<ReadHistoryWithArticle>>,
            val mediaPlayHistorySearchResult: Flow<PagingData<MediaPlayHistoryWithArticle>>
        ) : SearchResult

        data class Failed(val msg: String) : SearchResult
        data object Loading : SearchResult
    }

    sealed interface DeleteReadHistory : HistorySearchPartialStateChange {
        override fun reduce(oldState: HistorySearchState): HistorySearchState {
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

    sealed interface DeleteMediaPlayHistory : HistorySearchPartialStateChange {
        override fun reduce(oldState: HistorySearchState): HistorySearchState {
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