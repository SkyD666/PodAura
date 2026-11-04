package com.skyd.podaura.ui.screen.calendar

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
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
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.ui.component.navigation.PodAuraSerializersModule
import com.skyd.podaura.ui.player.jumper.PlayDataMode
import com.skyd.podaura.ui.player.jumper.PlayerJumper
import com.skyd.podaura.ui.player.jumper.ArticlePlaylistSource
import com.skyd.podaura.ui.screen.calendar.daylist.item.ArticleItem
import com.skyd.podaura.ui.screen.read.ReadRoute
import com.skyd.podaura.ui.theme.PodAuraTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.skia.Image
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.play
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CalendarArticleItemTest {
    private lateinit var imageLoader: ImageLoader

    @BeforeTest
    fun setUp() {
        imageLoader = ImageLoader.Builder(PlatformContext.INSTANCE).build()
        startKoin {
            modules(module {
                single<ImageLoader> { imageLoader }
                single<DataStore<Preferences>> {
                    object : DataStore<Preferences> {
                        override val data = MutableStateFlow(emptyPreferences())
                        override suspend fun updateData(
                            transform: suspend (Preferences) -> Preferences,
                        ): Preferences = transform(data.value).also { data.value = it }
                    }
                }
            })
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        imageLoader.shutdown()
    }

    @Test
    fun narrowLightColumnKeepsPlaySeparateFromReading() = checkItem(280, false)

    @Test
    fun darkPhoneItemKeepsPlaySeparateFromReading() = checkItem(360, true)

    @OptIn(ExperimentalTestApi::class)
    private fun checkItem(width: Int, dark: Boolean) = runDesktopComposeUiTest(width = width, height = 400) {
        val cover = File("../doc/image/PodAura.png").absolutePath.takeIf { dark }
        if (cover != null) {
            assertIs<SuccessResult>(runBlocking {
                imageLoader.execute(ImageRequest.Builder(PlatformContext.INSTANCE).data(cover).build())
            })
        }
        val day = 1_793_664_000_000L
        val title = "A long episode title covering several topics and continuing onto another line"
        val first = EnclosureBean("episode", "https://example.com/episode.mp3", 0, "audio/mpeg")
        val second = first.copy(url = "https://example.com/alternate.mp3")
        var enclosures by mutableStateOf(listOf(first))
        val playbackRequests = mutableListOf<PlayDataMode>()
        val playerJumper = object : PlayerJumper {
            override fun jump(mode: PlayDataMode) { playbackRequests += mode }
        }
        fun expectedRequest(url: String) = PlayDataMode.ArticleList(
            articleId = "episode", url = url, playlistSource = ArticlePlaylistSource.CalendarDay(day),
        )
        var playLabel = ""
        var routes: () -> List<Any> = { emptyList() }
        setContent {
            val backStack = rememberNavBackStack(
                SavedStateConfiguration { serializersModule = PodAuraSerializersModule }, CalendarRoute,
            )
            routes = { backStack.toList() }
            CompositionLocalProvider(
                LocalNavBackStack provides newNavBackStack(backStack, parent = null),
            ) {
                PodAuraTheme(darkTheme = dark) {
                    playLabel = stringResource(Res.string.play)
                    Surface(Modifier.fillMaxWidth().testTag("calendar-item")) {
                        ArticleItem(
                            articleWithFeed = ArticleWithFeed(
                                ArticleWithEnclosureBean(
                                    ArticleBean(articleId = "episode", feedUrl = "feed", title = title, isRead = true, image = cover),
                                    enclosures = enclosures, categories = emptyList(), media = null,
                                ),
                                FeedBean(url = "feed", title = "A podcast with a very long name"),
                            ),
                            day = day,
                            playerJumper = playerJumper,
                        )
                    }
                }
            }
        }
        val output = File("build/reports/calendar-playback-$width-${if (dark) "dark" else "light"}.png")
        output.parentFile.mkdirs()
        output.writeBytes(Image.makeFromBitmap(onNodeWithTag("calendar-item").captureToImage().asSkiaBitmap()).encodeToData()!!.bytes)
        val play = onNodeWithContentDescription(playLabel).assertIsDisplayed()
        val buttonBounds = play.fetchSemanticsNode().boundsInRoot
        val itemBounds = onNodeWithTag("calendar-item").fetchSemanticsNode().boundsInRoot
        val titleBounds = onNodeWithText(title, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(itemBounds.center.y, buttonBounds.center.y)
        assertTrue(buttonBounds.right < titleBounds.left)
        play.assertTouchWidthIsEqualTo(50.dp).assertTouchHeightIsEqualTo(50.dp)

        play.performTouchInput { click(Offset(4f, 4f)) }
        play.performTouchInput { click(Offset(this.width - 4f, this.height - 4f)) }
        runOnIdle {
            assertEquals<List<PlayDataMode>>(listOf(expectedRequest(first.url), expectedRequest(first.url)), playbackRequests)
            assertEquals(listOf(CalendarRoute), routes())
        }
        onNodeWithText(title).performTouchInput { click() }
        runOnIdle {
            assertEquals(ReadRoute("episode", playlistSource = ArticlePlaylistSource.CalendarDay(day)), routes().last())
            assertEquals<List<PlayDataMode>>(listOf(expectedRequest(first.url), expectedRequest(first.url)), playbackRequests)
            enclosures = emptyList()
        }
        onNodeWithContentDescription(playLabel).assertDoesNotExist()
        assertEquals(titleBounds, onNodeWithText(title, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot)
        onNodeWithText(title).performTouchInput { click() }
        runOnIdle {
            assertEquals(ReadRoute("episode", playlistSource = ArticlePlaylistSource.CalendarDay(day)), routes().last())
            assertEquals<List<PlayDataMode>>(listOf(expectedRequest(first.url), expectedRequest(first.url)), playbackRequests)
            enclosures = listOf(first, second)
        }
        onNodeWithContentDescription(playLabel).performClick()
        onNodeWithText(first.url).assertIsDisplayed()
        onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Expand))
            .performSemanticsAction(SemanticsActions.Expand)
        onNodeWithText(second.url).performScrollTo().assertIsDisplayed()
        runOnIdle { assertEquals<List<PlayDataMode>>(listOf(expectedRequest(first.url), expectedRequest(first.url)), playbackRequests) }
        onAllNodesWithContentDescription(playLabel).onLast().performScrollTo().performClick()
        runOnIdle {
            assertEquals<List<PlayDataMode>>(
                listOf(expectedRequest(first.url), expectedRequest(first.url), expectedRequest(second.url)),
                playbackRequests,
            )
        }
    }

}
