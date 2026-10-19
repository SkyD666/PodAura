package com.skyd.podaura.ui.player.mini

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import com.skyd.podaura.ui.component.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import coil3.ImageLoader
import coil3.PlatformContext
import com.skyd.podaura.ui.local.LocalWindowSizeClass
import com.skyd.podaura.ui.player.PlaybackEnd
import com.skyd.podaura.ui.player.PlaybackEndReason
import com.skyd.podaura.ui.player.PlayerCommand
import com.skyd.podaura.ui.player.PlayerEvent
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import com.skyd.podaura.ui.player.mpv.MPVPlayer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.skia.Image
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.retry
import podaura.shared.generated.resources.skip_next
import podaura.shared.generated.resources.skip_previous
import java.io.File
import kotlin.test.Test

class MiniPlayerLoadingTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun firstLoadDisablesSkippingAndFailureExposesRetryOnNarrowAndWideScreens() = runBlocking {
        val acquired = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val owner = launch(Dispatchers.IO) {
            MPVPlayer.instance.withOwnership { acquired.complete(Unit); release.await() }
        }
        acquired.await()
        val imageLoader = ImageLoader.Builder(PlatformContext.INSTANCE).build()
        startKoin { modules(module { single<ImageLoader> { imageLoader } }) }
        try {
            for (width in listOf(360, 840)) {
                var coordinator: PlayerCoordinator? = null
                try {
                    runDesktopComposeUiTest(width = width, height = 100) {
                        var next = ""
                        var previous = ""
                        var retry = ""
                        runOnIdle { coordinator = PlayerCoordinator() }
                        setContent {
                            next = stringResource(Res.string.skip_next)
                            previous = stringResource(Res.string.skip_previous)
                            retry = stringResource(Res.string.retry)
                            CompositionLocalProvider(LocalWindowSizeClass provides calculateWindowSizeClass()) {
                                MaterialTheme(colorScheme = if (width == 360) lightColorScheme() else darkColorScheme()) {
                                    Surface { MiniPlayer(coordinator, {}, {}) }
                                }
                            }
                        }
                        onNodeWithContentDescription(next).assertIsNotEnabled()
                        if (width == 840) onNodeWithContentDescription(previous).assertIsNotEnabled()
                        onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
                            .assertCountEquals(2)
                        val output = File("build/reports/player-runtime/mini-loading-$width.png")
                        output.parentFile.mkdirs()
                        output.writeBytes(Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap())
                            .encodeToData()!!.bytes)
                        runOnIdle {
                            coordinator!!.model.onEvent(PlayerEvent.EndFile(PlaybackEnd(PlaybackEndReason.Error)))
                        }
                        onNodeWithContentDescription(retry).assertIsEnabled()
                        onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
                            .assertCountEquals(1)
                        runOnIdle { coordinator!!.onCommand(PlayerCommand.Destroy) }
                    }
                } finally {
                    coordinator?.onCommand(PlayerCommand.Destroy)
                    coordinator?.awaitDestroyed()
                }
            }
        } finally {
            release.complete(Unit)
            owner.join()
            imageLoader.shutdown()
            stopKoin()
        }
    }
}
