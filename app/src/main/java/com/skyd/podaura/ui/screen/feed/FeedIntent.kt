package com.skyd.podaura.ui.screen.feed

import com.skyd.podaura.model.bean.feed.FeedViewBean
import com.skyd.podaura.model.bean.group.GroupVo
import com.skyd.podaura.ui.mvi.MviIntent

sealed interface FeedIntent : MviIntent {
    data object Init : FeedIntent
    data class AddFeed(
        val url: String,
        val nickname: String? = null,
        val group: GroupVo = GroupVo.DefaultGroup
    ) : FeedIntent

    data class OnEditFeedDialog(val feed: FeedViewBean?) : FeedIntent
    data class OnEditGroupDialog(val group: GroupVo?) : FeedIntent

    data class EditFeedUrl(val oldUrl: String, val newUrl: String) : FeedIntent
    data class EditFeedNickname(val url: String, val nickname: String?) : FeedIntent
    data class EditFeedGroup(val url: String, val groupId: String?) : FeedIntent
    data class EditFeedCustomDescription(val url: String, val customDescription: String?) :
        FeedIntent

    data class EditFeedCustomIcon(val url: String, val customIcon: String?) : FeedIntent
    data class EditFeedSortXmlArticlesOnUpdate(val url: String, val sort: Boolean) : FeedIntent

    data class ClearFeedArticles(val url: String) : FeedIntent
    data class RemoveFeed(val url: String) : FeedIntent
    data class ReadAllInFeed(val feedUrl: String) : FeedIntent
    data class ReadAllInGroup(val groupId: String?) : FeedIntent
    data class RefreshFeed(val url: String, val full: Boolean) : FeedIntent
    data class RefreshGroupFeed(val groupId: String?, val full: Boolean) : FeedIntent
    data class CreateGroup(val group: GroupVo) : FeedIntent
    data class ChangeGroupExpanded(val group: GroupVo, val expanded: Boolean) : FeedIntent
    data class ClearGroupArticles(val groupId: String) : FeedIntent
    data class DeleteGroup(val groupId: String) : FeedIntent
    data class RenameGroup(val groupId: String, val name: String) : FeedIntent
    data class MoveFeedsToGroup(val fromGroupId: String, val toGroupId: String) : FeedIntent
    data class MuteFeedsInGroup(val groupId: String, val mute: Boolean) : FeedIntent
    data class MuteFeed(val feedUrl: String, val mute: Boolean) : FeedIntent
    data class CollapseAllGroup(val collapse: Boolean) : FeedIntent
}