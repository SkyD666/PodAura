package com.skyd.anivu.ui.screen.settings.rssconfig.updatenotification

import com.skyd.anivu.base.mvi.MviIntent
import com.skyd.anivu.model.bean.ArticleNotificationRuleBean

sealed interface UpdateNotificationIntent : MviIntent {
    data object Init : UpdateNotificationIntent
    data class Add(val rule: ArticleNotificationRuleBean) : UpdateNotificationIntent
    data class Remove(val ruleId: Int) : UpdateNotificationIntent
}