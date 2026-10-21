package com.skyd.podaura.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clipToBounds
import com.skyd.podaura.ui.PlatformSurfaceHolder
import com.skyd.podaura.libmpv.podaura_output_frame_count
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.useContents
import platform.QuartzCore.CALayer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.skyd.podaura.ui.component.Window
import kotlinx.cinterop.toKString
import platform.AppKit.*
import com.skyd.podaura.libmpv.podaura_output_flush
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.objcPtr
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
        withMacosPlayerSession(backgroundPlay = true) { session ->
            val app = NSApplication.sharedApplication()
            session.open(PlayerOpenRequest.Files(listOf(io.github.vinceglb.filekit.PlatformFile(path)), showPlayer = false))
            awaitPlayer("headless video ready") { session.coordinator?.playerState?.value?.isVideo == true }
            val pip = requireNotNull(session.pictureInPicture)
            val player = requireNotNull(session.coordinator).renderPlayer
            player.setPropertyBoolean("mute", true)
            assertFalse(player.hasRenderedVideoFrame(), "headless playback must not create a renderer")
            var mounted by mutableStateOf(true)
            var generation by mutableStateOf(0)
            var videoOffset by mutableStateOf(0.dp)
            val host = Window(title = "PodAura mpv smoke", size = DpSize(640.dp, 420.dp), transparent = true) {
                Box(Modifier.fillMaxSize().background(Color.DarkGray)) {
                    Box(Modifier.fillMaxSize().padding(30.dp).clipToBounds()) {
                        if (mounted) key(generation) {
                            MacosVideoSurface(Modifier.fillMaxSize().offset(y = videoOffset), pip)
                        }
                    }
                    // A bright Compose marker must remain visible above the moving test pattern.
                    Box(Modifier.padding(50.dp).size(55.dp).background(Color.Magenta))
                }
            }
            try {
                app.activateIgnoringOtherApps(true)
                pumpPlayer(1.0)
                val deadline = NSDate.dateWithTimeIntervalSinceNow(10.0)
                while (player.getPropertyDouble("time-pos") < 0.6 && deadline.timeIntervalSinceNow > 0) pumpPlayer(0.1)
                assertEquals("coreaudio", player.getPropertyString("current-ao"))
                assertEquals("gpu-next", player.getPropertyString("current-vo"))
                assertTrue(player.getPropertyDouble("time-pos") > 0.5, "playback time must advance")
                assertTrue(player.getPropertyInt("video-out-params/w") > 0, "video output must have dimensions")
                getenv("PODAURA_MPV_TEST_SUBTITLE")?.toKString()?.let { subtitle ->
                    player.command("sub-add", subtitle, "select")
                    pumpPlayer(1.0)
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
                pumpPlayer(checkpointSeconds)
                player.setPropertyBoolean("pause", true)
                player.command("seek", "1", "absolute+exact")
                host.window.setContentSize(platform.Foundation.NSMakeSize(900.0, 500.0))
                pumpPlayer(2.0)
                assertTrue(player.getPropertyBoolean("pause"))
                assertTrue(player.getPropertyDouble("time-pos") in 0.8..1.2, "paused seek must reach one second")
                val viewport = pip.holder
                assertEquals(viewport.width, player.getPropertyInt("osd-dimensions/w"),
                    "macOS must render into the viewport rather than a media-aspect buffer")
                assertEquals(viewport.height, player.getPropertyInt("osd-dimensions/h"))
                player.setPropertyDouble("video-zoom", 1.0)
                player.setPropertyDouble("video-pan-x", 0.2)
                player.setPropertyDouble("video-pan-y", 0.1)
                pumpPlayer(0.5)
                assertEquals(viewport.width, player.getPropertyInt("osd-dimensions/w"))
                assertEquals(viewport.height, player.getPropertyInt("osd-dimensions/h"))
                assertTrue(viewport.layer.readyForDisplay)
                println("SMOKE zoomed-panned window=${host.window.windowNumber}")
                pumpPlayer(checkpointSeconds)
                player.setPropertyDouble("video-zoom", 0.0)
                player.setPropertyDouble("video-pan-x", 0.0)
                player.setPropertyDouble("video-pan-y", 0.0)
                pumpPlayer(0.5)
                println("SMOKE paused-resized window=${host.window.windowNumber}")
                pumpPlayer(checkpointSeconds)
                val layer = pip.holder.layer
                val videoView = requireNotNull(findView(requireNotNull(host.window.contentView), layer))
                val cropView = requireNotNull(videoView.superview)
                val fullSize = videoView.bounds.useContents { size.width to size.height }
                assertEquals("podaura", player.getPropertyString("gpu-context"))
                assertNull(player.getPropertyString("macos-surface-size"))
                assertNull(player.getPropertyString("android-surface-size"))
                videoOffset = (-80).dp
                pumpPlayer(0.5)
                assertEquals(fullSize, videoView.bounds.useContents { size.width to size.height }, "clipping must not resize video")
                assertEquals(fullSize.second - 80.0, cropView.frame.useContents { size.height }, 1.0)
                assertEquals(fullSize.second, videoView.frame.useContents { size.height }, 1.0)
                assertFalse(cropView.hidden)
                println("SMOKE clipped window=${host.window.windowNumber}")
                pumpPlayer(checkpointSeconds)
                videoOffset = (-1000).dp
                pumpPlayer(0.5)
                assertTrue(cropView.hidden, "fully clipped video must be hidden")
                videoOffset = 0.dp
                pumpPlayer(0.5)
                assertFalse(cropView.hidden)
                assertEquals(fullSize, videoView.bounds.useContents { size.width to size.height })
                assertTrue(player.getPropertyDouble("time-pos") in 0.8..1.2, "clipping must preserve the paused session")
                // Exercise the session-owned output; replacing its holder behind the coordinator
                // would bypass the production surface lifecycle this regression is meant to test.
                val retainedLayer = pip.layer
                player.setRenderingActive(false)
                podaura_output_flush(interpretCPointer<ByteVar>(retainedLayer.objcPtr()), true)
                pumpPlayer(0.2)
                assertFalse(retainedLayer.readyForDisplay)
                player.setRenderingActive(true)
                pumpPlayer(0.5)
                assertTrue(retainedLayer.readyForDisplay,
                    "reactivating a retained output must redraw even while playback is paused")
                assertTrue(player.getPropertyBoolean("pause"))
                val retainedOutput = player.getPropertyString("wid")
                player.setRenderingActive(false, suspendVideoDecoding = true)
                assertEquals("no", player.getPropertyString("vid"))
                assertEquals("gpu-next", player.getPropertyString("current-vo"))
                val output = requireNotNull(retainedOutput).toLong().toCPointer<ByteVar>()
                val framesBeforePreparation = podaura_output_frame_count(output)
                player.setRenderingActive(false)
                pumpPlayer(0.3)
                assertTrue(player.getPropertyInt("vid") > 0)
                assertEquals(framesBeforePreparation, podaura_output_frame_count(output),
                    "foreground preparation must restore decoding without submitting GPU frames")
                player.setRenderingActive(true, suspendVideoDecoding = true)
                pumpPlayer(0.5)
                assertEquals(retainedOutput, player.getPropertyString("wid"))
                assertTrue(player.getPropertyInt("vid") > 0)
                assertTrue(retainedLayer.readyForDisplay)
                assertTrue(player.getPropertyBoolean("pause"))
                val previousLayer = pip.layer
                generation++
                pumpPlayer(1.0)
                assertSame(previousLayer, pip.holder.layer, "the session owns the same source across view recreation")
                assertTrue(pip.holder.layer.readyForDisplay,
                    "replacing the interop view while paused must present the current frame")
                assertTrue(player.getPropertyDouble("time-pos") in 0.8..1.2)
                println("SMOKE replaced-paused window=${host.window.windowNumber}")
                mounted = false
                pumpPlayer(1.0)
                assertEquals("null", player.getPropertyString("vo"))
                player.setRenderingActive(false, suspendVideoDecoding = true)
                player.setRenderingActive(false)
                assertEquals("no", player.getPropertyString("vid"),
                    "foreground preparation must preserve headless video suspension")
                player.setRenderingActive(false, suspendVideoDecoding = true)
                player.setRenderingActive(true, suspendVideoDecoding = true)
                pumpPlayer(0.3)
                assertEquals("no", player.getPropertyString("vid"),
                    "foreground activation must preserve headless video suspension")
                assertEquals("null", player.getPropertyString("vo"))
                assertNull(player.getPropertyString("video-format"))
                assertFalse(player.getPropertyBoolean("eof-reached"))
                mounted = true
                pumpPlayer(2.0)
                player.setPropertyDouble("speed", 1.5)
                player.setPropertyBoolean("pause", false)
                pumpPlayer(1.0)
                assertEquals("gpu-next", player.getPropertyString("current-vo"))
                assertEquals(1.5, player.getPropertyDouble("speed"))
                println("SMOKE remounted window=${host.window.windowNumber}")
                pumpPlayer(checkpointSeconds)
            } finally {
                host.close()
            }
        }
    }

    private fun findView(root: NSView, layer: CALayer): NSView? {
        if (root.layer == layer || root.layer?.sublayers?.contains(layer) == true) return root
        return root.subviews.filterIsInstance<NSView>().firstNotNullOfOrNull { findView(it, layer) }
    }

}
