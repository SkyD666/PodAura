package com.skyd.podaura.ui.screen.feed.sheet

import com.skyd.mvi.MviSingleEvent
import com.skyd.podaura.model.bean.feed.FeedViewBean

sealed interface FeedSheetEvent : MviSingleEvent {
    sealed interface EditFeedResultEvent : FeedSheetEvent {
        data class Success(val feed: FeedViewBean) : EditFeedResultEvent
        data class Failed(val msg: String) : EditFeedResultEvent
    }

    sealed interface RemoveFeedResultEvent : FeedSheetEvent {
        data class Failed(val msg: String) : RemoveFeedResultEvent
    }

    sealed interface ClearFeedArticlesResultEvent : FeedSheetEvent {
        data class Success(val feed: FeedViewBean) : ClearFeedArticlesResultEvent
        data class Failed(val msg: String) : ClearFeedArticlesResultEvent
    }

    sealed interface RefreshFeedResultEvent : FeedSheetEvent {
        data class Success(val feeds: List<FeedViewBean>) : RefreshFeedResultEvent
        data class Failed(val msg: String) : RefreshFeedResultEvent
    }

    sealed interface CreateGroupResultEvent : FeedSheetEvent {
        data class Failed(val msg: String) : CreateGroupResultEvent
    }

    sealed interface MoveFeedsToGroupResultEvent : FeedSheetEvent {
        data class Failed(val msg: String) : MoveFeedsToGroupResultEvent
    }

    sealed interface ReadAllResultEvent : FeedSheetEvent {
        data class Success(val feeds: List<FeedViewBean>) : ReadAllResultEvent
        data class Failed(val msg: String) : ReadAllResultEvent
    }

    sealed interface MuteFeedResultEvent : FeedSheetEvent {
        data class Success(val mute: Boolean) : MuteFeedResultEvent
        data class Failed(val msg: String) : MuteFeedResultEvent
    }
}