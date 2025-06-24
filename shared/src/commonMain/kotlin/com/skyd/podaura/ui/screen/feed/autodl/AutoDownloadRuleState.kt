package com.skyd.podaura.ui.screen.feed.autodl

import com.skyd.mvi.MviViewState
import com.skyd.podaura.model.bean.download.autorule.AutoDownloadRuleBean

data class AutoDownloadRuleState(
    val ruleState: RuleState,
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = AutoDownloadRuleState(
            ruleState = RuleState.Init,
            loadingDialog = false,
        )
    }

    sealed interface RuleState {
        data class Success(val autoDownloadRule: AutoDownloadRuleBean) : RuleState
        data object Init : RuleState
        data class Failed(val msg: String) : RuleState
    }
}