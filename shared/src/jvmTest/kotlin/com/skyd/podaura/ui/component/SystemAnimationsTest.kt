package com.skyd.podaura.ui.component

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class)
class SystemAnimationsTest {
    @Test
    fun linuxReadsTheGtkXSettingsAnimationPreference() {
        // GNOME publishes Gtk/EnableAnimations; AWT exposes it with the gnome. prefix.
        val properties = mutableMapOf<String, Any>("gnome.Gtk/EnableAnimations" to 0)
        assertFalse(linuxAnimationsEnabled(properties::get))
        properties["gnome.Gtk/EnableAnimations"] = 1
        assertTrue(linuxAnimationsEnabled(properties::get))
        properties.clear()
        assertTrue(linuxAnimationsEnabled(properties::get))
    }

    @Test
    fun desktopWithoutComposeScaleReadsNativePreferenceAndStopsObservingWhenHidden() =
        runDesktopComposeUiTest(width = 64, height = 64, testTimeout = 30.seconds) {
            var nativeEnabled = false
            var reads = 0
            val reader = { reads++; nativeEnabled }
            var observe by mutableStateOf(true)
            var enabled = true
            setContent {
                val current = rememberSystemAnimationsEnabled(observe, reader)
                SideEffect { enabled = current }
            }
            runOnIdle { assertFalse(enabled); assertTrue(reads > 0) }
            mainClock.autoAdvance = false
            runOnIdle { nativeEnabled = true }
            mainClock.advanceTimeBy(1_100)
            runOnIdle { assertTrue(enabled) }
            runOnIdle { nativeEnabled = false }
            mainClock.advanceTimeBy(1_100)
            runOnIdle { assertFalse(enabled); observe = false }
            mainClock.advanceTimeBy(32)
            val stoppedReads = reads
            mainClock.advanceTimeBy(2_000)
            runOnIdle { assertEquals(stoppedReads, reads); nativeEnabled = true; observe = true }
            mainClock.advanceTimeBy(32)
            runOnIdle { assertTrue(enabled); observe = false }
            mainClock.advanceTimeBy(32)
        }
}
