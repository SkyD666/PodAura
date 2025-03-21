package com.skyd.anivu.ui.screen.settings.rssconfig.updatenotification

import com.skyd.anivu.model.bean.ArticleNotificationRuleBean


internal sealed interface UpdateNotificationPartialStateChange {
    fun reduce(oldState: UpdateNotificationState): UpdateNotificationState

    sealed interface LoadingDialog : UpdateNotificationPartialStateChange {
        data object Show : LoadingDialog {
            override fun reduce(oldState: UpdateNotificationState) =
                oldState.copy(loadingDialog = true)
        }
    }

    sealed interface Add : UpdateNotificationPartialStateChange {
        override fun reduce(oldState: UpdateNotificationState): UpdateNotificationState {
            return when (this) {
                is Failed -> oldState.copy(loadingDialog = false)
                is Success -> oldState.copy(loadingDialog = false)
            }
        }

        data object Success : Add
        data class Failed(val msg: String) : Add
    }

    sealed interface Remove : UpdateNotificationPartialStateChange {
        override fun reduce(oldState: UpdateNotificationState): UpdateNotificationState {
            return when (this) {
                is Failed -> oldState.copy(loadingDialog = false)
                is Success -> oldState.copy(loadingDialog = false)
            }
        }

        data object Success : Remove
        data class Failed(val msg: String) : Remove
    }

    sealed interface RuleList : UpdateNotificationPartialStateChange {
        override fun reduce(oldState: UpdateNotificationState): UpdateNotificationState {
            return when (this) {
                is Success -> oldState.copy(
                    ruleListState = RuleListState.Success(rules = rules),
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    ruleListState = RuleListState.Failed(msg = msg),
                    loadingDialog = false,
                )
            }
        }

        data class Success(val rules: List<ArticleNotificationRuleBean>) : RuleList
        data class Failed(val msg: String) : RuleList
    }
}
