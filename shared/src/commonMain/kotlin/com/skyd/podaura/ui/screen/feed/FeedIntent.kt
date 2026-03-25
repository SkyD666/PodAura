package com.skyd.podaura.ui.screen.feed

import com.skyd.mvi.MviIntent
import com.skyd.podaura.model.bean.group.GroupVo

sealed interface FeedIntent : MviIntent {
    data object Init : FeedIntent
    data class AddFeed(
        val url: String,
        val nickname: String? = null,
        val group: GroupVo = GroupVo.DefaultGroup
    ) : FeedIntent

    data class OnEditFeedDialog(val feedUrl: String?) : FeedIntent
    data class OnEditGroupDialog(val group: GroupVo?) : FeedIntent
    data class ReadAllInGroup(val groupId: String?) : FeedIntent
    data class RefreshGroupFeed(val groupId: String?, val full: Boolean) : FeedIntent
    data class CreateGroup(val group: GroupVo) : FeedIntent
    data class ChangeGroupExpanded(val group: GroupVo, val expanded: Boolean) : FeedIntent
    data class ClearGroupArticles(val groupId: String) : FeedIntent
    data class DeleteGroup(val groupId: String) : FeedIntent
    data class RenameGroup(val groupId: String, val name: String) : FeedIntent
    data class MoveFeedsToGroup(val fromGroupId: String, val toGroupId: String) : FeedIntent
    data class MuteFeedsInGroup(val groupId: String, val mute: Boolean) : FeedIntent
    data class CollapseAllGroup(val collapse: Boolean) : FeedIntent
}