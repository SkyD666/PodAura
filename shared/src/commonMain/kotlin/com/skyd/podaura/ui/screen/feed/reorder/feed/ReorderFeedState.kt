package com.skyd.podaura.ui.screen.feed.reorder.feed

import androidx.paging.PagingData
import com.skyd.mvi.MviViewState
import com.skyd.podaura.model.bean.feed.FeedViewBean
import kotlinx.coroutines.flow.Flow

data class ReorderFeedState(
    val feedListState: FeedListState,
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = ReorderFeedState(
            feedListState = FeedListState.Init,
            loadingDialog = false,
        )
    }
}

sealed interface FeedListState {
    data class Success(val pagingDataFlow: Flow<PagingData<FeedViewBean>>) : FeedListState
    data object Init : FeedListState
    data class Failed(val msg: String) : FeedListState
}