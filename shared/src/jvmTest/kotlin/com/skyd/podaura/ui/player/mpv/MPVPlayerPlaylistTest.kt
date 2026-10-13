@file:Suppress("INVISIBLE_REFERENCE")

package com.skyd.podaura.ui.player.mpv

import com.skyd.podaura.ui.player.PlaybackResumePolicy
import com.skyd.podaura.ui.player.PlayerCommand
import com.skyd.podaura.ui.player.isExplicitPlay
import com.skyd.podaura.ui.player.playbackIntent
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.openani.mediamp.mpv.MPVHandle
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MPVPlayerPlaylistTest {
    @Test
    fun skipsResumeAfterEofButPreserveOrdinaryPauses() = runBlocking {
        if (System.getenv("PODAURA_RUN_MPV_INTEGRATION") != "1") return@runBlocking
        val directory = Files.createTempDirectory("podaura-playlist-test").toFile()
        val mpv = MPV()
        try {
            // Initialize without a window or audio device; exercise the real playlist commands.
            val handle = MPV::class.java.getDeclaredField("mpvHandle").run {
                isAccessible = true
                get(mpv) as MPVHandle
            }
            for ((key, value) in mapOf(
                "config" to "no", "vo" to "null", "ao" to "null",
                "idle" to "yes", "keep-open" to "always",
            )) mpv.option(key, value)
            assertTrue(handle.initialize())
            val player = MPVPlayer()
            MPVPlayer::class.java.getDeclaredField("mpv").apply {
                isAccessible = true
                set(player, mpv)
            }
            // Three seconds of silent AAC, supported by the bundled desktop demuxers.
            val media = checkNotNull(javaClass.getResourceAsStream("/player/silence.m4a"))
                .use { it.readBytes() }
            val files = (0..2).map { index ->
                directory.resolve("$index.m4a").also { it.writeBytes(media) }
            }
            // Bypass the desktop surface gate only when loading these audio fixtures.
            for (file in files) handle.command("loadfile", file.path, "append")
            for (next in listOf(false, true)) {
                for (state in listOf("playing", "paused", "eof")) {
                    player.paused = true
                    mpv.command("playlist-play-index", "1")
                    await { player.path == files[1].path && !player.eofReached }
                    if (state != "paused") player.paused = false
                    if (state == "eof") {
                        mpv.command("seek", "2.9", "absolute+exact")
                        await { player.eofReached && player.paused }
                    }
                    val command = if (next) PlayerCommand.NextMedia else PlayerCommand.PreviousMedia
                    val intent = command.playbackIntent(player.paused, player.eofReached) == true
                    val policy = PlaybackResumePolicy()
                    policy.userAction(intent, explicitPlay = command.isExplicitPlay(player.paused))
                    if (next) player.playlistNext(play = intent) else player.playlistPrev(play = intent)
                    val target = files[if (next) 2 else 0]
                    await { player.path == target.path && !player.eofReached }
                    assertEquals(state == "paused", player.paused, "next=$next, state=$state")
                    if (state != "paused") await { mpv.getPropertyDouble("time-pos") > 0.1 }
                    assertEquals(intent, policy.enterBackground(playing = !player.paused, enabled = false))
                    assertEquals(intent, policy.enterForeground())
                }
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
