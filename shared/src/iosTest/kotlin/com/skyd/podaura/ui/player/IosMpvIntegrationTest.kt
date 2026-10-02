package com.skyd.podaura.ui.player

import com.skyd.podaura.ui.player.mpv.*
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.cinterop.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import platform.Foundation.*
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.test.*

class IosMpvIntegrationTest {
    @Test fun decodeObserveSeekAndRecreateNativePlayer() = runBlocking {
        val path = NSTemporaryDirectory() + "podaura-native-playback-test.wav"
        val bytes = silentWave()
        bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()).writeToFile(path, true) }
        try {
            // A real decoder and event loop, not a fake MPV. Two lifetimes catch stale handles.
            repeat(2) {
                val loaded = AtomicBoolean(false)
                val observedPause = AtomicBoolean(false)
                val player = MPV()
                try {
                    player.option("config", "no")
                    player.option("hwdec", "auto")
                    player.initialize()
                    assertEquals("videotoolbox", player.getPropertyString("hwdec"))
                    player.setPropertyString("hwdec", "no")
                    player.option("hwdec", "auto")
                    assertEquals("videotoolbox", player.getPropertyString("hwdec"))
                    player.setPropertyString("hwdec", "no")
                    player.command("cycle-values", "hwdec", "auto", "no")
                    assertEquals("videotoolbox", player.getPropertyString("hwdec"))
                    player.setPropertyString("ao", "null") // Keep tests silent.
                    player.observeProperty("pause", MPVFormat.MPV_FORMAT_FLAG)
                    player.addEventListener(object : EventListener {
                        override fun onPropertyChange(name: String) = Unit
                        override fun onPropertyChange(name: String, value: Boolean) { if (name == "pause" && value) observedPause.store(true) }
                        override fun onPropertyChange(name: String, value: Long) = Unit
                        override fun onPropertyChange(name: String, value: Double) = Unit
                        override fun onPropertyChange(name: String, value: String) = Unit
                        override fun onEvent(event: Int) { if (event == MPVEvent.FILE_LOADED) loaded.store(true) }
                    })
                    player.updateRequestHeaders(mapOf(path to mapOf("X-PodAura-Test" to "test,value")))
                    player.command("loadfile", path)
                    withTimeout(15_000) { while (!loaded.load()) delay(20) }
                    withTimeout(5_000) { while (player.getPropertyDouble("time-pos") < 0.1) delay(20) }
                    player.setPropertyBoolean("pause", true)
                    withTimeout(5_000) { while (!observedPause.load()) delay(20) }
                    assertTrue(player.getPropertyBoolean("pause"))
                    assertTrue(player.getPropertyDouble("duration") >= 9.9)
                    player.setPropertyDouble("speed", 1.5)
                    assertEquals(1.5, player.getPropertyDouble("speed"))
                    player.command("seek", "1", "absolute+exact")
                    // Let the audio clock consume the seek before checking its new position.
                    player.setPropertyBoolean("pause", false)
                    withTimeout(5_000) { while (player.getPropertyDouble("time-pos") < 0.8) delay(20) }
                    assertTrue(player.getPropertyDouble("time-pos") < 1.2)
                    assertTrue(player.getPropertyInt("track-list/count") >= 1)
                    assertEquals(path, player.getPropertyString("path"))
                    assertTrue(player.getPropertyString("http-header-fields").orEmpty().contains("X-PodAura-Test: test,value"))
                    player.updateRequestHeaders(emptyMap())
                    loaded.store(false)
                    player.command("loadfile", path)
                    withTimeout(15_000) { while (!loaded.load()) delay(20) }
                    assertFalse(player.getPropertyString("http-header-fields").orEmpty().contains("X-PodAura-Test"))
                } finally { player.destroy(); player.destroy() }
            }
            val media = resolveExternalMedia(PlatformFile(path))
            assertEquals(path, media.source)
            assertEquals(path, media.playbackUrl)
            media.release()
            val restored = resolveExternalMedia(PlatformFile(path))
            assertEquals(path, restored.source)
            restored.release()
        } finally {
            NSFileManager.defaultManager.removeItemAtPath(path, null)
            NSUserDefaults.standardUserDefaults.removeObjectForKey("podaura.mediaBookmark.$path")
        }
    }

    private fun silentWave(): ByteArray {
        val size = 44 + 8000 * 2 * 10
        val data = ByteArray(size)
        fun text(offset: Int, text: String) { text.encodeToByteArray().copyInto(data, offset) }
        fun number(offset: Int, value: Int, bytes: Int) { repeat(bytes) { data[offset + it] = (value ushr (8 * it)).toByte() } }
        text(0, "RIFF"); number(4, size - 8, 4); text(8, "WAVEfmt ")
        number(16, 16, 4); number(20, 1, 2); number(22, 1, 2)
        number(24, 8000, 4); number(28, 16000, 4); number(32, 2, 2); number(34, 16, 2)
        text(36, "data"); number(40, size - 44, 4)
        return data
    }
}
