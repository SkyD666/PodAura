package com.skyd.podaura.ui.screen.feed.mute

import com.skyd.podaura.model.bean.feed.FeedBean


internal sealed interface MuteFeedPartialStateChange {
    fun reduce(oldState: MuteFeedState): MuteFeedState

    sealed interface LoadingDialog : MuteFeedPartialStateChange {
        data object Show : LoadingDialog {
            override fun reduce(oldState: MuteFeedState) = oldState.copy(loadingDialog = true)
        }
    }

    sealed interface Init : MuteFeedPartialStateChange {
        override fun reduce(oldState: MuteFeedState): MuteFeedState {
            return when (this) {
                is Failed -> oldState.copy(loadingDialog = false)
                is Success -> oldState.copy(
                    listState = MuteFeedState.ListState.Success(feeds),
                    loadingDialog = false,
                )
            }
        }

        data class Success(val feeds: List<FeedBean>) : Init
        data class Failed(val msg: String) : Init
    }

    sealed interface Mute : MuteFeedPartialStateChange {
        override fun reduce(oldState: MuteFeedState): MuteFeedState {
            return when (this) {
                is Failed -> oldState.copy(loadingDialog = false)
                is Success -> oldState.copy(loadingDialog = false)
            }
        }

        data object Success : Mute
        data class Failed(val msg: String) : Mute
    }
}
