package com.skyd.podaura.ui.notification

import com.skyd.fundation.jna.NativeArticleNotifications
import com.skyd.fundation.util.Platform
import com.skyd.fundation.util.platform
import com.skyd.podaura.ui.component.UuidList
import com.skyd.podaura.ui.component.navigation.ExternalUrlHandler
import com.skyd.podaura.ui.component.navigation.toNavKey
import com.skyd.podaura.ui.screen.article.ArticleRoute
import com.skyd.podaura.ui.window.desktopLaunchArguments
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

class ArticleNotificationManagerTest {
    @Test
    fun macNativeBridgeLoadsButDoesNotRequestPermissionForAnUnbundledJvm() {
        if (platform != Platform.macOS_Jvm || System.getProperty("jpackage.app-path") != null) return
        assertFalse(NativeArticleNotifications.initialize { error("Unexpected activation") })
    }

    @Test
    fun retainedNotificationsKeepTheirOwnArticlesAcrossApplicationRestarts() = withStore { directory ->
        val firstArticles = List(5_000) { UUID.randomUUID().toString() }
        val secondArticles = listOf(UUID.randomUUID().toString())
        val store = DesktopNotificationStore(directory)
        val firstUri = NOTIFICATION_URI_PREFIX + store.save(firstArticles + firstArticles.first())
        val secondUri = NOTIFICATION_URI_PREFIX + store.save(secondArticles)

        val restarted = DesktopNotificationStore(directory)
        assertEquals(firstArticles, restarted.read(firstUri))
        assertEquals(secondArticles, restarted.read(secondUri))
        // The OS receives a constant-size ID even for a 5,000-article batch.
        assertEquals(NOTIFICATION_URI_PREFIX.length + 36, firstUri.length)
    }

    @Test
    fun notificationArticlesUseTheSameRouteAsAndroid() = withStore { directory ->
        val ids = List(3) { UUID.randomUUID().toString() }
        val store = DesktopNotificationStore(directory)
        val uri = NOTIFICATION_URI_PREFIX + store.save(ids)
        val route = ArticleRoute(articleIds = UuidList(store.read(uri)!!))
        assertEquals(route, ExternalUrlHandler.UrlData(url = route.toDeeplink()).toNavKey())
    }

    @Test
    fun invalidActivationCannotReadArbitraryFilesOrOpenAnUnfilteredArticleList() = withStore { directory ->
        val store = DesktopNotificationStore(directory)
        val id = UUID.randomUUID().toString()
        listOf(
            "file:///etc/passwd", "podaura://article.screen", "podaura://notification/../secret",
            "podaura://notification/%2e%2e%2fsecret", "podaura://notification/$id?extra=1",
            "podaura://notification/$id#extra", "podaura://user@notification/$id",
            "podaura://notification:123/$id", "podaura://notification/1-1-1-1-1",
        ).forEach { assertNull(store.read(it), it) }
        assertNull(store.read(NOTIFICATION_URI_PREFIX + id))
        Files.writeString(directory.resolve("$id.json"), "[]")
        assertNull(store.read(NOTIFICATION_URI_PREFIX + id))
        Files.writeString(directory.resolve("$id.json"), "broken json")
        assertNull(store.read(NOTIFICATION_URI_PREFIX + id))
        Files.writeString(directory.resolve("$id.json"), "[\"not-an-article-id\"]")
        assertNull(store.read(NOTIFICATION_URI_PREFIX + id))
        assertFailsWith<IllegalArgumentException> { store.save(emptyList()) }
    }

    @Test
    fun windowsForwardingKeepsActivationUriAndResolvesMediaFilesSeparately() {
        val uri = NOTIFICATION_URI_PREFIX + UUID.randomUUID()
        assertEquals(
            listOf(uri, Path.of("a b.mp3").toAbsolutePath().toString()),
            desktopLaunchArguments(arrayOf(uri, "a b.mp3", "")),
        )
    }

    @Test
    fun nativeClickIsBufferedUntilTheMainWindowIsReady() = runBlocking {
        val uri = NOTIFICATION_URI_PREFIX + UUID.randomUUID()
        DesktopArticleNotifications.activate("podaura://notification/invalid")
        DesktopArticleNotifications.activate(uri)
        assertEquals(uri, withTimeout(2_000) { DesktopArticleNotifications.activations.first() })
    }

    private fun withStore(block: (Path) -> Unit) {
        val directory = Files.createTempDirectory("podaura-notification-test")
        try {
            block(directory)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
