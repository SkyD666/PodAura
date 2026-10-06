package com.skyd.podaura.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.impl.use
import org.jetbrains.skia.Canvas as SkiaCanvas
import org.jetbrains.skiko.currentNanoTime
import platform.AppKit.NSApplication
import platform.AppKit.NSApplicationActivationPolicy
import platform.AppKit.NSModalPanelRunLoopMode
import platform.AppKit.NSSavePanel
import platform.Foundation.NSRunLoop
import platform.Foundation.NSTimer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ComposeWindowTest {
    @Test fun retainsLastFrameDuringReentrantRenderingAndFileKitModal() {
        val app = NSApplication.sharedApplication()
        app.setActivationPolicy(NSApplicationActivationPolicy.NSApplicationActivationPolicyRegular)
        app.finishLaunching()
        var color by mutableStateOf(Color.Red)
        var drawCount = 0
        var onDraw: () -> Unit = {}
        val host = Window(transparent = true) {
            Canvas(Modifier.fillMaxSize()) {
                drawCount++
                onDraw()
                drawRect(color)
            }
        }
        fun renderPixel(): Int = Bitmap().use { bitmap ->
            bitmap.allocN32Pixels(32, 32)
            SkiaCanvas(bitmap).use { canvas ->
                host.renderDelegate.onRender(canvas, 32, 32, currentNanoTime())
            }
            bitmap.getColor(16, 16)
        }
        try {
            assertEquals(org.jetbrains.skia.Color.RED, renderPixel())
            var nestedPixel: Int? = null
            color = Color.Blue
            onDraw = { nestedPixel = renderPixel() }
            assertEquals(org.jetbrains.skia.Color.BLUE, renderPixel())
            assertEquals(org.jetbrains.skia.Color.RED, nestedPixel)
            onDraw = {}

            var modalPixel: Int? = null
            var modalDrawCount: Int? = null
            var modalFailure: Throwable? = null
            val beforeModal = drawCount
            val timer = NSTimer.timerWithTimeInterval(0.1, repeats = false) {
                try {
                    color = Color.Green
                    modalPixel = renderPixel()
                    modalDrawCount = drawCount
                } catch (error: Throwable) {
                    modalFailure = error
                } finally {
                    (app.modalWindow as? NSSavePanel)?.cancel(null)
                }
            }
            NSRunLoop.mainRunLoop.addTimer(timer, NSModalPanelRunLoopMode)
            try {
                assertNull(runBlocking { FileKit.openDirectoryPicker() })
            } finally {
                timer.invalidate()
            }
            modalFailure?.let { throw it }
            assertEquals(org.jetbrains.skia.Color.BLUE, modalPixel)
            assertEquals(beforeModal, modalDrawCount, "modal rendering must not reenter Compose")
            assertEquals(org.jetbrains.skia.Color.GREEN, renderPixel())
        } finally {
            host.close()
        }
    }
}
