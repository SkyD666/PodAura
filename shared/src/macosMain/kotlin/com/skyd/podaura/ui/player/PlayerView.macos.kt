package com.skyd.podaura.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.skyd.podaura.ui.PlatformSurfaceHolder
import com.skyd.podaura.ui.component.AppKitView
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.ui.player.component.state.PlayStateCallback
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import com.skyd.podaura.ui.player.service.PlayerState
import kotlinx.cinterop.CValue
import kotlinx.cinterop.useContents
import platform.AVFoundation.AVLayerVideoGravityResizeAspect
import platform.AVFoundation.AVSampleBufferDisplayLayer
import platform.AppKit.NSView
import platform.CoreGraphics.CGRect
import platform.Foundation.NSMakeRect
import kotlin.concurrent.atomics.AtomicInt
import kotlin.math.roundToInt

private class MacosVideoView : NSView(NSMakeRect(0.0, 0.0, 1.0, 1.0)) {
    var attached = false
    private val pixelWidth = AtomicInt(2)
    private val pixelHeight = AtomicInt(2)
    val surface = object : PlatformSurfaceHolder {
        override val layer = AVSampleBufferDisplayLayer().apply {
            videoGravity = AVLayerVideoGravityResizeAspect
        }
        override val width get() = pixelWidth.load()
        override val height get() = pixelHeight.load()
        override var onResize: ((Int, Int) -> Unit)? = null
    }

    init {
        wantsLayer = true
        layer = surface.layer
    }

    override fun setFrame(frame: CValue<CGRect>) {
        super.setFrame(frame)
        updateSurfaceSize()
    }

    override fun viewDidChangeBackingProperties() {
        super.viewDidChangeBackingProperties()
        updateSurfaceSize()
    }

    private fun updateSurfaceSize() {
        val scale = window?.backingScaleFactor ?: 1.0
        val width = bounds.useContents { (size.width * scale).roundToInt().coerceAtLeast(2) }
        val height = bounds.useContents { (size.height * scale).roundToInt().coerceAtLeast(2) }
        val changed = pixelWidth.load() != width || pixelHeight.load() != height
        pixelWidth.store(width)
        pixelHeight.store(height)
        if (changed) surface.onResize?.invoke(width, height)
    }
}

@Composable
actual fun PlatformPlayerView(
    coordinator: PlayerCoordinator,
    modifier: Modifier,
    onCommand: (PlayerCommand) -> Unit,
) {
    key(coordinator) { MacosVideoSurface(modifier, onCommand) }
}

@Composable
internal fun MacosVideoSurface(modifier: Modifier, onCommand: (PlayerCommand) -> Unit) {
    val currentOnCommand by rememberUpdatedState(onCommand)
    AppKitView(
        factory = { MacosVideoView() },
        modifier = modifier,
        update = {
            if (!it.attached) {
                it.attached = true
                currentOnCommand(PlayerCommand.Attach(it.surface))
            }
        },
        onRelease = { if (it.attached) currentOnCommand(PlayerCommand.Detach(it.surface)) },
    )
}

// A minimized desktop window keeps playing, regardless of the background preference.
@Composable
actual fun PlatformPlayerLifecycleEffect(coordinator: PlayerCoordinator) = Unit

@Composable
actual fun PlatformContent(
    modifier: Modifier,
    onBack: () -> Unit,
    coordinator: PlayerCoordinator,
    playerState: PlayerState,
    playState: PlayState,
    playStateCallback: PlayStateCallback,
    commonContent: @Composable () -> Unit,
) = commonContent()
