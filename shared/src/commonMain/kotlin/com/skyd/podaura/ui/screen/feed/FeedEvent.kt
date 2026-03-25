package com.skyd.podaura.ui.screen.feed

import com.skyd.mvi.MviSingleEvent
import com.skyd.podaura.model.bean.feed.FeedViewBean
import com.skyd.podaura.model.bean.group.GroupVo

sealed interface FeedEvent : MviSingleEvent {
    sealed interface InitFeetListResultEvent : FeedEvent {
        data class Failed(val msg: String) : InitFeetListResultEvent
    }

    sealed interface AddFeedResultEvent : FeedEvent {
        data class Success(val feed: FeedViewBean) : AddFeedResultEvent
        data class Failed(val msg: String) : AddFeedResultEvent
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

    sealed interface MuteFeedsInGroupResultEvent : FeedEvent {
        data class Failed(val msg: String) : MuteFeedsInGroupResultEvent
    }

    sealed interface CollapseAllGroupResultEvent : FeedEvent {
        data class Failed(val msg: String) : CollapseAllGroupResultEvent
    }
}