package com.skyd.podaura.ui.player.pip

import com.skyd.podaura.ui.player.withMacosPlayerSession
import com.skyd.podaura.ui.player.awaitPlayer
import com.skyd.podaura.ui.player.pumpPlayer
import com.skyd.podaura.ui.player.PlayerCommand
import com.skyd.podaura.ui.player.PlayerOpenRequest
import com.skyd.podaura.ui.player.LoopMode
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.cinterop.toKString
import kotlinx.cinterop.readValue
import platform.CoreMedia.CMTimeMake
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.kCMTimeZero
import kotlinx.cinterop.useContents
import platform.AVFoundation.*
import platform.AppKit.*
import platform.Foundation.*
import platform.posix.getenv
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.skyd.fundation.di.get
import coil3.ImageLoader
import com.skyd.podaura.ui.player.media.loadAppleArtwork
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.addressOf
import kotlin.test.*

/** Opt-in real AVKit test, with an isolated preferences file and playback-history database.
 * PODAURA_PIP_TEST_VIDEO and PODAURA_PIP_TEST_AUDIO must point to local media lasting >30s.
 * scripts/test-macos-pip.sh generates moving color bars; the JVM playback fixture is pure black.
 * State assertions do not verify the system window's pixels: use the inspect checkpoints for visual QA.
 * Optional PODAURA_PIP_TEST_IMAGE exercises starting PiP directly from audio with square artwork.
 */
class MacosPictureInPictureTest {
    @Test
    fun unsupportedSystemVideoLeavesInlinePlaybackAvailable() {
        val path = getenv("PODAURA_PIP_TEST_UNSUPPORTED")?.toKString() ?: return
        withMacosPlayerSession { session ->
            session.open(PlayerOpenRequest.Files(listOf(PlatformFile(path))))
            val player = requireNotNull(session.coordinator)
            val pip = requireNotNull(session.pictureInPicture)
            player.renderPlayer.setPropertyBoolean("mute", true)
            awaitPlayer("mpv supports the test container") {
                pip.canEnter && player.playerState.value.isVideo && player.renderPlayer.hasRenderedVideoFrame()
            }
            player.onCommand(PlayerCommand.Paused(true))
            awaitPlayer("inline paused") { player.playerState.value.paused }
            pip.enter()
            awaitPlayer("unsupported system PiP reports an error") { pip.error != null }
            assertFalse(pip.keepsSession)
            assertTrue(session.isFullPlayerVisible)
            assertSame(player, session.coordinator)
            assertTrue(player.playerState.value.paused)
            assertTrue(pip.canEnter, "A failed attempt must not leave the session stuck starting")
            assertTrue(player.renderPlayer.hasRenderedVideoFrame())

            getenv("PODAURA_PIP_TEST_VIDEO")?.toKString()?.let { supported ->
                session.open(PlayerOpenRequest.Files(listOf(PlatformFile(supported))))
                awaitPlayer("supported video ready") {
                    player.playerState.value.path == supported && pip.canEnter
                }
                pip.enter()
                awaitPlayer("supported PiP started") { pip.active }
                NSApplication.sharedApplication().windows.filterIsInstance<NSWindow>()
                    .single { it.visible && it.title == "Player" }.performClose(null)
                session.open(PlayerOpenRequest.Files(listOf(PlatformFile(path)), showPlayer = false))
                awaitPlayer("unsupported next item returns to the player") {
                    pip.error != null && !pip.keepsSession && session.isFullPlayerVisible
                }
                assertSame(player, session.coordinator)
                awaitPlayer("mpv renders the unsupported system format") {
                    player.renderPlayer.hasRenderedVideoFrame()
                }
            }
        }
    }

    @Test
    fun nativeVideoEndDoesNotPauseMpvBeforePlaylistCompletion() {
        val path = getenv("PODAURA_PIP_TEST_VIDEO")?.toKString() ?: return
        withMacosPlayerSession { session ->
            session.open(PlayerOpenRequest.Files(listOf(PlatformFile(path))))
            val player = requireNotNull(session.coordinator)
            val pip = requireNotNull(session.pictureInPicture)
            player.renderPlayer.setPropertyBoolean("mute", true)
            awaitPlayer("video metadata") {
                pip.canEnter && player.playerState.value.seekable && player.playerState.value.duration > 2
            }
            player.onCommand(PlayerCommand.SetLoopMode(LoopMode.None))
            pip.enter()
            awaitPlayer("PiP started") { pip.active }
            player.onCommand(PlayerCommand.SeekTo(player.playerState.value.duration - 1))
            awaitPlayer("mpv reaches EOF") {
                player.renderPlayer.getPropertyBoolean("eof-reached")
            }
            awaitPlayer("native video pauses at EOF") { pip.nativePlayer.rate == 0f }
        }
    }

