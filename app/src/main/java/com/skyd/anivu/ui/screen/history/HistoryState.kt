package com.skyd.anivu.ui.screen.history

import androidx.paging.PagingData
import com.skyd.anivu.base.mvi.MviViewState
import com.skyd.anivu.model.bean.history.MediaPlayHistoryWithArticle
import com.skyd.anivu.model.bean.history.ReadHistoryWithArticle
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