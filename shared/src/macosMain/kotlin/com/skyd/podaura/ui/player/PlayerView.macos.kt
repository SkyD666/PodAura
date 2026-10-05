package com.skyd.podaura.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.roundToIntRect
import com.skyd.podaura.ui.PlatformSurfaceHolder
import com.skyd.podaura.ui.component.LocalMacosVideoContainer
import com.skyd.podaura.ui.component.LocalMacosViewUpdates
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.ui.player.component.state.PlayStateCallback
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import com.skyd.podaura.ui.player.service.PlayerState
import kotlinx.cinterop.CValue
import kotlinx.cinterop.interpretObjCPointer
import kotlinx.cinterop.objcPtr
import kotlinx.cinterop.useContents
import platform.AppKit.NSView
import platform.AppKit.NSWindowBelow
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSMakeRect
import platform.Metal.MTLCreateSystemDefaultDevice
import platform.QuartzCore.CAMetalLayer
import platform.QuartzCore.CATransaction

/** The video view is below Skiko; only the measured video rectangle clears Compose alpha. */
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
    val container = LocalMacosVideoContainer.current
    val updates = LocalMacosViewUpdates.current
    val density = LocalDensity.current.density
    val currentOnCommand by rememberUpdatedState(onCommand)
    val holder = remember(container) {
        object : PlatformSurfaceHolder {
            override var onResize: ((Int, Int) -> Unit)? = null
            var lastSize = IntSize.Zero
            override val layer = CAMetalLayer().apply {
                device = MTLCreateSystemDefaultDevice()?.let { interpretObjCPointer(it.objcPtr()) }
                framebufferOnly = true
            }
            val view = NSView(NSMakeRect(0.0, 0.0, 1.0, 1.0))
            val clipView = object : NSView(view.frame) {
                // Compose owns input and accessibility for the player controls above this view.
                override fun hitTest(point: CValue<CGPoint>): NSView? = null
            }

            init {
                view.wantsLayer = true
                view.layer = layer
                clipView.wantsLayer = true
                clipView.layer?.masksToBounds = true
                clipView.setAccessibilityElement(false)
                clipView.setAccessibilityChildren(emptyList<Any>())
                clipView.addSubview(view)
            }
        }
    }
    DisposableEffect(holder, updates) {
        updates.schedule {
            container.addSubview(holder.clipView, positioned = NSWindowBelow, relativeTo = null)
            currentOnCommand(PlayerCommand.Attach(holder))
        }
        onDispose {
            updates.schedule {
                currentOnCommand(PlayerCommand.Detach(holder))
                holder.clipView.removeFromSuperview()
            }
        }
    }
    Box(modifier.onGloballyPositioned { coordinates ->
        // Crop the wrapper while preserving the full
        // native view size. Resizing the decoder output to the visible fragment stretches video.
        val root = coordinates.findRootCoordinates()
        val full = root.localBoundingBoxOf(coordinates, clipBounds = false).roundToIntRect()
        val clipped = root.localBoundingBoxOf(coordinates, clipBounds = true).roundToIntRect()
        updates.schedule {
            val scale = density.toDouble()
            val height = container.bounds.useContents { size.height }
            val size = IntSize(full.width.coerceAtLeast(1), full.height.coerceAtLeast(1))
            CATransaction.begin()
            CATransaction.setDisableActions(true)
            try {
                holder.clipView.hidden = clipped.isEmpty
                holder.clipView.setFrame(
                    NSMakeRect(
                        clipped.left / scale, height - clipped.bottom / scale,
                        clipped.width / scale, clipped.height / scale,
                    )
                )
                // AppKit's unflipped coordinates have their origin at the bottom left.
                holder.view.setFrame(
                    NSMakeRect(
                        (full.left - clipped.left) / scale, (clipped.bottom - full.bottom) / scale,
                        full.width / scale, full.height / scale,
                    )
                )
                holder.layer.contentsScale = scale
                holder.layer.drawableSize =
                    CGSizeMake(size.width.toDouble(), size.height.toDouble())
            } finally {
                CATransaction.commit()
            }
            if (holder.lastSize != size) {
                holder.lastSize = size
                holder.onResize?.invoke(size.width, size.height)
            }
        }
    }.drawBehind {
        drawRect(Color.Transparent, blendMode = BlendMode.Clear)
    })
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
