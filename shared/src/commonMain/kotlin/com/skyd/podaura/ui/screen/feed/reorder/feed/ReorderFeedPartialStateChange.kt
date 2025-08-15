package com.skyd.podaura.ui.screen.feed.reorder.feed

import androidx.paging.PagingData
import com.skyd.podaura.model.bean.feed.FeedViewBean
import kotlinx.coroutines.flow.Flow


internal sealed interface ReorderFeedPartialStateChange {
    fun reduce(oldState: ReorderFeedState): ReorderFeedState

    sealed interface LoadingDialog : ReorderFeedPartialStateChange {
        data object Show : LoadingDialog {
            override fun reduce(oldState: ReorderFeedState) = oldState.copy(loadingDialog = true)
        }
    }

    sealed interface Reorder : ReorderFeedPartialStateChange {
        override fun reduce(oldState: ReorderFeedState): ReorderFeedState {
            return when (this) {
                is Failed -> oldState.copy(loadingDialog = false)
                is Success -> oldState.copy(loadingDialog = false)
            }
        }

        data object Success : Reorder
        data class Failed(val msg: String) : Reorder
    }

    sealed interface FeedList : ReorderFeedPartialStateChange {
        override fun reduce(oldState: ReorderFeedState): ReorderFeedState {
            return when (this) {
                is Success -> oldState.copy(
                    feedListState = FeedListState.Success(pagingDataFlow = pagingDataFlow),
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    feedListState = FeedListState.Failed(msg = msg),
                    loadingDialog = false,
                )
            }
        }

        data class Success(val pagingDataFlow: Flow<PagingData<FeedViewBean>>) : FeedList
        data class Failed(val msg: String) : FeedList
    }
}
