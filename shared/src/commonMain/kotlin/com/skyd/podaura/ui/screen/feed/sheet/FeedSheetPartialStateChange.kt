package com.skyd.podaura.ui.screen.feed.sheet

import androidx.paging.PagingData
import com.skyd.podaura.model.bean.feed.FeedViewBean
import com.skyd.podaura.model.bean.group.GroupVo
import kotlinx.coroutines.flow.Flow


internal sealed interface FeedSheetPartialStateChange {
    fun reduce(oldState: FeedSheetState): FeedSheetState

    sealed interface LoadingDialog : FeedSheetPartialStateChange {
        data object Show : LoadingDialog {
            override fun reduce(oldState: FeedSheetState) = oldState.copy(loadingDialog = true)
        }
    }

    sealed interface Init : FeedSheetPartialStateChange {
        override fun reduce(oldState: FeedSheetState): FeedSheetState {
            return when (this) {
                is Success -> oldState.copy(
                    editFeedDialogBean = editFeedDialogBean,
                    groups = groups,
                )

                is Failed, Loading -> oldState
            }
        }

        data class Success(
            val editFeedDialogBean: FeedViewBean?,
            val groups: Flow<PagingData<GroupVo>>,
        ) : Init

        data class Failed(val msg: String) : Init
        data object Loading : Init
    }

    sealed interface EditFeed : FeedSheetPartialStateChange {
        override fun reduce(oldState: FeedSheetState): FeedSheetState {
            return when (this) {
                is Success -> oldState.copy(
                    editFeedDialogBean = feed.takeIf { oldState.editFeedDialogBean != null },
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

    sealed interface ClearFeedArticles : FeedSheetPartialStateChange {
        override fun reduce(oldState: FeedSheetState): FeedSheetState {
            return when (this) {
                is Success -> oldState.copy(
                    editFeedDialogBean = feed.takeIf { oldState.editFeedDialogBean != null },
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

    sealed interface RemoveFeed : FeedSheetPartialStateChange {
        override fun reduce(oldState: FeedSheetState): FeedSheetState {
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

    sealed interface ReadAll : FeedSheetPartialStateChange {
        override fun reduce(oldState: FeedSheetState): FeedSheetState {
            return when (this) {
                is Success -> oldState.copy(
                    editFeedDialogBean = oldState.editFeedDialogBean?.let { editFeedDialogBean ->
                        feeds.firstOrNull { feed -> feed.feed.url == editFeedDialogBean.feed.url }
                    } ?: oldState.editFeedDialogBean,
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

    sealed interface RefreshFeed : FeedSheetPartialStateChange {
        override fun reduce(oldState: FeedSheetState): FeedSheetState {
            return when (this) {
                is Success -> oldState.copy(
                    editFeedDialogBean = oldState.editFeedDialogBean?.let { editFeedDialogBean ->
                        feeds.firstOrNull { feed -> feed.feed.url == editFeedDialogBean.feed.url }
                    },
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

    sealed interface CreateGroup : FeedSheetPartialStateChange {
        override fun reduce(oldState: FeedSheetState): FeedSheetState {
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

    sealed interface MoveFeedsToGroup : FeedSheetPartialStateChange {
        override fun reduce(oldState: FeedSheetState): FeedSheetState {
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

    sealed interface MuteFeed : FeedSheetPartialStateChange {
        override fun reduce(oldState: FeedSheetState): FeedSheetState {
            return when (this) {
                is Success -> oldState.copy(
                    editFeedDialogBean = oldState.editFeedDialogBean?.let { feedView ->
                        feedView.copy(feed = feedView.feed.copy(mute = mute))
                    },
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

    sealed interface MuteFeedsInGroup : FeedSheetPartialStateChange {
        override fun reduce(oldState: FeedSheetState): FeedSheetState {
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
