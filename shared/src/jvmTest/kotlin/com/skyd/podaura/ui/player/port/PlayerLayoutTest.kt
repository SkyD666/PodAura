package com.skyd.podaura.ui.player.port

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import coil3.ImageLoader
import coil3.PlatformContext
import com.skyd.podaura.model.repository.player.PlayerRepository
import com.skyd.podaura.ui.player.PlayerArticleContextState
import com.skyd.podaura.ui.player.PlayerArticleContextViewModel
import com.skyd.podaura.ui.player.PlayerProgress
import com.skyd.podaura.ui.player.PlayerViewModel
import com.skyd.podaura.ui.player.Track
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.ui.player.component.state.dialog.OnDialogVisibilityChanged
import com.skyd.podaura.ui.player.queue.queueCallbacks
import com.skyd.podaura.ui.player.queue.queueItems
import com.skyd.podaura.ui.player.queue.queueState
import com.skyd.podaura.ui.player.service.PlayerState
import com.skyd.podaura.ui.window.DesktopAppState
import com.skyd.podaura.ui.window.DesktopWindowManager
import com.skyd.podaura.ui.window.LocalDesktopAppState
import com.skyd.podaura.ui.window.PlayerWindowController
import java.io.File
import java.lang.reflect.Proxy
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@OptIn(ExperimentalTestApi::class)
class PlayerLayoutTest {
    private lateinit var imageLoader: ImageLoader
    private lateinit var app: DesktopAppState
    private val store = ViewModelStore()

    @BeforeTest
    fun setUp() {
        imageLoader = ImageLoader.Builder(PlatformContext.INSTANCE).build()
        startKoin { modules(module { single<ImageLoader> { imageLoader } }) }
        val player = PlayerViewModel(PlayerRepository(unused(), unused(), unused()), unused())
        val article = PlayerArticleContextViewModel(unused())
        store.put("player", player)
        store.put("article", article)
        app = DesktopAppState(DesktopWindowManager(), PlayerWindowController(player), article, { false })
    }

    @AfterTest
    fun tearDown() {
        runBlocking { withContext(Dispatchers.Main) { store.clear() } }
        imageLoader.shutdown()
        stopKoin()
    }

    @Test
    fun availableWidthSwitchesBetweenSidebarAndSheetInsideTheSameWindow() =
        runDesktopComposeUiTest(width = 1100, height = 760) {
            var availableWidth by mutableStateOf(1100.dp)
            setContent { MaterialTheme { TestPlayer(modifier = Modifier.width(availableWidth)) } }
            onNodeWithText("Current queue").assertIsDisplayed()
            onNodeWithContentDescription("Playlist").assertDoesNotExist()
            runOnIdle { availableWidth = 600.dp }
            onNodeWithText("Current queue").assertDoesNotExist()
            onNodeWithContentDescription("Playlist").assertIsDisplayed().performClick()
            onNodeWithText("Current queue").assertIsDisplayed()
            save("narrow-queue-sheet", onNode(isDialog()).captureToImage())
            runOnIdle { availableWidth = 1100.dp }
            onNode(isDialog()).assertDoesNotExist()
            onNodeWithContentDescription("Playlist").assertDoesNotExist()
            onNodeWithText("Current queue").assertIsDisplayed()
        }

