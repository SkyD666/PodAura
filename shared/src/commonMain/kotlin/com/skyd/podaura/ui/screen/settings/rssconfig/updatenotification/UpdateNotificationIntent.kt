package com.skyd.podaura.ui.screen.settings.rssconfig.updatenotification

import com.skyd.mvi.MviIntent
import com.skyd.podaura.model.bean.ArticleNotificationRuleBean

sealed interface UpdateNotificationIntent : MviIntent {
    data object Init : UpdateNotificationIntent
    data class Add(val rule: ArticleNotificationRuleBean) : UpdateNotificationIntent
    data class Remove(val ruleId: Int) : UpdateNotificationIntent
}