package com.skyd.podaura.ui.player.volume

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VolumeControllerTest {
    @Test
    fun mutePreservesBoostButZeroRestoresAnAudibleUserRangeValue() {
        val boosted = VolumeState().withVolume(150f)
        assertEquals(150f, boosted.volume)
        assertEquals(boosted, boosted.toggledMute().toggledMute())
        assertTrue(boosted.toggledMute().muted)
        val zero = boosted.withVolume(0f)
        assertEquals(100f, zero.toggledMute().volume)
        assertFalse(zero.toggledMute().muted)
        assertEquals(35f, VolumeState().withVolume(35f).withVolume(0f).toggledMute().volume)
        assertEquals(0.4f, VolumeState().withVolume(0.4f).withVolume(0f).toggledMute().volume)
        assertEquals(boosted, boosted.withVolume(Float.NaN))
    }

    @Test
    fun sharedAdjustmentHonorsSystemAndApplicationRanges() {
        val controller = object : VolumeController {
            override val range = 0f..15f
            override var value = 5f
        }
        controller.adjustTo(30f)
        assertEquals(15f, controller.value)
        controller.adjustTo(-5f)
        assertEquals(0f, controller.value)
        controller.adjustTo(Float.NaN)
        assertEquals(0f, controller.value)
    }
}
