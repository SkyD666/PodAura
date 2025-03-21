package com.skyd.anivu.ui.screen.settings.data


internal sealed interface DataPartialStateChange {
    fun reduce(oldState: DataState): DataState

    sealed interface LoadingDialog : DataPartialStateChange {
        data object Show : LoadingDialog {
            override fun reduce(oldState: DataState) = oldState.copy(loadingDialog = true)
        }
    }

    data object Init : DataPartialStateChange {
        override fun reduce(oldState: DataState) = oldState
    }

    sealed interface ClearCacheResult : DataPartialStateChange {
        override fun reduce(oldState: DataState): DataState {
            return when (this) {
                is Success,
                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data class Success(val deletedSize: Long) : ClearCacheResult
        data class Failed(val msg: String) : ClearCacheResult
    }

    sealed interface DeletePlayHistoryResult : DataPartialStateChange {
        override fun reduce(oldState: DataState): DataState {
            return when (this) {
                is Success,
                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data class Success(val count: Int) : DeletePlayHistoryResult
        data class Failed(val msg: String) : DeletePlayHistoryResult
    }

    sealed interface DeleteArticleBeforeResult : DataPartialStateChange {
        override fun reduce(oldState: DataState): DataState {
            return when (this) {
                is Success,
                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data class Success(val count: Int) : DeleteArticleBeforeResult
        data class Failed(val msg: String) : DeleteArticleBeforeResult
    }
}
