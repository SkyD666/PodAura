package com.skyd.podaura.ui.screen.feed

import com.skyd.podaura.model.bean.feed.FeedViewBean
import com.skyd.podaura.model.bean.group.GroupVo
import com.skyd.podaura.ui.mvi.MviSingleEvent

sealed interface FeedEvent : MviSingleEvent {
    sealed interface InitFeetListResultEvent : FeedEvent {
        data class Failed(val msg: String) : InitFeetListResultEvent
    }

    sealed interface AddFeedResultEvent : FeedEvent {
        data class Success(val feed: FeedViewBean) : AddFeedResultEvent
        data class Failed(val msg: String) : AddFeedResultEvent
    }

    sealed interface EditFeedResultEvent : FeedEvent {
        data class Success(val feed: FeedViewBean) : EditFeedResultEvent
        data class Failed(val msg: String) : EditFeedResultEvent
    }

    sealed interface RemoveFeedResultEvent : FeedEvent {
        data class Failed(val msg: String) : RemoveFeedResultEvent
    }

    sealed interface ClearFeedArticlesResultEvent : FeedEvent {
        data class Success(val feed: FeedViewBean) : ClearFeedArticlesResultEvent
        data class Failed(val msg: String) : ClearFeedArticlesResultEvent
    }

    sealed interface RefreshFeedResultEvent : FeedEvent {
        data class Success(val feeds: List<FeedViewBean>) : RefreshFeedResultEvent
        data class Failed(val msg: String) : RefreshFeedResultEvent
    }

    sealed interface CreateGroupResultEvent : FeedEvent {
        data class Failed(val msg: String) : CreateGroupResultEvent
    }

    sealed interface DeleteGroupResultEvent : FeedEvent {
        data class Failed(val msg: String) : DeleteGroupResultEvent
    }

    sealed interface ClearGroupArticlesResultEvent : FeedEvent {
        data class Failed(val msg: String) : ClearGroupArticlesResultEvent
    }

    sealed interface MoveFeedsToGroupResultEvent : FeedEvent {
        data class Failed(val msg: String) : MoveFeedsToGroupResultEvent
    }

    sealed interface EditGroupResultEvent : FeedEvent {
        data class Success(val group: GroupVo) : EditGroupResultEvent
        data class Failed(val msg: String) : EditGroupResultEvent
    }

    sealed interface ReadAllResultEvent : FeedEvent {
        data class Success(val feeds: List<FeedViewBean>) : ReadAllResultEvent
        data class Failed(val msg: String) : ReadAllResultEvent
    }

    sealed interface MuteFeedResultEvent : FeedEvent {
        data class Success(val mute: Boolean) : MuteFeedResultEvent
        data class Failed(val msg: String) : MuteFeedResultEvent
    }

    sealed interface MuteFeedsInGroupResultEvent : FeedEvent {
        data class Failed(val msg: String) : MuteFeedsInGroupResultEvent
    }

    sealed interface CollapseAllGroupResultEvent : FeedEvent {
        data class Failed(val msg: String) : CollapseAllGroupResultEvent
    }
}