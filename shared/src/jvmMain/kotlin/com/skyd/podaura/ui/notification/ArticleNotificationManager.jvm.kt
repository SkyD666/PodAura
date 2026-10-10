package com.skyd.podaura.ui.notification

import co.touchlab.kermit.Logger
import com.skyd.compone.component.blockString
import com.skyd.podaura.model.bean.ArticleNotificationRuleBean
import com.skyd.podaura.model.bean.article.ArticleBean
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.article_notification_content_text
import podaura.shared.generated.resources.article_notification_new_articles

private const val TAG = "ArticleNotification"

actual object PlatformArticleNotification {
    actual suspend fun requestPermission(showSettingsIfDenied: Boolean) =
        DesktopArticleNotifications.requestPermission()

    actual suspend fun sendNotification(matchedData: List<Pair<String, ArticleNotificationRuleBean>>) {
        if (matchedData.isEmpty()) return
        val ruleNames = matchedData.map { it.second }.distinctBy { it.id }
            .joinToString(", ") { it.name }
        DesktopArticleNotifications.send(
            articleIds = matchedData.map { it.first }.distinct(),
            title = blockString(Res.string.article_notification_new_articles),
            body = blockString(Res.string.article_notification_content_text, ruleNames),
        )
    }
}

actual object PlatformAutoDownload {
    actual suspend fun autoDownload(data: Map<String, List<ArticleBean>>) {
        // Desktop automatic downloads are not implemented yet; do not cancel the update batch.
        Logger.w(tag = TAG) { "Automatic article downloads are unavailable on desktop" }
    }
}
