@file:Suppress("INVISIBLE_REFERENCE")

package com.skyd.podaura.ui.player.mpv

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MPVBackgroundPlaybackTest {
    @Test
    fun startsPlaybackWithoutAWindowOrRenderContext() = runBlocking {
        if (System.getenv("PODAURA_RUN_MPV_INTEGRATION") != "1") return@runBlocking
        val directory = Files.createTempDirectory("podaura-background-").toFile()
        val mpv = MPV()
        try {
            mpv.option("config", "no")
            mpv.initialize()
            mpv.setPropertyString("ao", "null")
            for (extension in listOf("m4a", "mp4")) {
                val file = directory.resolve("media.$extension")
                file.writeBytes(checkNotNull(javaClass.getResourceAsStream("/player/background-playback.$extension"))
                    .use { it.readBytes() })
                mpv.command("loadfile", file.path)
                withTimeout(10_000) {
                    while (mpv.getPropertyString("path") != file.path ||
                        mpv.getPropertyDouble("time-pos") < 0.1) delay(20)
                }
                assertEquals("null", mpv.getPropertyString("vo"))
                assertTrue(mpv.getPropertyDouble("duration") > 1.0)
                mpv.command("stop")
            }
        } finally {
            mpv.destroy()
            directory.deleteRecursively()
        }
    }
}
