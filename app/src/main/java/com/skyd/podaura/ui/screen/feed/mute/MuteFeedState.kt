package com.skyd.podaura.ui.screen.feed.mute

import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.ui.mvi.MviViewState

data class MuteFeedState(
    val listState: ListState,
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = MuteFeedState(
            listState = ListState.Init,
            loadingDialog = false,
        )
    }

    sealed interface ListState {
        data class Success(val dataList: List<FeedBean>) : ListState
        data object Init : ListState
        data class Failed(val msg: String) : ListState
    }
}