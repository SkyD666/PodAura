package com.skyd.podaura.ui.screen.article

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.skyd.compone.component.navigation.LocalNavBackStack
import com.skyd.compone.component.navigation.newNavBackStack
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.article.ArticleWithEnclosureBean
import com.skyd.podaura.model.bean.article.ArticleWithFeed
import com.skyd.podaura.model.bean.article.EnclosureBean
import com.skyd.podaura.model.bean.article.RssMediaBean
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.repository.player.PlayerRepository
import com.skyd.podaura.ui.component.navigation.PodAuraSerializersModule
import com.skyd.podaura.ui.player.PlayerArticleContextViewModel
import com.skyd.podaura.ui.player.PlayerViewModel
import com.skyd.podaura.ui.window.DesktopAppState
import com.skyd.podaura.ui.window.DesktopWindowManager
import com.skyd.podaura.ui.window.LocalDesktopAppState
import com.skyd.podaura.ui.window.PlayerWindowController
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.skia.Image
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.article_screen_mark_as_unread
import podaura.shared.generated.resources.article_screen_unfavorite
import podaura.shared.generated.resources.play
import java.io.File
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ArticlePlaybackItemTest {
    @Test
    fun narrowLightItemPlaysAndKeepsItsLayoutInSelectionMode() = checkPlayback(320, false)

    @Test
    fun wideDarkItemPlaysAndKeepsItsLayoutInSelectionMode() = checkPlayback(840, true)

    @Test
    fun narrowLightCoverKeepsPlaybackInTheSameFooterPosition() = checkPlayback(320, false, true)

    @Test
    fun wideDarkCoverKeepsPlaybackInTheSameFooterPosition() = checkPlayback(840, true, true)

    @OptIn(ExperimentalTestApi::class)
    private fun checkPlayback(width: Int, dark: Boolean, withCover: Boolean = false) {
        val imageLoader = ImageLoader.Builder(PlatformContext.INSTANCE).build()
        val cover = File("../doc/image/PodAura.png").absolutePath.takeIf { withCover }
        if (cover != null) {
            assertIs<SuccessResult>(runBlocking {
                imageLoader.execute(ImageRequest.Builder(PlatformContext.INSTANCE).data(cover).build())
            })
        }
        startKoin { modules(module { single<ImageLoader> { imageLoader } }) }
        try {
            runDesktopComposeUiTest(width = width, height = 260) {
                var playbackRequests = 0
                val app = DesktopAppState(
                    DesktopWindowManager(),
                    PlayerWindowController(PlayerViewModel(PlayerRepository(unused(), unused(), unused()), unused())),
                    PlayerArticleContextViewModel(unused()),
                    hasAcceptedTerms = { playbackRequests++; false },
                )
                var selected by mutableStateOf<Boolean?>(null)
                var duration by mutableStateOf<Long?>(2720000)
                var hasMedia by mutableStateOf(true)
                var showCover by mutableStateOf(withCover)
                var playLabel = ""
                var favoriteLabel = ""
                var readLabel = ""
                setContent {
                    val backStack = rememberNavBackStack(
                        SavedStateConfiguration { serializersModule = PodAuraSerializersModule }, ArticleRoute(),
                    )
                    CompositionLocalProvider(
                        LocalNavBackStack provides newNavBackStack(backStack, parent = null),
                        LocalDesktopAppState provides app,
                    ) {
                        MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                            playLabel = stringResource(Res.string.play)
                            favoriteLabel = stringResource(Res.string.article_screen_unfavorite)
                            readLabel = stringResource(Res.string.article_screen_mark_as_unread)
                            Surface {
                                Article1Item(
                                    data = ArticleWithFeed(
                                        ArticleWithEnclosureBean(
                                            ArticleBean(
                                                articleId = "episode", feedUrl = "feed", isRead = true,
                                                title = "A long episode title covering several topics",
                                                description = "An episode summary that continues onto another line.",
                                            ),
                                            enclosures = if (hasMedia) listOf(
                                                EnclosureBean("episode", "https://example.com/episode.mp3", 0, "audio/mpeg")
                                            ) else emptyList(),
                                            categories = emptyList(),
                                            media = RssMediaBean("episode", duration, image = cover.takeIf { showCover }),
                                        ),
                                        FeedBean(url = "feed", title = "A podcast with a long name"),
                                    ),
                                    onFavorite = { _, _ -> error("Unexpected favorite") },
                                    onRead = { _, _ -> error("Unexpected read") },
                                    onDelete = { error("Unexpected delete") },
                                    onMessage = { error("Unexpected message") },
                                    selected = selected,
                                )
                            }
                        }
                    }
                }
                onNodeWithText("45:20").assertIsDisplayed()
                val itemBounds = onNodeWithTag("ArticleItem").fetchSemanticsNode().boundsInRoot
                val buttonBounds = onNodeWithText("45:20").fetchSemanticsNode().boundsInRoot
                val favoriteBounds = onNodeWithContentDescription(favoriteLabel).fetchSemanticsNode().boundsInRoot
                val readBounds = onNodeWithContentDescription(readLabel).fetchSemanticsNode().boundsInRoot
                assertEquals(favoriteBounds.center.y, buttonBounds.center.y)
                val output = File("build/reports/article-playback-$width${if (withCover) "-cover" else ""}.png")
                output.parentFile.mkdirs()
                output.writeBytes(Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).encodeToData()!!.bytes)
                onNodeWithText("45:20").performClick()
                runOnIdle { assertEquals(1, playbackRequests) }
                if (withCover) {
                    runOnIdle { showCover = false }
                    val withoutCover = onNodeWithText("45:20").fetchSemanticsNode().boundsInRoot
                    assertEquals(buttonBounds.left, withoutCover.left)
                    assertEquals(buttonBounds.right, withoutCover.right)
                    assertEquals(buttonBounds.size, withoutCover.size)
                    runOnIdle { showCover = true }
                }
                runOnIdle { selected = true }
                onNodeWithText("45:20").assertIsNotEnabled()
                assertEquals(itemBounds, onNodeWithTag("ArticleItem").fetchSemanticsNode().boundsInRoot)
                assertEquals(buttonBounds, onNodeWithText("45:20").fetchSemanticsNode().boundsInRoot)
                runOnIdle { selected = null; duration = 7384000 }
                onNodeWithText("2:03:04").assertIsDisplayed()
                runOnIdle { duration = null }
                onNodeWithText(playLabel).assertIsDisplayed()
                runOnIdle { hasMedia = false }
                onNodeWithText(playLabel).assertDoesNotExist()
                assertEquals(itemBounds, onNodeWithTag("ArticleItem").fetchSemanticsNode().boundsInRoot)
                assertEquals(favoriteBounds, onNodeWithContentDescription(favoriteLabel).fetchSemanticsNode().boundsInRoot)
                assertEquals(readBounds, onNodeWithContentDescription(readLabel).fetchSemanticsNode().boundsInRoot)
            }
        } finally {
            stopKoin()
            imageLoader.shutdown()
        }
    }

    private inline fun <reified T> unused(): T = Proxy.newProxyInstance(
        T::class.java.classLoader, arrayOf(T::class.java),
    ) { _, method, _ -> error("Unexpected repository access: ${method.name}") } as T
}
