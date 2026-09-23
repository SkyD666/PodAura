package com.skyd.podaura.ui.notification

import co.touchlab.kermit.Logger
import com.skyd.fundation.config.appDirectories
import com.skyd.fundation.jna.NativeArticleNotifications
import com.skyd.fundation.util.Platform
import com.skyd.fundation.util.platform
import com.skyd.podaura.ui.component.UuidList
import com.skyd.podaura.ui.screen.article.ArticleRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext
import java.nio.file.Path

internal object DesktopArticleNotifications {
    private const val TAG = "ArticleNotification"
    private val pending = Channel<String>(Channel.UNLIMITED)
    val activations = pending.receiveAsFlow()
    private val store by lazy {
        DesktopNotificationStore(Path.of(appDirectories.dataDir, "article-notifications"))
    }
    @Volatile private var initialized = false
    private var available = false

    /** On macOS this must run before AWT starts NSApplication, to catch cold-start responses. */
    @Synchronized
    fun initialize() {
        if (initialized) return
        initialized = true
        if (platform != Platform.macOS_Jvm && platform != Platform.Windows) return
        available = runCatching {
            if (platform == Platform.Windows && !registerWindowsNotificationProtocol()) return@runCatching false
            NativeArticleNotifications.initialize(::activate)
        }.onFailure { error ->
            Logger.w(throwable = error, tag = TAG) { "Desktop notification initialization failed" }
        }.getOrDefault(false)
        if (!available) Logger.w(tag = TAG) {
            "Native notifications are unavailable for this launch; use an installed PodAura application"
        }
    }

    fun activate(uri: String) {
        if (notificationIdFromUri(uri) != null) pending.trySend(uri)
    }

    fun requestPermission() {
        initialize()
        if (available) runCatching { NativeArticleNotifications.requestPermission() }
            .onFailure { Logger.w(throwable = it, tag = TAG) { "Notification permission request failed" } }
    }

    fun send(articleIds: List<String>, title: String, body: String) {
        initialize()
        if (!available) return
        runCatching {
            val id = store.save(articleIds)
            try {
                check(NativeArticleNotifications.send(id, title, body, NOTIFICATION_URI_PREFIX + id)) {
                    "The native notification service rejected the notification"
                }
            } catch (error: Throwable) {
                store.remove(id)
                throw error
            }
        }.onFailure { error ->
            Logger.w(throwable = error, tag = TAG) { "Failed to send desktop article notification" }
        }
    }

    suspend fun articleDeeplink(uri: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            store.read(uri)?.let { ArticleRoute(articleIds = UuidList(it)).toDeeplink() }
        }.onFailure { Logger.w(throwable = it, tag = TAG) { "Failed to read notification articles" } }
            .getOrNull()
    }
}
