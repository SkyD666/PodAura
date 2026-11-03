package com.skyd.podaura.ui.player.volume

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.skyd.podaura.ui.player.mpv.platformMPV
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MpvVolumeBackendTest {
    @Test
    fun actualMpvVolumePersistsBeforeDestroyAndDoesNotInterfereWithSleepFade() {
        if (System.getenv("PODAURA_RUN_MPV_INTEGRATION") != "1") return
        val store = object : DataStore<Preferences> {
            override val data = MutableStateFlow(emptyPreferences())
            private val mutex = Mutex()
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                mutex.withLock {
                    delay(20) // Shutdown must wait for the final write, even with a slow store.
                    transform(data.value).also { data.value = it }
                }
        }
        val controller = PlayerVolumeController { store }
        val mpv = platformMPV()
        mpv.option("config", "no")
        mpv.initialize()
        try {
            mpv.setPropertyDouble("volume", 12.0)
            mpv.setPropertyBoolean("mute", true)
            controller.attach(MpvVolumeBackend(mpv))
            assertEquals(100f, controller.state.value.volume)
            assertFalse(controller.state.value.muted)
            mpv.setPropertyDouble("volume-gain", -8.0)
            mpv.setPropertyDouble("volume", 150.0)
            controller.refresh()
            assertEquals(150f, controller.state.value.volume)
            controller.toggleMute()
            assertTrue(controller.state.value.muted)
            controller.toggleMute()
            assertEquals(150f, controller.state.value.volume)
            controller.setVolume(250f)
            assertEquals(100f, controller.state.value.volume)
            controller.setVolume(35f)
            controller.setVolume(0f)
            assertEquals(0f, controller.state.value.volume)
            assertFalse(controller.state.value.muted)
            controller.toggleMute()
            assertEquals(35f, controller.state.value.volume)
            controller.toggleMute()
            controller.setVolume(28f)
            assertFalse(controller.state.value.muted)
            assertEquals(-8.0, mpv.getPropertyDouble("volume-gain"))
            controller.toggleMute()
            controller.detach()
            mpv.setPropertyDouble("volume", 100.0)
            mpv.setPropertyBoolean("mute", false)
            val reopened = PlayerVolumeController { store }
            try {
                reopened.attach(MpvVolumeBackend(mpv))
                assertEquals(28f, reopened.state.value.volume)
                assertTrue(reopened.state.value.muted)
                // External changes are captured even without receiving their property events.
                mpv.setPropertyDouble("volume", 160.0)
                reopened.detach()
                controller.attach(MpvVolumeBackend(mpv))
                assertEquals(160f, controller.state.value.volume)
            } finally {
                reopened.detach()
            }
        } finally {
            controller.detach()
            mpv.destroy()
        }
        // A queued UI adjustment arriving after detach must not touch the destroyed handle.
        controller.setVolume(70f)
        controller.toggleMute()
    }
}
