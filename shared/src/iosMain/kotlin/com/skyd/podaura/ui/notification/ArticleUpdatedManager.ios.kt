package com.skyd.podaura.ui.notification

import co.touchlab.kermit.Logger
import com.skyd.podaura.model.bean.ArticleNotificationRuleBean
import com.skyd.podaura.model.bean.article.ArticleBean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import platform.Foundation.NSURL
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleCancel
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIApplicationState.UIApplicationStateActive
import platform.UIKit.UIBackgroundTaskInvalid
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSettings
import platform.UserNotifications.UNUserNotificationCenter
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.cancel
import podaura.shared.generated.resources.notification_permission_denied
import podaura.shared.generated.resources.open_notification_settings
import podaura.shared.generated.resources.update_notification_screen_name
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.uuid.Uuid

actual object PlatformArticleNotification {
    private val center get() = UNUserNotificationCenter.currentNotificationCenter()
    private val permissionMutex = Mutex()
    private val delegate = ArticleNotificationDelegate()

    internal fun initialize() {
        center.delegate = delegate
    }

    actual suspend fun requestPermission(showSettingsIfDenied: Boolean) =
        withContext(Dispatchers.Main) {
            permissionMutex.withLock {
                if (UIApplication.sharedApplication.applicationState != UIApplicationStateActive) return@withLock
                try {
                    when (settings().authorizationStatus) {
                        UNAuthorizationStatusNotDetermined -> suspendCancellableCoroutine { continuation ->
                            center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { _, error ->
                                if (error == null) continuation.resume(Unit)
                                else continuation.resumeWithException(IllegalStateException(error.localizedDescription))
                            }
                        }

                        UNAuthorizationStatusDenied -> if (showSettingsIfDenied) showSettingsAlert()
                        else -> Unit
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Logger.w(
                        throwable = error,
                        tag = "ArticleNotification"
                    ) { "Notification permission request failed" }
                }
            }
        }

    actual suspend fun sendNotification(matchedData: List<Pair<String, ArticleNotificationRuleBean>>) {
        if (matchedData.isEmpty()) return
        withContext(Dispatchers.Main) {
            val app = UIApplication.sharedApplication
            var backgroundTask = UIBackgroundTaskInvalid
            fun finish() {
                if (backgroundTask != UIBackgroundTaskInvalid) {
                    app.endBackgroundTask(backgroundTask)
                    backgroundTask = UIBackgroundTaskInvalid
                }
            }
            backgroundTask = app.beginBackgroundTaskWithName("Article notification", ::finish)
            try {
                val status = settings().authorizationStatus
                if (status != UNAuthorizationStatusAuthorized && status != UNAuthorizationStatusProvisional &&
                    status != UNAuthorizationStatusEphemeral
                ) return@withContext
                val request = UNNotificationRequest.requestWithIdentifier(
                    Uuid.random().toString(), articleNotificationContent(matchedData), null,
                )
                suspendCancellableCoroutine<Unit> { continuation ->
                    center.addNotificationRequest(request) { error ->
                        if (error == null) continuation.resume(Unit)
                        else continuation.resumeWithException(IllegalStateException(error.localizedDescription))
                    }
                }
            } finally {
                finish()
            }
        }
    }

    private suspend fun settings(): UNNotificationSettings =
        suspendCancellableCoroutine { continuation ->
            center.getNotificationSettingsWithCompletionHandler { continuation.resume(it!!) }
        }

    private suspend fun showSettingsAlert() {
        val window =
            UIApplication.sharedApplication.connectedScenes.filterIsInstance<UIWindowScene>()
                .flatMap { it.windows.filterIsInstance<UIWindow>() }
                .firstOrNull { it.isKeyWindow() }
        var presenter = window?.rootViewController ?: return
        while (presenter.presentedViewController != null) presenter =
            presenter.presentedViewController!!
        if (presenter is UIAlertController) return
        val alert = UIAlertController.alertControllerWithTitle(
            getString(Res.string.update_notification_screen_name),
            getString(Res.string.notification_permission_denied), UIAlertControllerStyleAlert,
        )
        alert.addAction(
            UIAlertAction.actionWithTitle(
                getString(Res.string.cancel),
                UIAlertActionStyleCancel,
                null
            )
        )
        alert.addAction(
            UIAlertAction.actionWithTitle(
                getString(Res.string.open_notification_settings), UIAlertActionStyleDefault,
            ) {
                UIApplication.sharedApplication.openURL(
                    NSURL(string = UIApplicationOpenSettingsURLString),
                    emptyMap<Any?, Any>(),
                    null
                )
            })
        presenter.presentViewController(alert, animated = true, completion = null)
    }
}

actual object PlatformAutoDownload {
    actual suspend fun autoDownload(data: Map<String, List<ArticleBean>>) {
        Logger.w(tag = "ArticleNotification") { "Automatic article downloads are unavailable on iOS" }
    }
}
