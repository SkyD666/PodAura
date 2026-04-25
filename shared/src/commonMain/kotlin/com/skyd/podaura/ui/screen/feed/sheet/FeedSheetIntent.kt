package com.skyd.podaura.ui.screen.feed.sheet

import com.skyd.mvi.MviIntent
import com.skyd.podaura.model.bean.group.GroupVo

sealed interface FeedSheetIntent : MviIntent {
    data class Init(val feedUrl: String) : FeedSheetIntent
    data class EditFeedUrl(val oldUrl: String, val newUrl: String) : FeedSheetIntent
    data class EditFeedNickname(val url: String, val nickname: String?) : FeedSheetIntent
    data class EditFeedGroup(val url: String, val groupId: String?) : FeedSheetIntent
    data class EditFeedCustomDescription(val url: String, val customDescription: String?) :
        FeedSheetIntent

    data class EditFeedCustomIcon(val url: String, val customIcon: String?) : FeedSheetIntent
    data class EditFeedSortXmlArticlesOnUpdate(val url: String, val sort: Boolean) : FeedSheetIntent

    data class ClearFeedArticles(val url: String) : FeedSheetIntent
    data class RemoveFeed(val url: String) : FeedSheetIntent
    data class ReadAllInFeed(val feedUrl: String) : FeedSheetIntent
    data class RefreshFeed(val url: String, val full: Boolean) : FeedSheetIntent
    data class CreateGroup(val group: GroupVo) : FeedSheetIntent
    data class MuteFeed(val feedUrl: String, val mute: Boolean) : FeedSheetIntent
}