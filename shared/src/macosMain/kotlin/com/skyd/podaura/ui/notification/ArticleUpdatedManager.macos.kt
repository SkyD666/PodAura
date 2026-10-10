package com.skyd.podaura.ui.notification

import com.skyd.podaura.model.bean.ArticleNotificationRuleBean
import com.skyd.podaura.model.bean.article.ArticleBean

actual object PlatformArticleNotification {
    actual suspend fun requestPermission(showSettingsIfDenied: Boolean) = Unit

    actual suspend fun sendNotification(matchedData: List<Pair<String, ArticleNotificationRuleBean>>) {
        TODO("Not yet implemented")
    }
}

actual object PlatformAutoDownload {
    actual suspend fun autoDownload(data: Map<String, List<ArticleBean>>) {
        TODO("Not yet implemented")
    }
}