    @Test
    fun nativePipSurvivesWindowCloseAndMediaChanges() {
        val video = getenv("PODAURA_PIP_TEST_VIDEO")?.toKString() ?: run {
            println("SKIP PiP smoke: set PODAURA_PIP_TEST_VIDEO and PODAURA_PIP_TEST_AUDIO")
            return
        }
        val audio = requireNotNull(getenv("PODAURA_PIP_TEST_AUDIO")?.toKString())
        withMacosPlayerSession { session ->
            val app = NSApplication.sharedApplication()
            val existingWindows = app.windows.filterIsInstance<NSWindow>().toSet()
            assertTrue(supportPip, "This smoke test requires system PiP on macOS 12+")
            getenv("PODAURA_PIP_TEST_IMAGE")?.toKString()?.let { image ->
                session.open(PlayerOpenRequest.Files(listOf(PlatformFile(audio))))
                val pip = requireNotNull(session.pictureInPicture)
                val player = requireNotNull(session.coordinator)
                player.renderPlayer.setPropertyBoolean("mute", true)
                awaitPlayer("audio ready") { player.playerState.value.mediaStarted }
                player.onCommand(PlayerCommand.Paused(true))
                awaitPlayer("audio paused") { player.playerState.value.paused }
                var loaded = false
                CoroutineScope(Dispatchers.Main).launch {
                    val data = assertNotNull(loadAppleArtwork(image, get<ImageLoader>()))
                    val nativeImage = data.pngBytes.usePinned {
                        NSImage(data = NSData.create(it.addressOf(0), data.pngBytes.size.toULong()))
                    }.apply { setSize(NSMakeSize(1024.0, 1024.0)) }
                    pip.updateArtwork(nativeImage)
                    loaded = true
                }
                awaitPlayer("real artwork loader") { loaded && pip.canEnter }
                pip.enter()
                awaitPlayer("audio PiP started") { pip.active }
                assertNotNull(pip.nativePlayer.currentItem).presentationSize.useContents {
                    assertTrue(width > 0)
                    assertEquals(width, height, 1.0, "The native artwork track must retain its aspect ratio")
                }
                checkpoint("audio-cover")
                pip.nativePlayer.seekToTime(CMTimeMake(3, 1),
                    toleranceBefore = kCMTimeZero.readValue(), toleranceAfter = kCMTimeZero.readValue())
                awaitPlayer("audio cover PiP seeks audio") {
                    player.renderPlayer.getPropertyDouble("time-pos") >= 2.5
                }
                pip.restore()
                awaitPlayer("audio cover restored inline") { !pip.active && session.isFullPlayerVisible }
                assertTrue(player.playerState.value.paused)
                pip.updateArtwork(null)
            }
            session.open(PlayerOpenRequest.Files(listOf(PlatformFile(video))))
            app.activateIgnoringOtherApps(true)
            try {
                awaitPlayer("video ready for PiP") {
                    session.coordinator?.playerState?.value?.let {
                        it.path == video && it.isVideo && it.mediaStarted
                    } == true && session.pictureInPicture?.canEnter == true
                }
            } catch (failure: AssertionError) {
                val source = session.pictureInPicture
                println("PiP readiness: possible=${source?.possible}, native=${source?.controller?.pictureInPicturePossible}, " +
                        "display=${source?.layer?.readyForDisplay}, frame=${session.coordinator?.renderPlayer?.hasRenderedVideoFrame()}, " +
                        "engine=${session.coordinator?.engineState?.value}, media=${session.coordinator?.playerState?.value?.mediaStarted}")
                throw failure
            }
            val pip = requireNotNull(session.pictureInPicture)
            val player = requireNotNull(session.coordinator)
            player.renderPlayer.setPropertyBoolean("mute", true)
            val window = app.windows.filterIsInstance<NSWindow>().first { it !in existingWindows && it.visible }
            val controller = requireNotNull(pip.controller)
            val nativePlayer = pip.nativePlayer
            fun findInline(root: NSView): NSView? =
                if (root.layer == pip.layer.superlayer) root
                else root.subviews.filterIsInstance<NSView>().firstNotNullOfOrNull(::findInline)
            fun assertSourceFrame(inline: NSView) {
                val source = app.windows.filterIsInstance<NSWindow>()
                    .single { it.contentView?.layer == pip.pipLayer.superlayer }
                assertNotSame(inline.window, source, "PiP must use an isolated source window")
                val frame = requireNotNull(inline.window)
                    .convertRectToScreen(inline.convertRect(inline.bounds, toView = null))
                val actual = source.frame.useContents { listOf(origin.x, origin.y, size.width, size.height) }
                val expected = frame.useContents { listOf(origin.x, origin.y, size.width, size.height) }
                expected.zip(actual).forEach { (expectedValue, actualValue) ->
                    assertEquals(expectedValue, actualValue, 1.0, "PiP animation must target the inline screen rect")
                }
            }
            player.onCommand(PlayerCommand.Paused(true))
            awaitPlayer("paused before PiP") { player.playerState.value.paused }
            awaitPlayer("seekable metadata") {
                player.playerState.value.seekable && player.playerState.value.duration > 0
            }
            val before = player.playerState.value.position
            val inline = requireNotNull(findInline(requireNotNull(window.contentView)))
            window.setFrameOrigin(NSMakePoint(180.0, 160.0))
            pumpPlayer(0.1)
            pip.enter()
            assertSourceFrame(inline)
            awaitPlayer("PiP started") { pip.active || pip.error != null }
            assertNull(pip.error)
            assertTrue(pip.active)
            assertTrue(pip.contentReady)
            assertFalse(controller.requiresLinearPlayback)
            assertTrue(CMTimeGetSeconds(requireNotNull(nativePlayer.currentItem).duration) > 0)
            nativePlayer.play()
            awaitPlayer("native PiP resumes mpv") { !player.playerState.value.paused }
            nativePlayer.pause()
            awaitPlayer("native PiP pauses mpv") { player.playerState.value.paused }
            nativePlayer.seekToTime(CMTimeMake(3, 1),
                toleranceBefore = kCMTimeZero.readValue(), toleranceAfter = kCMTimeZero.readValue())
            awaitPlayer("native PiP seeks mpv") { player.renderPlayer.getPropertyDouble("time-pos") >= 2.5 }
            assertTrue(player.playerState.value.paused)
            player.onCommand(PlayerCommand.SeekTo(6))
            awaitPlayer("mpv seek reaches native PiP") {
                kotlin.math.abs(CMTimeGetSeconds(nativePlayer.currentTime()) - 6) < 0.5
            }
            nativePlayer.play()
            awaitPlayer("PiP resumes after seek") { !player.playerState.value.paused }
            player.onCommand(PlayerCommand.SetSpeed(1.5f))
            awaitPlayer("PiP follows playback speed") { nativePlayer.rate == 1.5f }
            nativePlayer.pause()
            awaitPlayer("PiP pauses at custom speed") { player.playerState.value.paused }
            nativePlayer.play()
            awaitPlayer("PiP resumes at custom speed") {
                !player.playerState.value.paused && nativePlayer.rate == 1.5f
            }
            player.onCommand(PlayerCommand.SetSpeed(1f))
            awaitPlayer("normal playback speed") { nativePlayer.rate == 1f }
            assertTrue(session.isFullPlayerVisible)
            val source = app.windows.filterIsInstance<NSWindow>().single {
                it.contentView?.layer == pip.pipLayer.superlayer
            }
            assertFalse(source.visible, "PiP must hide its source window on every display")
            checkpoint("video")
            assertFalse(pip.canEnter)
            window.performClose(null)
            pumpPlayer(0.5)
            assertFalse(session.isFullPlayerVisible)
            assertSame(player, session.coordinator, "PiP must override background-play=false")
            assertTrue(pip.keepsSession)
            awaitPlayer("playback continues without a window") { player.playerState.value.position > before }
            awaitPlayer("PiP video stays synchronized with mpv audio") {
                kotlin.math.abs(CMTimeGetSeconds(nativePlayer.currentTime()) -
                        player.renderPlayer.getPropertyDouble("time-pos")) < 0.25
            }
            checkpoint("video-headless")

            session.open(PlayerOpenRequest.Files(listOf(PlatformFile(audio)), showPlayer = false))
            awaitPlayer("audio artwork/background is displayed") {
                player.playerState.value.path == audio && player.playerState.value.mediaStarted &&
                        !player.playerState.value.isVideo && pip.contentReady
            }
            assertTrue(pip.active, "changing video to audio must retain system PiP")
            assertTrue(controller.pictureInPictureActive, "AVKit must remain active for audio")
            checkpoint("audio")
            getenv("PODAURA_PIP_TEST_COVER")?.toKString()?.let { cover ->
                session.open(PlayerOpenRequest.Files(listOf(PlatformFile(cover)), showPlayer = false))
                awaitPlayer("audio with artwork") {
                    player.playerState.value.path == cover && player.playerState.value.mediaStarted &&
                            !player.playerState.value.isVideo && pip.contentReady
                }
                checkpoint("cover")
            }
            assertFalse(session.isFullPlayerVisible)
            session.open(PlayerOpenRequest.Files(listOf(PlatformFile(video)), showPlayer = false))
            awaitPlayer("video resumes in PiP") { player.playerState.value.isVideo && pip.contentReady }
            assertTrue(pip.active)
            assertTrue(requireNotNull(pip.controller).pictureInPictureActive, "AVKit must remain active for video")

            player.onCommand(PlayerCommand.Paused(true))
            awaitPlayer("paused") { player.playerState.value.paused }
            checkpoint("video-returned")
            pip.restore()
            awaitPlayer("returned to recreated player window") { !pip.active && session.isFullPlayerVisible }
            assertTrue(player.playerState.value.paused, "return must not resume paused playback")
            checkpoint("restored")
            assertSame(player, session.coordinator)
            awaitPlayer("ready for another PiP entry") { pip.canEnter }
            val restoredInline = app.windows.filterIsInstance<NSWindow>()
                .firstNotNullOf { it.contentView?.let(::findInline) }
            pip.enter()
            assertSourceFrame(restoredInline)
            // An unrelated window may become visible during AVKit's start animation.
            val concurrentWindow = NSWindow(
                NSMakeRect(0.0, 0.0, 120.0, 80.0), NSWindowStyleMaskBorderless,
                NSBackingStoreBuffered, false,
            ).apply { releasedWhenClosed = false; orderFront(null) }
            try {
                awaitPlayer("second PiP entry") { pip.active }
                assertTrue(pip.contentReady, "Unrelated windows must not affect system PiP")
            } finally {
                concurrentWindow.close()
            }
            player.onCommand(PlayerCommand.Paused(false))
            awaitPlayer("playing") { !player.playerState.value.paused }
            if (getenv("PODAURA_PIP_EXTERNAL_CLOSE") != null) {
                // Click the real system close button. Programmatic stop requests UI restoration
                // on macOS and is not equivalent to the close button.
                println("PIP CHECKPOINT close: click the system close button")
                awaitPlayer("system close pauses", timeoutSeconds = 60.0) { !pip.active && player.playerState.value.paused }
            } else {
                requireNotNull(restoredInline.window).setFrameOrigin(NSMakePoint(340.0, 220.0))
                var restored = false
                pip.pictureInPictureController(
                    requireNotNull(pip.controller),
                    restoreUserInterfaceForPictureInPictureStopWithCompletionHandler = {
                        assertTrue(it)
                        assertSourceFrame(restoredInline)
                        restored = true
                    },
                )
                awaitPlayer("source aligned before restore completion") { restored }
                pip.restore()
                awaitPlayer("playing restore") { !pip.active }
                assertFalse(player.playerState.value.paused)
            }
            assertTrue(session.isFullPlayerVisible)
            println("PIP SMOKE passed: window close, media changes, paused restore, and " +
                    if (getenv("PODAURA_PIP_EXTERNAL_CLOSE") != null) "system close" else "playing restore")
        }
    }

    private fun checkpoint(name: String) {
        println("PIP CHECKPOINT $name")
        getenv("PODAURA_PIP_INSPECT_SECONDS")?.toKString()?.toDoubleOrNull()?.let(::pumpPlayer)
    }

}
