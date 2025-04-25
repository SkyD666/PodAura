package com.skyd.anivu.ui.screen.settings.rssconfig.updatenotification

import com.skyd.anivu.ui.mvi.MviViewState
import com.skyd.anivu.model.bean.ArticleNotificationRuleBean

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
    data class Success(val rules: List<ArticleNotificationRuleBean>) : RuleListState
    data object Init : RuleListState
    data class Failed(val msg: String) : RuleListState
}