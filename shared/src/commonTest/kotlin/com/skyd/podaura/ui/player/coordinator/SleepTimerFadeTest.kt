package com.skyd.podaura.ui.player.coordinator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource

class SleepTimerFadeTest {
    @Test
    fun fadesFromExistingGainOverOneSecondAndStaysFinished() {
        val clock = TestTimeSource()
        val fade = SleepTimerFade(originalGain = -3.0, started = clock.markNow())
        assertEquals(1_000L, fade.remainingMillis)
        assertEquals(-3.0, fade.gain)
        clock += 500.milliseconds
        assertEquals(500L, fade.remainingMillis)
        assertEquals(-9.0206, fade.gain, 0.0001)
        clock += 500.milliseconds
        assertEquals(0L, fade.remainingMillis)
        assertEquals(-150.0, fade.gain)
        clock += 5_000.milliseconds
        assertEquals(0L, fade.remainingMillis)
        assertEquals(-3.0, fade.originalGain)
    }
}
