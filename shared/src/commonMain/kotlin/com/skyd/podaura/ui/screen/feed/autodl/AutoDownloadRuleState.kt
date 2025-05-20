package com.skyd.podaura.ui.screen.feed.autodl

import com.skyd.podaura.model.bean.download.autorule.AutoDownloadRuleBean
import com.skyd.podaura.ui.mvi.MviViewState

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