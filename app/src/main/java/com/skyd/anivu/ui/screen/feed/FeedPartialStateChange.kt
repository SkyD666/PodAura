package com.skyd.anivu.ui.screen.feed

import com.skyd.anivu.model.bean.feed.FeedViewBean
import com.skyd.anivu.model.bean.group.GroupVo


internal sealed interface FeedPartialStateChange {
    fun reduce(oldState: FeedState): FeedState

    sealed interface LoadingDialog : FeedPartialStateChange {
        data object Show : LoadingDialog {
            override fun reduce(oldState: FeedState) = oldState.copy(loadingDialog = true)
        }
    }

    sealed interface AddFeed : FeedPartialStateChange {
        override fun reduce(oldState: FeedState): FeedState {
            return when (this) {
                is Success -> oldState.copy(
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data class Success(val feed: FeedViewBean) : AddFeed
        data class Failed(val msg: String) : AddFeed
    }

    sealed interface EditFeed : FeedPartialStateChange {
        override fun reduce(oldState: FeedState): FeedState {
            return when (this) {
                is Success -> oldState.copy(
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data class Success(val feed: FeedViewBean) : EditFeed
        data class Failed(val msg: String) : EditFeed
    }

    sealed interface ClearFeedArticles : FeedPartialStateChange {
        override fun reduce(oldState: FeedState): FeedState {
            return when (this) {
                is Success -> oldState.copy(
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data class Success(val feed: FeedViewBean) : ClearFeedArticles
        data class Failed(val msg: String) : ClearFeedArticles
    }

    sealed interface RemoveFeed : FeedPartialStateChange {
        override fun reduce(oldState: FeedState): FeedState {
            return when (this) {
                is Success -> oldState.copy(
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data object Success : RemoveFeed
        data class Failed(val msg: String) : RemoveFeed
    }

    sealed interface ReadAll : FeedPartialStateChange {
        override fun reduce(oldState: FeedState): FeedState {
            return when (this) {
                is Success -> oldState.copy(
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data class Success(val feeds: List<FeedViewBean>) : ReadAll
        data class Failed(val msg: String) : ReadAll
    }

    sealed interface RefreshFeed : FeedPartialStateChange {
        override fun reduce(oldState: FeedState): FeedState {
            return when (this) {
                is Success -> oldState.copy(
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data class Success(val feeds: List<FeedViewBean>) : RefreshFeed
        data class Failed(val msg: String) : RefreshFeed
    }

    sealed interface CreateGroup : FeedPartialStateChange {
        override fun reduce(oldState: FeedState): FeedState {
            return when (this) {
                is Success -> oldState.copy(
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data object Success : CreateGroup
        data class Failed(val msg: String) : CreateGroup
    }

    sealed interface GroupExpandedChanged : FeedPartialStateChange {
        override fun reduce(oldState: FeedState): FeedState = oldState

        data object Success : GroupExpandedChanged
        data class Failed(val msg: String) : GroupExpandedChanged
    }

    sealed interface ClearGroupArticles : FeedPartialStateChange {
        override fun reduce(oldState: FeedState): FeedState {
            return when (this) {
                is Success -> oldState.copy(
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data object Success : ClearGroupArticles
        data class Failed(val msg: String) : ClearGroupArticles
    }

    sealed interface DeleteGroup : FeedPartialStateChange {
        override fun reduce(oldState: FeedState): FeedState {
            return when (this) {
                is Success -> oldState.copy(
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data object Success : DeleteGroup
        data class Failed(val msg: String) : DeleteGroup
    }

    sealed interface MoveFeedsToGroup : FeedPartialStateChange {
        override fun reduce(oldState: FeedState): FeedState {
            return when (this) {
                is Success -> oldState.copy(
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data object Success : MoveFeedsToGroup
        data class Failed(val msg: String) : MoveFeedsToGroup
    }

    sealed interface EditGroup : FeedPartialStateChange {
        override fun reduce(oldState: FeedState): FeedState {
            return when (this) {
                is Success -> oldState.copy(
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data class Success(val group: GroupVo) : EditGroup
        data class Failed(val msg: String) : EditGroup
    }

    sealed interface FeedList : FeedPartialStateChange {
        override fun reduce(oldState: FeedState): FeedState {
            return when (this) {
                is Success -> oldState.copy(
                    groupListState = GroupListState.Success(dataList = dataList),
                )

                is Failed -> oldState.copy(
                    groupListState = GroupListState.Failed(msg = msg),
                )

                Loading -> oldState.copy(
                    groupListState = GroupListState.Loading,
                )
            }
        }

        data class Success(val dataList: List<Any>) : FeedList
        data class Failed(val msg: String) : FeedList
        data object Loading : FeedList
    }

    sealed interface MuteFeed : FeedPartialStateChange {
        override fun reduce(oldState: FeedState): FeedState {
            return when (this) {
                is Success -> oldState.copy(
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data class Success(val mute: Boolean) : MuteFeed
        data class Failed(val msg: String) : MuteFeed
    }

    sealed interface MuteFeedsInGroup : FeedPartialStateChange {
        override fun reduce(oldState: FeedState): FeedState {
            return when (this) {
                is Success -> oldState.copy(
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data object Success : MuteFeedsInGroup
        data class Failed(val msg: String) : MuteFeedsInGroup
    }
}
