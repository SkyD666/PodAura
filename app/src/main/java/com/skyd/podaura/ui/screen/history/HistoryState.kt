package com.skyd.podaura.ui.screen.history

import androidx.paging.PagingData
import com.skyd.podaura.model.bean.history.MediaPlayHistoryWithArticle
import com.skyd.podaura.model.bean.history.ReadHistoryWithArticle
import com.skyd.podaura.ui.mvi.MviViewState
import kotlinx.coroutines.flow.Flow

data class HistoryState(
    val historyListState: HistoryListState,
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = HistoryState(
            historyListState = HistoryListState.Init,
            loadingDialog = false,
        )
    }
}

sealed interface HistoryListState {
    data class Success(
        val readHistoryList: Flow<PagingData<ReadHistoryWithArticle>>,
        val mediaPlayHistoryList: Flow<PagingData<MediaPlayHistoryWithArticle>>,
    ) : HistoryListState

    data object Init : HistoryListState
    data object Loading : HistoryListState
    data class Failed(val msg: String) : HistoryListState
}