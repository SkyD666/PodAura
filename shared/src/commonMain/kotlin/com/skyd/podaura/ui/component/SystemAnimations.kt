package com.skyd.podaura.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.MotionDurationScale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds

/** Android/iOS supply a scale; desktop hosts need their native accessibility preference. */
@Composable
internal fun rememberSystemAnimationsEnabled(
    observe: Boolean,
    readPlatformPreference: () -> Boolean = ::platformAnimationsEnabled,
): Boolean {
    val scale = rememberCoroutineScope().coroutineContext[MotionDurationScale]
    if (scale != null) return scale.scaleFactor != 0f

    val initial = remember(readPlatformPreference) { readPlatformPreference() }
    val enabled by produceState(initial, observe, readPlatformPreference) {
        // ponytail: poll only while visible/playing; use native notifications if used broadly.
        do {
            value = readPlatformPreference()
            if (!observe) break
            delay(1_000.milliseconds)
        } while (isActive)
    }
    return enabled
}

internal expect fun platformAnimationsEnabled(): Boolean
