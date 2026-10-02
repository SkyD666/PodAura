package com.skyd.podaura.ui.screen.feed

import androidx.paging.PagingData
import com.skyd.mvi.MviViewState
import com.skyd.podaura.model.bean.group.GroupVo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

data class FeedState(
    val allGroupCollapsed: Boolean,
    val groups: Flow<PagingData<GroupVo>>,
    val listState: ListState,
    val editFeedUrl: String?,
    val editGroupDialogBean: GroupVo?,
    val loadingDialog: Boolean,
    val refreshAllFeedsInProgress: Boolean,
    val addFeedState: AddFeedState? = null,
    val refreshFeedState: RefreshFeedState? = null,
) : MviViewState {
    companion object {
        fun initial() = FeedState(
            allGroupCollapsed = false,
            groups = flowOf(PagingData.empty()),
            listState = ListState.Init,
            editFeedUrl = null,
            editGroupDialogBean = null,
            loadingDialog = false,
            refreshAllFeedsInProgress = false,
        )
    }
}

enum class AddFeedState { Loading, Saving }

enum class RefreshFeedState { Loading, Cancelling }

sealed interface ListState {
    data class Success(val dataPagingDataFlow: Flow<PagingData<Any>>) : ListState
    data object Init : ListState
    data object Loading : ListState
    data class Failed(val msg: String) : ListState
}
