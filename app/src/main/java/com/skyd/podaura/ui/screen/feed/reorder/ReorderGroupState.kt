package com.skyd.podaura.ui.screen.feed.reorder

import androidx.paging.PagingData
import com.skyd.podaura.model.bean.group.GroupVo
import com.skyd.podaura.ui.mvi.MviViewState
import kotlinx.coroutines.flow.Flow

data class ReorderGroupState(
    val groupListState: GroupListState,
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = ReorderGroupState(
            groupListState = GroupListState.Init,
            loadingDialog = false,
        )
    }
}

sealed interface GroupListState {
    data class Success(val pagingDataFlow: Flow<PagingData<GroupVo>>) : GroupListState
    data object Init : GroupListState
    data class Failed(val msg: String) : GroupListState
}