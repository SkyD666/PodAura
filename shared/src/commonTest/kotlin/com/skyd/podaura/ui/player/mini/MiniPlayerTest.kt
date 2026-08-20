package com.skyd.podaura.ui.player.mini

import kotlin.test.Test
import kotlin.test.assertEquals

class MiniPlayerTest {
    @Test
    fun progressIsZeroWhenDurationIsNotPositive() {
        assertEquals(0f, miniPlayerProgress(position = 10, duration = 0))
        assertEquals(0f, miniPlayerProgress(position = 10, duration = -1))
    }

    @Test
    fun progressIsClampedToValidRange() {
        assertEquals(0f, miniPlayerProgress(position = -10, duration = 100))
        assertEquals(0.5f, miniPlayerProgress(position = 50, duration = 100))
        assertEquals(1f, miniPlayerProgress(position = 110, duration = 100))
    }
}
