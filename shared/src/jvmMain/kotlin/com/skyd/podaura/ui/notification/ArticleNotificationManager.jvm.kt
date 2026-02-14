package com.skyd.podaura.ui.notification

import com.skyd.podaura.model.bean.ArticleNotificationRuleBean
import com.skyd.podaura.model.bean.article.ArticleBean

actual object PlatformArticleNotification {
    actual fun sendNotification(matchedData: List<Pair<String, ArticleNotificationRuleBean>>) {
        TODO()
    }
}

actual object PlatformAutoDownload {
    actual suspend fun autoDownload(data: Map<String, List<ArticleBean>>) {
        TODO()
    }
}
