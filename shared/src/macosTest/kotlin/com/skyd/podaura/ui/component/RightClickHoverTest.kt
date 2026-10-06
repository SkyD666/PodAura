package com.skyd.podaura.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.skyd.podaura.ext.onRightClickIfSupported
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.impl.use
import org.jetbrains.skiko.currentNanoTime
import platform.AppKit.NSApplication
import platform.AppKit.NSApplicationActivationPolicy
import platform.AppKit.NSEvent
import platform.AppKit.NSMouseMoved
import platform.AppKit.NSRightMouseDown
import platform.AppKit.NSRightMouseUp
import platform.AppKit.NSView
import platform.Foundation.NSMakePoint
import platform.Foundation.NSDate
import platform.Foundation.NSRunLoop
import platform.Foundation.dateWithTimeIntervalSinceNow
import platform.Foundation.runUntilDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.jetbrains.skia.Canvas as SkiaCanvas

class RightClickHoverTest {
    @Test
    fun nativeRightButtonPressPreservesTabHover() {
        val app = NSApplication.sharedApplication()
        app.setActivationPolicy(NSApplicationActivationPolicy.NSApplicationActivationPolicyRegular)
        app.finishLaunching()
        val source = MutableInteractionSource()
        val rightSource = MutableInteractionSource()
        var hovered = false
        var primaryPressed = false
        var rightPressed = false
        var rightClicks = 0
        val host = Window(size = DpSize(180.dp, 48.dp), transparent = true) {
            hovered = source.collectIsHoveredAsState().value
            primaryPressed = source.collectIsPressedAsState().value
            rightPressed = rightSource.collectIsPressedAsState().value
            MaterialTheme {
                PrimaryScrollableTabRow(
                    modifier = Modifier.fillMaxWidth(),
                    selectedTabIndex = 0,
                    edgePadding = 0.dp,
                ) {
                    Tab(
                        selected = true,
                        onClick = {},
                        modifier = Modifier.onRightClickIfSupported(
                            interactionSource = rightSource,
                            pass = PointerEventPass.Initial,
                            onClick = { rightClicks++ },
                        ).indication(
                            interactionSource = rightSource,
                            indication = LocalIndication.current,
                        ),
                        text = { Text("Media group") },
                        interactionSource = source,
                    )
                }
            }
        }
        var frameTime = currentNanoTime()
        fun renderPixel(): Int = Bitmap().use { bitmap ->
            NSRunLoop.mainRunLoop.runUntilDate(NSDate.dateWithTimeIntervalSinceNow(0.01))
            bitmap.allocN32Pixels(360, 96)
            SkiaCanvas(bitmap).use { canvas ->
                frameTime += 1_000_000_000L
                host.renderDelegate.onRender(canvas, 360, 96, frameTime)
            }
            bitmap.getColor(10, 10)
        }
        fun event(type: ULong): NSEvent = requireNotNull(NSEvent.mouseEventWithType(
            type = type,
            location = NSMakePoint(40.0, 24.0),
            modifierFlags = 0u,
            timestamp = 1.0,
            windowNumber = host.window.windowNumber,
            context = null,
            eventNumber = 1,
            clickCount = 1,
            pressure = 1.0f,
        ))
        try {
            val view = host.window.contentView!!.subviews.last() as NSView
            renderPixel()
            val idle = renderPixel()
            view.mouseMoved(event(NSMouseMoved))
            renderPixel()
            val hover = renderPixel()
            assertTrue(hovered, "Mouse movement should enter the native tab")
            assertNotEquals(idle, hover, "Hover should visibly highlight the native tab")
            view.rightMouseDown(event(NSRightMouseDown))
            renderPixel()
            assertTrue(hovered, "Pressing the right button should retain hover")
            assertTrue(rightPressed, "Pressing the right button should provide pressed feedback")
            assertEquals(false, primaryPressed, "A right-click must not enter the primary gesture")
            assertNotEquals(idle, renderPixel(), "Pressing the right button should retain a highlight")
            assertEquals(0, rightClicks)
            view.rightMouseUp(event(NSRightMouseUp))
            renderPixel()
            assertEquals(1, rightClicks)
            assertEquals(false, rightPressed)
        } finally {
            host.close()
        }
    }
}
