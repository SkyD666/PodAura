package com.skyd.podaura.ui.player.volume

import com.skyd.podaura.ui.player.volume.windows.PreparedWindowsAudio
import com.skyd.podaura.ui.player.volume.windows.WindowsAudioRoute
import com.skyd.podaura.ui.player.volume.windows.resolveWindowsAudioDevice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WindowsAudioRouteTest {
    private val speaker = "{0.0.0.00000000}.{speaker}"
    private val headset = "{0.0.0.00000000}.{headset}"

    @Test
    fun resolvesExplicitShortAndFullIdsInsteadOfFallingBackToTheDefault() {
        val ids = listOf(speaker, headset)
        assertEquals(headset, resolveWindowsAudioDevice("wasapi/{headset}", ids, speaker))
        assertEquals(headset, resolveWindowsAudioDevice("wasapi/$headset", ids, speaker))
        assertEquals(headset, resolveWindowsAudioDevice("wasapi/1", ids, speaker))
        assertEquals(speaker, resolveWindowsAudioDevice("auto", ids, speaker))
        assertNull(resolveWindowsAudioDevice("wasapi/{disconnected}", ids, speaker))
    }

    @Test
    fun preparesMuteBeforeChangingTheDefaultDeviceAndRetainsAutoIntent() {
        val calls = mutableListOf<String>()
        var configured = "auto"
        var defaultId = speaker
        val muted = VolumeState(volume = 23f, muted = true)
        val route = WindowsAudioRoute(
            bind = { calls += "bind:$it"; configured = it },
            reload = { calls += "reload" },
        )
        val prepare: (String?) -> PreparedWindowsAudio = {
            assertEquals("auto", it)
            calls += "mute:$defaultId"
            PreparedWindowsAudio(defaultId, muted)
        }
        assertEquals(muted, route.refresh(configured, null, prepare))
        defaultId = headset // hot-plugged while playback is muted
        assertEquals(muted, route.refresh(configured, "wasapi", prepare))
        assertEquals(listOf(
            "mute:$speaker", "bind:wasapi/$speaker",
            "mute:$headset", "bind:wasapi/$headset",
        ), calls)
    }

    @Test
    fun failedPreparationCannotRouteToDefaultAndSameDeviceNullOutputRetries() {
        var configured = "wasapi/{headset}"
        val calls = mutableListOf<String>()
        val route = WindowsAudioRoute(
            bind = { calls += "bind:$it"; configured = it },
            reload = { calls += "reload" },
        )
        assertNull(route.refresh(configured, null) { null })
        assertEquals("wasapi/PodAura-unavailable-endpoint", configured)
        val prepare: (String?) -> PreparedWindowsAudio = {
            assertEquals("wasapi/{headset}", it)
            calls += "prepare"
            PreparedWindowsAudio(headset, VolumeState(volume = 18f))
        }
        route.refresh(configured, "null", prepare)
        route.refresh(configured, "null", prepare) // device recovers with the same ID
        assertEquals(listOf(
            "bind:wasapi/PodAura-unavailable-endpoint", "prepare", "bind:wasapi/$headset",
            "prepare", "reload",
        ), calls)
        route.refresh("auto", "wasapi") { requested ->
            assertEquals("auto", requested)
            PreparedWindowsAudio(speaker, VolumeState())
        }
        assertEquals("wasapi/$speaker", configured)
    }
}
