package com.skyd.podaura.ui.player.volume

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerVolumeControllerTest {
    @Test
    fun adoptsSystemMixerThenPersistsExternalChangesAndKeepsZeroDistinctFromMute() {
        val store = object : DataStore<Preferences> {
            override val data = MutableStateFlow(preferencesOf(
                floatPreferencesKey("playerVolume") to 80f,
                booleanPreferencesKey("playerMuted") to false,
            ))
            private val lock = Mutex()
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences) =
                lock.withLock { transform(data.value).also { data.value = it } }
        }
        val backend = object : PlayerVolumeBackend {
            var actual: VolumeState? = VolumeState(volume = 37f, muted = true)
            var closed = false
            override fun initialize(saved: VolumeState) = Unit // The OS owns the session value.
            override fun read() = actual.also { check(!closed) }
            override fun apply(value: VolumeState) { check(!closed); actual = value }
            override fun close() { closed = true }
        }
        val controller = PlayerVolumeController { store }
        controller.attach(backend)
        assertEquals(37f, controller.state.value.volume)
        assertTrue(controller.state.value.muted)
        controller.setVolume(28f)
        assertFalse(controller.state.value.muted)
        controller.setVolume(0f)
        assertFalse(controller.state.value.muted)
        controller.toggleMute()
        assertEquals(28f, controller.state.value.volume)
        backend.actual = null
        controller.refresh()
        assertFalse(controller.state.value.available)
        backend.actual = VolumeState(volume = 19f, muted = true)
        controller.refresh()
        assertTrue(controller.state.value.available)
        controller.detach()
        assertTrue(backend.closed)
        assertEquals(19f, store.data.value[floatPreferencesKey("playerVolume")])
        assertTrue(store.data.value[booleanPreferencesKey("playerMuted")] == true)
        controller.setVolume(50f)
        controller.toggleMute() // Queued commands cannot reopen a released COM/mpv handle.
        assertEquals(19f, controller.state.value.volume)
    }
}
