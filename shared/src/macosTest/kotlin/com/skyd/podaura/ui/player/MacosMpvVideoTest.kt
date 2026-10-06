package com.skyd.podaura.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clipToBounds
import com.skyd.podaura.ui.PlatformSurfaceHolder
import kotlinx.cinterop.useContents
import platform.QuartzCore.CALayer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.skyd.podaura.ui.component.Window
import com.skyd.podaura.ui.player.mpv.MPV
import com.skyd.podaura.ui.player.mpv.resizeSurface
import kotlinx.cinterop.toKString
import platform.AppKit.*
import platform.Foundation.*
import platform.posix.getenv
import kotlin.test.*

/** Run with platform/macos/playback-smoke/run.py; uses the production window and video surface. */
class MacosMpvVideoTest {
    @Test fun sampleBufferVideoUnderCompose() {
        val path = getenv("PODAURA_MPV_TEST_VIDEO")?.toKString() ?: run {
            println("SKIP video smoke: set PODAURA_MPV_TEST_VIDEO or run the smoke script")
            return
        }
        val checkpointSeconds = getenv("PODAURA_MPV_CHECKPOINT_SECONDS")?.toKString()?.toDoubleOrNull() ?: 2.0
        val app = NSApplication.sharedApplication()
        app.setActivationPolicy(NSApplicationActivationPolicy.NSApplicationActivationPolicyRegular)
        app.finishLaunching()
        val player = MPV()
        player.option("config", "no")
        player.option("terminal", "yes")
        player.option("msg-level", "all=v")
        player.option("keep-open", "yes")
        player.option("hwdec", "auto")
        player.initialize()
        player.setPropertyBoolean("mute", true)
        var mounted by mutableStateOf(true)
        var generation by mutableStateOf(0)
        var videoOffset by mutableStateOf(0.dp)
        var attached: PlatformSurfaceHolder? = null
        val host = Window(title = "PodAura mpv smoke", size = DpSize(640.dp, 420.dp), transparent = true) {
            Box(Modifier.fillMaxSize().background(Color.DarkGray)) {
                Box(Modifier.fillMaxSize().padding(30.dp).clipToBounds()) {
                    if (mounted) key(generation) { MacosVideoSurface(Modifier.fillMaxSize().offset(y = videoOffset)) { command ->
                        when (command) {
                            is PlayerCommand.Attach -> {
                                attached = command.surfaceHolder
                                command.surfaceHolder.onResize = { width, height ->
                                    player.resizeSurface(width, height)
                                }
                                player.attachSurface(command.surfaceHolder)
                                player.setPropertyString("vo", "gpu-next")
                            }
                            is PlayerCommand.Detach -> {
                                command.surfaceHolder.onResize = null
                                if (attached === command.surfaceHolder) {
                                    player.detachSurface()
                                    attached = null
                                }
                            }
                            else -> Unit
                        }
                    } }
                }
                // A bright Compose marker must remain visible above the moving test pattern.
                Box(Modifier.padding(50.dp).size(55.dp).background(Color.Magenta))
            }
        }
        try {
            app.activateIgnoringOtherApps(true)
            pump(1.0)
            player.command("loadfile", path)
            val deadline = NSDate.dateWithTimeIntervalSinceNow(10.0)
            while (player.getPropertyDouble("time-pos") < 0.6 && deadline.timeIntervalSinceNow > 0) pump(0.1)
            assertEquals("coreaudio", player.getPropertyString("current-ao"))
            assertEquals("gpu-next", player.getPropertyString("current-vo"))
            assertTrue(player.getPropertyDouble("time-pos") > 0.5, "playback time must advance")
            assertTrue(player.getPropertyInt("video-out-params/w") > 0, "video output must have dimensions")
            getenv("PODAURA_MPV_TEST_SUBTITLE")?.toKString()?.let { subtitle ->
                player.command("sub-add", subtitle, "select")
                pump(1.0)
                assertTrue(player.getPropertyInt("sid") > 0)
                assertEquals("sub", player.getPropertyString("track-list/2/type"))
                player.setPropertyString("sid", "no")
                assertEquals("no", player.getPropertyString("sid"))
                player.setPropertyString("sid", "auto")
            }
            assertTrue(player.getPropertyInt("aid") > 0)
            player.setPropertyString("aid", "no")
            assertEquals("no", player.getPropertyString("aid"))
            player.setPropertyString("aid", "auto")
            println("SMOKE playing window=${host.window.windowNumber}")
            pump(checkpointSeconds)
            player.setPropertyBoolean("pause", true)
            player.command("seek", "1", "absolute+exact")
            host.window.setContentSize(platform.Foundation.NSMakeSize(900.0, 500.0))
            pump(2.0)
            assertTrue(player.getPropertyBoolean("pause"))
            assertTrue(player.getPropertyDouble("time-pos") in 0.8..1.2, "paused seek must reach one second")
            val viewport = requireNotNull(attached)
            assertEquals(viewport.width, player.getPropertyInt("osd-dimensions/w"),
                "macOS must render into the viewport rather than a media-aspect buffer")
            assertEquals(viewport.height, player.getPropertyInt("osd-dimensions/h"))
            player.setPropertyDouble("video-zoom", 1.0)
            player.setPropertyDouble("video-pan-x", 0.2)
            player.setPropertyDouble("video-pan-y", 0.1)
            pump(0.5)
            assertEquals(viewport.width, player.getPropertyInt("osd-dimensions/w"))
            assertEquals(viewport.height, player.getPropertyInt("osd-dimensions/h"))
            assertTrue(viewport.layer.readyForDisplay)
            println("SMOKE zoomed-panned window=${host.window.windowNumber}")
            pump(checkpointSeconds)
            player.setPropertyDouble("video-zoom", 0.0)
            player.setPropertyDouble("video-pan-x", 0.0)
            player.setPropertyDouble("video-pan-y", 0.0)
            pump(0.5)
            println("SMOKE paused-resized window=${host.window.windowNumber}")
            pump(checkpointSeconds)
            val layer = requireNotNull(attached).layer
            val videoView = requireNotNull(findView(requireNotNull(host.window.contentView), layer))
            val cropView = requireNotNull(videoView.superview)
            val fullSize = videoView.bounds.useContents { size.width to size.height }
            assertEquals("podaura", player.getPropertyString("gpu-context"))
            assertNull(player.getPropertyString("macos-surface-size"))
            assertNull(player.getPropertyString("android-surface-size"))
            videoOffset = (-80).dp
            pump(0.5)
            assertEquals(fullSize, videoView.bounds.useContents { size.width to size.height }, "clipping must not resize video")
            assertEquals(fullSize.second - 80.0, cropView.frame.useContents { size.height }, 1.0)
            assertEquals(fullSize.second, videoView.frame.useContents { size.height }, 1.0)
            assertFalse(cropView.hidden)
            println("SMOKE clipped window=${host.window.windowNumber}")
            pump(checkpointSeconds)
            videoOffset = (-1000).dp
            pump(0.5)
            assertTrue(cropView.hidden, "fully clipped video must be hidden")
            videoOffset = 0.dp
            pump(0.5)
            assertFalse(cropView.hidden)
            assertEquals(fullSize, videoView.bounds.useContents { size.width to size.height })
            assertTrue(player.getPropertyDouble("time-pos") in 0.8..1.2, "clipping must preserve the paused session")
            val previousHolder = requireNotNull(attached)
            val replacement = object : PlatformSurfaceHolder {
                override val layer = platform.AVFoundation.AVSampleBufferDisplayLayer()
                override val width = previousHolder.width
                override val height = previousHolder.height
                override var onResize: ((Int, Int) -> Unit)? = null
            }
            videoView.layer = replacement.layer
            player.attachSurface(replacement)
            pump(1.0)
            assertTrue(replacement.layer.readyForDisplay,
                "a live renderer must redraw into its replacement layer while paused")
            assertTrue(player.getPropertyBoolean("pause"))
            videoView.layer = previousHolder.layer
            player.attachSurface(previousHolder)
            pump(0.5)
            val previousLayer = previousHolder.layer
            generation++
            pump(1.0)
            assertNotSame(previousLayer, requireNotNull(attached).layer)
            assertTrue(requireNotNull(attached).layer.readyForDisplay,
                "replacing the interop view while paused must present the current frame")
            assertTrue(player.getPropertyDouble("time-pos") in 0.8..1.2)
            println("SMOKE replaced-paused window=${host.window.windowNumber}")
            mounted = false
            pump(1.0)
            assertEquals("null", player.getPropertyString("vo"))
            mounted = true
            pump(2.0)
            player.setPropertyDouble("speed", 1.5)
            player.setPropertyBoolean("pause", false)
            pump(1.0)
            assertEquals("gpu-next", player.getPropertyString("current-vo"))
            assertEquals(1.5, player.getPropertyDouble("speed"))
            println("SMOKE remounted window=${host.window.windowNumber}")
            pump(checkpointSeconds)
        } finally {
            host.close()
            player.destroy()
        }
    }

    private fun findView(root: NSView, layer: CALayer): NSView? {
        if (root.layer == layer) return root
        return root.subviews.filterIsInstance<NSView>().firstNotNullOfOrNull { findView(it, layer) }
    }

    private fun pump(seconds: Double) {
        val end = NSDate.dateWithTimeIntervalSinceNow(seconds)
        while (end.timeIntervalSinceNow > 0) {
            NSRunLoop.mainRunLoop.runUntilDate(NSDate.dateWithTimeIntervalSinceNow(0.01))
            val app = NSApplication.sharedApplication()
            while (true) {
                val event = app.nextEventMatchingMask(ULong.MAX_VALUE, untilDate = NSDate.date(), inMode = NSDefaultRunLoopMode, dequeue = true) ?: break
                app.sendEvent(event)
            }
        }
    }
}
