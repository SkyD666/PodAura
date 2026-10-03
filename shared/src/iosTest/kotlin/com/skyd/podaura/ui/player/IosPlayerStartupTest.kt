package com.skyd.podaura.ui.player

import com.skyd.podaura.di.dataStoreModule
import com.skyd.podaura.ui.player.mpv.EventListener
import com.skyd.podaura.ui.player.mpv.MPVEvent
import com.skyd.podaura.ui.player.mpv.MPVPlayer
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.create
import platform.Foundation.writeToFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.TimeSource

class IosPlayerStartupTest {
    @Test
    fun opensSelectedMediaInLargePlaylist() = runBlocking {
        startKoin { modules(dataStoreModule) }
        val path = NSTemporaryDirectory() + "podaura-startup.wav"
        val wave = ByteArray(44 + 16000)
        fun text(offset: Int, value: String) = value.encodeToByteArray().copyInto(wave, offset)
        fun number(offset: Int, value: Int, bytes: Int) {
            repeat(bytes) { wave[offset + it] = (value ushr (8 * it)).toByte() }
        }
        text(0, "RIFF"); number(4, wave.size - 8, 4); text(8, "WAVEfmt ")
        number(16, 16, 4); number(20, 1, 2); number(22, 1, 2)
        number(24, 8000, 4); number(28, 16000, 4); number(32, 2, 2); number(34, 16, 2)
        text(36, "data"); number(40, wave.size - 44, 4)
        wave.usePinned {
            assertTrue(NSData.create(it.addressOf(0), wave.size.toULong()).writeToFile(path, true))
        }
        val player = MPVPlayer.instance
        try {
            val startup = TimeSource.Monotonic.markNow()
            player.ensureInitialized()
            println("iOS player initialization: ${startup.elapsedNow()}")
            player.mpv.setPropertyString("ao", "null")
            player.mpv.setPropertyBoolean("shuffle", true)
            val loaded = CompletableDeferred<Unit>()
            player.mpv.addEventListener(object : EventListener {
                override fun onEvent(event: Int) {
                    if (event == MPVEvent.FILE_LOADED && player.path == path) loaded.complete(Unit)
                }
                override fun onPropertyChange(name: String) = Unit
                override fun onPropertyChange(name: String, value: Boolean) = Unit
                override fun onPropertyChange(name: String, value: Long) = Unit
                override fun onPropertyChange(name: String, value: Double) = Unit
                override fun onPropertyChange(name: String, value: String) = Unit
            })
            val files = List(1000) { NSTemporaryDirectory() + "podaura-unused-$it.wav" } + path
            val loading = TimeSource.Monotonic.markNow()
            player.loadList(files, path)
            println("iOS playlist submission: ${loading.elapsedNow()}")
            withTimeout(15_000) { loaded.await() }
            println("iOS selected media loaded: ${loading.elapsedNow()}")
            assertEquals(path, player.path)
            assertEquals(files.lastIndex, player.playlistPos)
            assertEquals(files, player.loadPlaylist())
            assertTrue(player.shuffle)
        } finally {
            player.destroy()
            NSFileManager.defaultManager.removeItemAtPath(path, null)
            stopKoin()
        }
    }
}
