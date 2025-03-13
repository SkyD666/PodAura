package com.skyd.anivu.ui.screen.feed

import androidx.paging.PagingData
import com.skyd.anivu.base.mvi.MviViewState
import com.skyd.anivu.model.bean.group.GroupVo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

data class FeedState(
    val allGroupCollapsed: Boolean,
    val groups: Flow<PagingData<GroupVo>>,
    val listState: ListState,
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = FeedState(
            allGroupCollapsed = false,
            groups = flowOf(PagingData.empty()),
            listState = ListState.Init,
            loadingDialog = false,
        )
    }
}

sealed interface ListState {
    data class Success(val dataPagingDataFlow: Flow<PagingData<Any>>) : ListState
    data object Init : ListState
    data object Loading : ListState
    data class Failed(val msg: String) : ListState
}