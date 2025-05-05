package com.skyd.podaura.ui.notification

import com.skyd.podaura.model.bean.ArticleNotificationRuleBean

actual object PlatformArticleNotification {
    actual fun sendNotification(matchedData: List<Pair<String, ArticleNotificationRuleBean>>) {
        TODO()
    }
}