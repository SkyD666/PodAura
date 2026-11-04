package com.skyd.podaura.ui.notification

import com.skyd.podaura.model.bean.ArticleNotificationRuleBean
import com.skyd.podaura.ui.component.UuidList
import com.skyd.podaura.ui.screen.article.ArticleRoute
import kotlinx.coroutines.channels.Channel
import org.jetbrains.compose.resources.getString
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationDefaultActionIdentifier
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionList
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.article_notification_content_text
import podaura.shared.generated.resources.article_notification_new_articles
import kotlin.uuid.Uuid

private const val ARTICLE_IDS = "articleIds"
internal val appleArticleNotificationRequests = Channel<ArticleRoute>(Channel.UNLIMITED)

/** The OS retains each notification's IDs, including across application restarts. */
internal fun articleNotificationRoute(userInfo: Map<Any?, *>): ArticleRoute? {
    val values = userInfo[ARTICLE_IDS] as? List<*> ?: return null
    if (values.isEmpty() || values.any { it !is String || Uuid.parseOrNull(it) == null }) return null
    return ArticleRoute(articleIds = UuidList(values.filterIsInstance<String>().distinct()))
}

internal suspend fun articleNotificationContent(
    matchedData: List<Pair<String, ArticleNotificationRuleBean>>,
): UNMutableNotificationContent = UNMutableNotificationContent().apply {
    setTitle(getString(Res.string.article_notification_new_articles))
    setBody(
        getString(
            Res.string.article_notification_content_text,
            matchedData.map { it.second }.distinctBy { it.id }.joinToString(", ") { it.name },
        )
    )
    setSound(UNNotificationSound.defaultSound)
    setUserInfo(mapOf(ARTICLE_IDS to matchedData.map { it.first }.distinct()))
}

// UNUserNotificationCenter holds its delegate weakly; retain it for the app's lifetime.
internal class ArticleNotificationDelegate : NSObject(), UNUserNotificationCenterDelegateProtocol {
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (UNNotificationPresentationOptions) -> Unit,
    ) {
        withCompletionHandler(
            UNNotificationPresentationOptionBanner or UNNotificationPresentationOptionList or
                    UNNotificationPresentationOptionSound
        )
    }

    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        didReceiveNotificationResponse: UNNotificationResponse,
        withCompletionHandler: () -> Unit,
    ) {
        if (didReceiveNotificationResponse.actionIdentifier == UNNotificationDefaultActionIdentifier) {
            articleNotificationRoute(didReceiveNotificationResponse.notification.request.content.userInfo)
                ?.let { appleArticleNotificationRequests.trySend(it) }
        }
        withCompletionHandler()
    }
}
