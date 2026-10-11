package com.skyd.podaura.ui.component

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Slider
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.skyd.podaura.ext.hideCursorIfSupported
import com.skyd.podaura.ui.player.PressAndHoldSpeedController
import com.skyd.podaura.ui.player.PressAndHoldSpeedSource
import com.skyd.podaura.ui.window.PlayerRightArrowKeyHandler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.cinterop.useContents
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.impl.use
import org.jetbrains.skiko.currentNanoTime
import platform.AppKit.NSApplication
import platform.AppKit.NSApplicationActivationPolicy
import platform.AppKit.NSCursor
import platform.AppKit.NSEvent
import platform.AppKit.NSKeyDown
import platform.AppKit.NSKeyUp
import platform.AppKit.NSMouseMoved
import platform.AppKit.NSView
import platform.Foundation.NSMakePoint
import platform.Foundation.NSMakeSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.jetbrains.skia.Canvas as SkiaCanvas

class PlayerInputTest {
    @Test
    fun rightArrowHoldYieldsToDialogTextAndSlider() = runTest {
        initializeApp()
        var speed = 1.5f
        val seeks = mutableListOf<Long>()
        var sceneKeys = 0
        var fallbackKeys = 0
        val speedController = PressAndHoldSpeedController({ speed }, { speed = it })
        val handler = PlayerRightArrowKeyHandler(
            coroutineScope = this,
            longPressTimeoutMillis = 500,
            rapidTapTimeoutMillis = 300,
            mediaStarted = { true },
            currentMediaPath = { "media" },
            currentPosition = { 100 },
            forwardSeconds = { 10 },
            onSeekTo = { seeks += it },
            onLongPressStart = { speedController.start(PressAndHoldSpeedSource.Keyboard) },
            onLongPressEnd = { speedController.stop(PressAndHoldSpeedSource.Keyboard) },
        )
        val focus = FocusRequester()
        val dialogFocus = FocusRequester()
        var dialog by mutableStateOf(0)
        var text by mutableStateOf(TextFieldValue("100", TextRange.Zero))
        var slider by mutableStateOf(1f)
        val host = Window(
            onKeyEvent = { fallbackKeys++; true },
        ) {
            DisposableEffect(handler) {
                onPreviewKeyEvent = handler::onKeyEvent
                onOverlayFocus = { handler.cancel() }
                onDispose {
                    onPreviewKeyEvent = null
                    onOverlayFocus = null
                    handler.cancel()
                }
            }
            Box(Modifier.fillMaxSize().onPreviewKeyEvent { sceneKeys++; true }
                .focusRequester(focus).focusable())
            LaunchedEffect(Unit) { focus.requestFocus() }
            if (dialog != 0) {
                Dialog(
                    onDismissRequest = { dialog = 0 },
                    properties = DialogProperties(animateTransition = false),
                ) {
                    if (dialog == 1) {
                        BasicTextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier.focusRequester(dialogFocus),
                        )
                    } else {
                        Slider(
                            value = slider,
                            onValueChange = { slider = it },
                            valueRange = 0.25f..3f,
                            modifier = Modifier.focusRequester(dialogFocus),
                        )
                    }
                    LaunchedEffect(dialog) { dialogFocus.requestFocus() }
                }
            }
        }
        fun keyEvent(type: ULong, repeat: Boolean = false) = requireNotNull(
            NSEvent.keyEventWithType(
                type = type,
                location = NSMakePoint(0.0, 0.0),
                modifierFlags = 0u,
                timestamp = 1.0,
                windowNumber = host.window.windowNumber,
                context = null,
                characters = "\uF703",
                charactersIgnoringModifiers = "\uF703",
                isARepeat = repeat,
                keyCode = 124u,
            )
        )
        try {
            host.renderFrame()
            val view = host.window.contentView!!.subviews.last() as NSView
            view.keyDown(keyEvent(NSKeyDown))
            advanceTimeBy(500)
            runCurrent()
            view.keyDown(keyEvent(NSKeyDown, repeat = true))
            assertEquals(3f, speed)
            assertEquals(emptyList(), seeks)
            view.keyUp(keyEvent(NSKeyUp))
            assertEquals(1.5f, speed)
            view.keyDown(keyEvent(NSKeyDown))
            view.keyUp(keyEvent(NSKeyUp))
            assertEquals(listOf(110L), seeks)
            assertEquals(0, sceneKeys)
            assertEquals(0, fallbackKeys)

            // Opening a dialog during an active hold must restore playback immediately.
            view.keyDown(keyEvent(NSKeyDown))
            advanceTimeBy(500)
            runCurrent()
            assertEquals(3f, speed)
            dialog = 1
            host.renderFrame()
            assertEquals(1.5f, speed)
            view.keyUp(keyEvent(NSKeyUp))
            view.keyDown(keyEvent(NSKeyDown))
            advanceTimeBy(500)
            runCurrent()
            view.keyUp(keyEvent(NSKeyUp))
            assertEquals(TextRange(1), text.selection)
            assertEquals(1.5f, speed)
            assertEquals(listOf(110L), seeks)

            dialog = 2
            host.renderFrame()
            assertTrue(dialogFocus.requestFocus(), "The dialog slider must own keyboard focus")
            host.renderFrame()
            view.keyDown(keyEvent(NSKeyDown))
            advanceTimeBy(500)
            runCurrent()
            view.keyUp(keyEvent(NSKeyUp))
            assertTrue(slider > 1f, "Right must adjust the focused dialog slider")
            assertEquals(1.5f, speed)
            assertEquals(listOf(110L), seeks)

            dialog = 0
            host.renderFrame()
            view.keyDown(keyEvent(NSKeyDown))
            view.keyUp(keyEvent(NSKeyUp))
            assertEquals(listOf(110L, 110L), seeks)
            assertEquals(0, fallbackKeys, "Dialog input must not reach the player fallback")
        } finally {
            handler.cancel()
            host.close()
        }
        assertNull(host.onPreviewKeyEvent)
        assertNull(host.onOverlayFocus)
    }

    @Test
    fun nativeTitlebarDoesNotHitFullscreenPlayerContent() {
        initializeApp()
        val host = Window(size = DpSize(400.dp, 300.dp)) {
            Box(Modifier.fillMaxSize())
        }
        fun assertTitlebarBelongsToAppKit() {
            val container = requireNotNull(host.window.contentView)
            val composeView = container.subviews.last() as NSView
            val contentTop = host.window.contentLayoutRect.useContents { origin.y + size.height }
            val viewTop = container.bounds.useContents { size.height }
            val centerX = container.bounds.useContents { size.width / 2 }
            assertTrue(viewTop > contentTop, "The transparent titlebar must overlap the drawing area")
            val titlebarPoint = NSMakePoint(centerX, (contentTop + viewTop) / 2)
            assertNull(container.hitTest(titlebarPoint), "Titlebar clicks must bypass player gestures")
            assertNotEquals(composeView, container.superview?.hitTest(titlebarPoint))
            assertSame(composeView, container.hitTest(NSMakePoint(centerX, contentTop - 8)))
        }
        try {
            assertTitlebarBelongsToAppKit()
            host.window.setContentSize(NSMakeSize(800.0, 600.0))
            assertTitlebarBelongsToAppKit()
        } finally {
            host.close()
        }
    }

    @Test
    fun stationaryCursorHidesAndRestoresWithController() {
        initializeApp()
        var hide by mutableStateOf(false)
        val host = Window(size = DpSize(100.dp, 100.dp)) {
            Box(Modifier.fillMaxSize().hideCursorIfSupported(hide))
        }
        try {
            host.renderFrame()
            val view = host.window.contentView!!.subviews.last() as NSView
            view.mouseMoved(requireNotNull(NSEvent.mouseEventWithType(
                type = NSMouseMoved,
                location = NSMakePoint(40.0, 40.0),
                modifierFlags = 0u,
                timestamp = 1.0,
                windowNumber = host.window.windowNumber,
                context = null,
                eventNumber = 1,
                clickCount = 0,
                pressure = 0f,
            )))
            host.renderFrame()
            assertEquals(NSCursor.arrowCursor, NSCursor.currentCursor)
            hide = true
            host.renderFrame()
            assertNotEquals(NSCursor.arrowCursor, NSCursor.currentCursor)
            hide = false
            host.renderFrame()
            assertEquals(NSCursor.arrowCursor, NSCursor.currentCursor)
        } finally {
            host.close()
            NSCursor.arrowCursor.set()
        }
    }
}

private fun initializeApp() {
    NSApplication.sharedApplication().apply {
        setActivationPolicy(NSApplicationActivationPolicy.NSApplicationActivationPolicyRegular)
        finishLaunching()
    }
}

private fun ComposeWindow.renderFrame() = Bitmap().use { bitmap ->
    bitmap.allocN32Pixels(200, 200)
    SkiaCanvas(bitmap).use { canvas ->
        renderDelegate.onRender(canvas, 200, 200, currentNanoTime())
    }
}