    @Test
    fun wideNarrowShortAndLargeTypeKeepControlsReachableAndMediaProportional() {
        val cases = listOf(
            LayoutCase("wide-light", 1200, 800),
            LayoutCase("wide-dark", 1200, 800, dark = true),
            LayoutCase("narrow-light", 390, 800),
            LayoutCase("narrow-landscape", 700, 400),
            LayoutCase("wide-short", 1100, 420),
            LayoutCase("wide-short-large-type", 1100, 420, fontScale = 2f, dark = true),
            LayoutCase("narrow-large-type", 390, 600, fontScale = 2f),
            LayoutCase("empty", 1100, 700, count = 0),
            LayoutCase("single", 1100, 700, count = 1),
            LayoutCase("loading", 1100, 700, count = 0, presentation = PlayerPresentationState.Loading),
            LayoutCase("failed", 1100, 420, count = 0,
                presentation = PlayerPresentationState.Failed("Decoder unavailable. Retry playback.")),
        )
        for (case in cases) runDesktopComposeUiTest(width = case.width, height = case.height) {
            setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, case.fontScale)) {
                    MaterialTheme(colorScheme = if (case.dark) darkColorScheme() else lightColorScheme()) {
                        val items = remember { queueItems(case.count).also { items ->
                            items.firstOrNull()?.playlistMediaBean?.title =
                                "A long episode title about music, science, and the stories behind everyday discoveries"
                        } }
                        TestPlayer(state = queueState(items), presentation = case.presentation)
                    }
                }
            }
            val media = onNodeWithTag("PlayerMedia").fetchSemanticsNode().boundsInRoot
            assertTrue(abs(media.width - media.height) < 1f, "${case.name}: media must remain square")
            assertTrue(media.height > 0f)
            if (case.width >= 840) {
                val queue = onNodeWithText("Current queue").fetchSemanticsNode().boundsInRoot
                assertTrue(media.right < queue.left, "${case.name}: queue belongs to the right of media")
                onNodeWithContentDescription("Playlist").assertDoesNotExist()
            } else {
                onNodeWithText("Current queue").assertDoesNotExist()
            }
            save(case.name, onNodeWithTag("PlayerLayout").captureToImage())
            if (case.count == 0) onNodeWithText("The queue is empty").assertExists()
            onNodeWithContentDescription("Fullscreen").performScrollTo().assertIsDisplayed()
            if (case.fontScale > 1f) save("${case.name}-scrolled", onNodeWithTag("PlayerLayout").captureToImage())
            if (case.presentation is PlayerPresentationState.Failed) {
                onNodeWithContentDescription("Retry").performScrollTo().assertIsDisplayed()
            }
        }
    }

    @Test
    fun ordinaryVideoStillHasTheQueueAndRequestsFullscreen() =
        runDesktopComposeUiTest(width = 1100, height = 700) {
            var fullscreenRequests = 0
            var mounts = 0
            var disposals = 0
            var availableWidth by mutableStateOf(1100.dp)
            setContent {
                MaterialTheme {
                    TestPlayer(
                        modifier = Modifier.width(availableWidth),
                        state = queueState(queueItems(12)).copy(videoTracks = listOf(Track(1, "Video", false))),
                        onEnterFullscreen = { fullscreenRequests++ },
                        playerContent = {
                            DisposableEffect(Unit) { mounts++; onDispose { disposals++ } }
                            Box(Modifier.fillMaxSize().background(Color.Black).testTag("VideoSurface"))
                        },
                    )
                }
            }
            onNodeWithTag("VideoSurface").assertIsDisplayed()
            onNodeWithText("Current queue").assertIsDisplayed()
            runOnIdle { availableWidth = 600.dp }
            onNodeWithTag("VideoSurface").assertIsDisplayed()
            runOnIdle { availableWidth = 1100.dp }
            onNodeWithContentDescription("Fullscreen").performClick()
            runOnIdle {
                assertTrue(fullscreenRequests == 1)
                assertTrue(mounts == 1 && disposals == 0, "Resizing must keep the same video composition")
            }
            save("ordinary-video", onNodeWithTag("PlayerLayout").captureToImage())
        }

    @Composable
    private fun TestPlayer(
        modifier: Modifier = Modifier.fillMaxSize(),
        state: PlayerState = queueState(queueItems(30)),
        presentation: PlayerPresentationState = PlayerPresentationState.Ready,
        onEnterFullscreen: () -> Unit = {},
        playerContent: @Composable () -> Unit = {},
    ) {
        CompositionLocalProvider(LocalDesktopAppState provides app) {
            PortraitPlayerView(
                playState = PlayState(false, false, state),
                articleContextState = PlayerArticleContextState(),
                playStateCallback = queueCallbacks,
                onDialogVisibilityChanged = OnDialogVisibilityChanged(
                    onSpeedDialog = {}, onSubtitleTrackDialog = {}, onAudioTrackDialog = {},
                    onSubtitleSettingDialog = {}, onAudioSettingDialog = {}, onReplaySecondDialog = {},
                    onForwardSecondDialog = {}, onSleepTimerDialog = {},
                ),
                onBack = {}, onEnterFullscreen = onEnterFullscreen, onSetArticleFavorite = {},
                snackbarHostState = remember { SnackbarHostState() }, modifier = modifier.testTag("PlayerLayout"),
                presentationState = presentation, playerContent = playerContent,
                initialProgress = PlayerProgress(state.position, state.duration, state.buffer),
            )
        }
    }

    private inline fun <reified T> unused(): T = Proxy.newProxyInstance(
        T::class.java.classLoader, arrayOf(T::class.java),
    ) { _, method, _ -> error("Unexpected ${method.name}") } as T

    private data class LayoutCase(
        val name: String,
        val width: Int,
        val height: Int,
        val fontScale: Float = 1f,
        val dark: Boolean = false,
        val count: Int = 30,
        val presentation: PlayerPresentationState = PlayerPresentationState.Ready,
    )

    private fun save(name: String, bitmap: androidx.compose.ui.graphics.ImageBitmap) {
        val output = File("build/reports/player-layout/$name.png")
        output.parentFile.mkdirs()
        output.writeBytes(Image.makeFromBitmap(bitmap.asSkiaBitmap()).encodeToData()!!.bytes)
    }
}
