package com.skyd.podaura.ui.screen.history.search

import androidx.paging.PagingData
import com.skyd.mvi.MviViewState
import com.skyd.podaura.model.bean.history.MediaPlayHistoryWithArticle
import com.skyd.podaura.model.bean.history.ReadHistoryWithArticle
import kotlinx.coroutines.flow.Flow

data class HistorySearchState(
    val readHistorySearchResultState: ReadHistorySearchResultState,
    val mediaPlayHistorySearchResultState: MediaPlayHistorySearchResultState,
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = HistorySearchState(
            readHistorySearchResultState = ReadHistorySearchResultState.Init,
            mediaPlayHistorySearchResultState = MediaPlayHistorySearchResultState.Init,
            loadingDialog = false,
        )
    }
}

sealed interface ReadHistorySearchResultState {
    data class Success(val result: Flow<PagingData<ReadHistoryWithArticle>>) :
        ReadHistorySearchResultState

    data object Init : ReadHistorySearchResultState
    data object Loading : ReadHistorySearchResultState
    data class Failed(val msg: String) : ReadHistorySearchResultState
}

sealed interface MediaPlayHistorySearchResultState {
    data class Success(val result: Flow<PagingData<MediaPlayHistoryWithArticle>>) :
        MediaPlayHistorySearchResultState

    data object Init : MediaPlayHistorySearchResultState
    data object Loading : MediaPlayHistorySearchResultState
    data class Failed(val msg: String) : MediaPlayHistorySearchResultState
}