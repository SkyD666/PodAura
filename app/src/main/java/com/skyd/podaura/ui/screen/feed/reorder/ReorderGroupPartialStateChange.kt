package com.skyd.podaura.ui.screen.feed.reorder

import androidx.paging.PagingData
import com.skyd.podaura.model.bean.group.GroupVo
import kotlinx.coroutines.flow.Flow


internal sealed interface ReorderGroupPartialStateChange {
    fun reduce(oldState: ReorderGroupState): ReorderGroupState

    sealed interface LoadingDialog : ReorderGroupPartialStateChange {
        data object Show : LoadingDialog {
            override fun reduce(oldState: ReorderGroupState) = oldState.copy(loadingDialog = true)
        }
    }

    sealed interface Reorder : ReorderGroupPartialStateChange {
        override fun reduce(oldState: ReorderGroupState): ReorderGroupState {
            return when (this) {
                is Failed -> oldState.copy(loadingDialog = false)
                is Success -> oldState.copy(loadingDialog = false)
            }
        }

        data object Success : Reorder
        data class Failed(val msg: String) : Reorder
    }

    sealed interface GroupList : ReorderGroupPartialStateChange {
        override fun reduce(oldState: ReorderGroupState): ReorderGroupState {
            return when (this) {
                is Success -> oldState.copy(
                    groupListState = GroupListState.Success(pagingDataFlow = pagingDataFlow),
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    groupListState = GroupListState.Failed(msg = msg),
                    loadingDialog = false,
                )
            }
        }

        data class Success(val pagingDataFlow: Flow<PagingData<GroupVo>>) : GroupList
        data class Failed(val msg: String) : GroupList
    }
}
