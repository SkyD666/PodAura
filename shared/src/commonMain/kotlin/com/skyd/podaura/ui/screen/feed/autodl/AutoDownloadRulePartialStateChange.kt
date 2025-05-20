package com.skyd.podaura.ui.screen.feed.autodl

import com.skyd.podaura.model.bean.download.autorule.AutoDownloadRuleBean


internal sealed interface AutoDownloadRulePartialStateChange {
    fun reduce(oldState: AutoDownloadRuleState): AutoDownloadRuleState

    sealed interface LoadingDialog : AutoDownloadRulePartialStateChange {
        data object Show : LoadingDialog {
            override fun reduce(oldState: AutoDownloadRuleState) =
                oldState.copy(loadingDialog = true)
        }
    }

    sealed interface Init : AutoDownloadRulePartialStateChange {
        override fun reduce(oldState: AutoDownloadRuleState): AutoDownloadRuleState {
            return when (this) {
                is Failed -> oldState.copy(loadingDialog = false)
                is Success -> oldState.copy(
                    ruleState = AutoDownloadRuleState.RuleState.Success(autoDownloadRule),
                    loadingDialog = false,
                )
            }
        }

        data class Success(val autoDownloadRule: AutoDownloadRuleBean) : Init
        data class Failed(val msg: String) : Init
    }

    sealed interface Update : AutoDownloadRulePartialStateChange {
        override fun reduce(oldState: AutoDownloadRuleState): AutoDownloadRuleState {
            return when (this) {
                is Failed -> oldState.copy(loadingDialog = false)
                is Success -> oldState.copy(loadingDialog = false)
            }
        }

        data object Success : Update
        data class Failed(val msg: String) : Update
    }

    sealed interface Delete : AutoDownloadRulePartialStateChange {
        override fun reduce(oldState: AutoDownloadRuleState): AutoDownloadRuleState {
            return when (this) {
                is Failed -> oldState.copy(loadingDialog = false)
                is Success -> oldState.copy(
                    ruleState = AutoDownloadRuleState.RuleState.Init,
                    loadingDialog = false
                )
            }
        }

        data object Success : Delete
        data class Failed(val msg: String) : Delete
    }
}
