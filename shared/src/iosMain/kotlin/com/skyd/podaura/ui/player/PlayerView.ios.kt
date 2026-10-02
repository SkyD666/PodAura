package com.skyd.podaura.ui.player

import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.skyd.podaura.ui.PlatformSurfaceHolder
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.ui.player.component.state.PlayStateCallback
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import com.skyd.podaura.ui.player.service.PlayerState
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.AVFoundation.AVLayerVideoGravityResize
import platform.AVFoundation.AVSampleBufferDisplayLayer
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.QuartzCore.CATransaction
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationState
import platform.UIKit.UIApplicationWillResignActiveNotification
import platform.UIKit.UIColor
import platform.UIKit.UIScreen
import platform.UIKit.UIView
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicInt

private class IosVideoView(
    private val onCommand: (PlayerCommand) -> Unit,
    private val releaseVideo: () -> Unit,
) :
    UIView(CGRectZero.readValue()) {
    private val display = AVSampleBufferDisplayLayer()
    private val pixelWidth = AtomicInt(0)
    private val pixelHeight = AtomicInt(0)
    private val renderingActive = AtomicBoolean(false)
    private val holder = object : PlatformSurfaceHolder {
        override val isActive get() = renderingActive.load()
        override val layer = display
        override val width get() = pixelWidth.load()
        override val height get() = pixelHeight.load()
    }
    private var attached = false
    private var lastSize: Pair<Double, Double>? = null
    private val notificationsCenter = NSNotificationCenter.defaultCenter
    private val notifications = listOf(
        notificationsCenter.addObserverForName(
            UIApplicationWillResignActiveNotification,
            null,
            NSOperationQueue.mainQueue
        ) { detach() },
        notificationsCenter.addObserverForName(
            UIApplicationDidBecomeActiveNotification,
            null,
            NSOperationQueue.mainQueue
        ) { attach() },
    )

    init {
        backgroundColor = UIColor.blackColor
        display.videoGravity = AVLayerVideoGravityResize
        display.opaque = true
        layer.addSublayer(display)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        display.frame = bounds
        val scale = window?.screen?.scale ?: UIScreen.mainScreen.scale
        display.contentsScale = scale
        bounds.useContents {
            pixelWidth.store((size.width * scale).toInt())
            pixelHeight.store((size.height * scale).toInt())
        }
        CATransaction.commit()
        val currentSize = bounds.useContents { size.width to size.height }
        if (lastSize != currentSize) {
            lastSize = currentSize
            // Reattach requests a redraw even while playback is paused.
            detach()
        }
        if (UIApplication.sharedApplication.applicationState == UIApplicationState.UIApplicationStateActive) attach()
    }

    private fun attach() {
        if (!attached && window != null) {
            attached = true
            renderingActive.store(true)
            onCommand(PlayerCommand.Attach(holder))
        }
    }

    private fun detach() {
        if (attached) {
            attached = false
            renderingActive.store(false)
            // Drain GPU work synchronously; the coordinator processes its bookkeeping asynchronously.
            releaseVideo()
            onCommand(PlayerCommand.Detach(holder))
        }
    }

    fun release() {
        detach(); notifications.forEach(notificationsCenter::removeObserver)
    }
}

@Composable
actual fun PlatformPlayerView(
    coordinator: PlayerCoordinator,
    modifier: Modifier,
    onCommand: (PlayerCommand) -> Unit
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(focus) { focus.requestFocus() }
    UIKitView(
        factory = { IosVideoView(onCommand) { coordinator.renderPlayer.detachSurface() } },
        modifier = modifier.onPreviewKeyEvent(coordinator::onKey).focusRequester(focus).focusable(),
        onRelease = { it.release() },
        properties = UIKitInteropProperties(
            interactionMode = null,
            isNativeAccessibilityEnabled = false
        ),
    )
}

// System audio and app lifecycle outlive the visible player and belong to IosMediaSession.
@Composable
actual fun PlatformPlayerLifecycleEffect(coordinator: PlayerCoordinator) = Unit

@Composable
actual fun PlatformContent(
    modifier: Modifier, onBack: () -> Unit, coordinator: PlayerCoordinator,
    playerState: PlayerState, playState: PlayState, playStateCallback: PlayStateCallback,
    commonContent: @Composable () -> Unit
) = commonContent()
