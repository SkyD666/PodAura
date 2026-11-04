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
import platform.Foundation.NSBundle
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSettings
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.uuid.Uuid

actual object PlatformArticleNotification {
    private val delegate = ArticleNotificationDelegate()
    private val permissionMutex = Mutex()
    private val center: UNUserNotificationCenter? by lazy {
        // UserNotifications raises an Objective-C exception for an unbundled executable.
        val bundle = NSBundle.mainBundle
        if (bundle.bundleIdentifier == "com.skyd.podaura" &&
            bundle.bundlePath.endsWith(".app", ignoreCase = true)
        ) {
            UNUserNotificationCenter.currentNotificationCenter().also { it.delegate = delegate }
        } else {
            Logger.w(tag = "ArticleNotification") {
                "Native notifications require a bundled PodAura application"
            }
            null
        }
    }

    /** Install the delegate before AppKit delivers a cold-start notification response. */
    internal fun initialize() {
        center
    }

    actual suspend fun requestPermission(showSettingsIfDenied: Boolean) =
        withContext(Dispatchers.Main) {
            val center = center ?: return@withContext
            permissionMutex.withLock {
                try {
                    if (settings(center).authorizationStatus == UNAuthorizationStatusNotDetermined) {
                        suspendCancellableCoroutine<Unit> { continuation ->
                            center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { _, error ->
                                if (error == null) continuation.resume(Unit)
                                else continuation.resumeWithException(IllegalStateException(error.localizedDescription))
                            }
                        }
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Logger.w(throwable = error, tag = "ArticleNotification") {
                        "Notification permission request failed"
                    }
                }
            }
        }

    actual suspend fun sendNotification(matchedData: List<Pair<String, ArticleNotificationRuleBean>>) {
        if (matchedData.isEmpty()) return
        withContext(Dispatchers.Main) {
            val center = center ?: return@withContext
            if (settings(center).authorizationStatus != UNAuthorizationStatusAuthorized) return@withContext
            val request = UNNotificationRequest.requestWithIdentifier(
                Uuid.random().toString(), articleNotificationContent(matchedData), null,
            )
            suspendCancellableCoroutine<Unit> { continuation ->
                center.addNotificationRequest(request) { error ->
                    if (error == null) continuation.resume(Unit)
                    else continuation.resumeWithException(IllegalStateException(error.localizedDescription))
                }
            }
        }
    }

    private suspend fun settings(center: UNUserNotificationCenter): UNNotificationSettings =
        suspendCancellableCoroutine { continuation ->
            center.getNotificationSettingsWithCompletionHandler { continuation.resume(it!!) }
        }
}

actual object PlatformAutoDownload {
    actual suspend fun autoDownload(data: Map<String, List<ArticleBean>>) {
        Logger.w(tag = "ArticleNotification") { "Automatic article downloads are unavailable on desktop" }
    }
}
