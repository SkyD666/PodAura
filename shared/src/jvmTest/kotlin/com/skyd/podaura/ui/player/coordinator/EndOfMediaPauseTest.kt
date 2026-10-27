@file:Suppress("INVISIBLE_REFERENCE")

package com.skyd.podaura.ui.player.coordinator

import com.skyd.podaura.ui.player.LoopMode
import com.skyd.podaura.ui.player.PlayerCommand
import com.skyd.podaura.ui.player.mpv.MPV
import com.skyd.podaura.ui.player.mpv.MPVPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.openani.mediamp.mpv.MPVHandle
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EndOfMediaPauseTest {
    @Test
    fun endTimerHoldsCurrentMediaBeforeRepeatingOrAdvancingAndRestoresOptions() = runBlocking {
        if (System.getenv("PODAURA_RUN_MPV_INTEGRATION") != "1") return@runBlocking
        val directory = Files.createTempDirectory("podaura-sleep-timer").toFile()
        val mpv = MPV()
        try {
            val handle = MPV::class.java.getDeclaredField("mpvHandle").run {
                isAccessible = true
                get(mpv) as MPVHandle
            }
            for ((key, value) in mapOf(
                "config" to "no", "vo" to "null", "ao" to "null",
                "idle" to "yes", "keep-open" to "yes", "input-default-bindings" to "yes",
            )) mpv.option(key, value)
            assertTrue(handle.initialize())
            val player = MPVPlayer()
            MPVPlayer::class.java.getDeclaredField("mpv").apply {
                isAccessible = true
                set(player, mpv)
            }
            val endOfMediaPause = EndOfMediaPause(player)
            assertEquals(PlayerCommand.PlayOrPause, mpv.playbackCommandForKey("SPACE"))
            mpv.command("keybind", "SPACE", "ignore")
            assertNull(mpv.playbackCommandForKey("SPACE"))
            mpv.command("keybind", "SPACE", "cycle pause")
            assertEquals(PlayerCommand.PlayOrPause, mpv.playbackCommandForKey("SPACE"))
            // Generated 1.25-second AAC silence; the bundled mpv omits the WAV demuxer.
            val silence = checkNotNull(javaClass.getResourceAsStream("/sleep-timer.m4a"))
                .use { it.readBytes() }
            val files = (0..2).map { index ->
                directory.resolve("$index.m4a").also { it.writeBytes(silence) }
            }
            files.forEach { handle.command("loadfile", it.path, "append") }
            for (loop in listOf("no", "file", "playlist")) {
                player.paused = true
                mpv.command("playlist-play-index", "0")
                await { player.path == files[0].path && !player.eofReached }
                endOfMediaPause.setLoopMode(when (loop) {
                    "file" -> LoopMode.LoopFile
                    "playlist" -> LoopMode.LoopPlaylist
                    else -> LoopMode.None
                })
                endOfMediaPause.setEnabled(true)
                assertEquals("always", mpv.getPropertyString("keep-open"))
                assertFalse(player.loopOne)
                assertFalse(player.loopPlaylist)
                player.paused = false
                // Exercise natural EOF as well as dragging directly to the end.
                if (loop != "no") mpv.command("seek", "1", "absolute+exact")
                await { player.eofReached && player.paused }
                delay(150)
                assertEquals(files[0].path, player.path, "must not advance: $loop")
                assertEquals(0, player.playlistPos)
                assertTrue(player.paused)
                // Expiration keeps the guard until an explicit playback action.
                endOfMediaPause.setEnabled(false)
                assertEquals("yes", mpv.getPropertyString("keep-open"))
                assertEquals(loop == "file", player.loopOne)
                assertEquals(loop == "playlist", player.loopPlaylist)
                assertTrue(player.paused)
                endOfMediaPause.setLoopMode(LoopMode.None)
            }
            mpv.command("playlist-play-index", "0")
            await { player.path == files[0].path && !player.eofReached }
            mpv.setPropertyString("keep-open", "always")
            endOfMediaPause.setEnabled(true)
            // Editing loop and queue while armed must not remove the EOF guard.
            endOfMediaPause.setLoopMode(LoopMode.LoopPlaylist)
            endOfMediaPause.setExternalQueue(true)
            assertEquals("always", mpv.getPropertyString("keep-open"))
            assertFalse(player.loopPlaylist)
            endOfMediaPause.setEnabled(false)
            assertTrue(player.loopPlaylist)
            assertEquals("yes", mpv.getPropertyString("keep-open"))
            endOfMediaPause.setEnabled(true)
            endOfMediaPause.setExternalQueue(false)
            assertEquals("always", mpv.getPropertyString("keep-open"))
            endOfMediaPause.setEnabled(false)
            assertEquals("always", mpv.getPropertyString("keep-open"))
            endOfMediaPause.setExternalQueue(true)
            endOfMediaPause.setLoopMode(LoopMode.None)
            player.paused = false
            await { player.path == files[1].path && mpv.getPropertyDouble("duration") > 1.0 }
            player.paused = true
            endOfMediaPause.setEnabled(true)
            // The UI reports whole seconds. Scrubbing to its endpoint must still reach the
            // fractional media endpoint, including when playback was already paused.
            endOfMediaPause.seek(player.duration)
            await { player.eofReached && player.paused }

            for ((start, next, target) in listOf(
                Triple(0, true, 1), Triple(1, false, 0),
                Triple(2, true, 0), Triple(0, false, 2),
            )) {
                player.paused = true
                mpv.command("playlist-play-index", start.toString())
                await { player.path == files[start].path && !player.eofReached }
                endOfMediaPause.setLoopMode(LoopMode.LoopPlaylist)
                endOfMediaPause.setEnabled(true)
                mpv.command("seek", "1.2", "absolute+exact")
                player.paused = false
                await { player.eofReached && player.paused }
                endOfMediaPause.skip(next, play = true, loopPlaylist = true)
                await { player.path == files[target].path && !player.eofReached }
                // The coordinator restores options only after the target has loaded.
                endOfMediaPause.setEnabled(false)
                delay(150)
                assertEquals(target, player.playlistPos, "start=$start, next=$next")
                assertFalse(player.paused)
                assertTrue(player.loopPlaylist)
            }
        } finally {
            mpv.destroy()
            directory.deleteRecursively()
        }
    }

    private suspend fun await(condition: () -> Boolean) = withTimeout(5_000) {
        while (!condition()) delay(10)
    }

}
