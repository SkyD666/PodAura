package com.skyd.podaura.ui.screen.settings.rssconfig.updatenotification

import com.skyd.mvi.MviViewState
import com.skyd.podaura.model.bean.ArticleNotificationRuleBean
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.bean.group.GroupBean

data class UpdateNotificationState(
    val ruleListState: RuleListState,
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = UpdateNotificationState(
            ruleListState = RuleListState.Init,
            loadingDialog = false,
        )
    }
}

sealed interface RuleListState {
    data class Success(
        val rules: List<ArticleNotificationRuleBean>,
        val feeds: List<FeedBean>,
        val groups: List<GroupBean>,
    ) : RuleListState

    data object Init : RuleListState
    data class Failed(val msg: String) : RuleListState
}
