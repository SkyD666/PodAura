package com.skyd.podaura.ui.player.coordinator

import kotlin.math.log10
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** A temporary software gain, independent of the user's volume and playback speed. */
internal class SleepTimerFade(
    val originalGain: Double,
    private val started: TimeMark = TimeSource.Monotonic.markNow(),
) {
    val remainingMillis: Long
        get() = (1_000L - started.elapsedNow().inWholeMilliseconds).coerceIn(0L, 1_000L)

    val gain: Double
        get() {
            val amplitude = remainingMillis / 1_000.0
            // -150 dB is mpv's lowest software gain; pause at the end of the ramp.
            return if (amplitude == 0.0) -150.0
            else (originalGain + 20.0 * log10(amplitude)).coerceAtLeast(-150.0)
        }
}
