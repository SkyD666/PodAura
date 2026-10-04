package com.skyd.podaura.ui.activity.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PipDismissalTrackerTest {
    @Test
    fun lockScreenAndRecreationAreNotDismissalButClosingIs() {
        val tracker = PipDismissalTracker()
        tracker.onModeChanged(true)
        // Locking or recreating the activity does not finish it.
        assertFalse(tracker.isDismissed(isFinishing = false))
        // Some versions clear PiP before destroying, others afterwards.
        assertTrue(tracker.isDismissed(isFinishing = true))
        tracker.onModeChanged(false)
        assertTrue(tracker.isDismissed(isFinishing = true))
    }

    @Test
    fun restoringFullPlayerClearsPipDismissalRegardlessOfCallbackOrder() {
        for (modeChangeFirst in listOf(true, false)) {
            val tracker = PipDismissalTracker()
            tracker.onModeChanged(true)
            if (modeChangeFirst) tracker.onModeChanged(false)
            tracker.onFullPlayerResumed()
            if (!modeChangeFirst) tracker.onModeChanged(false)
            assertFalse(tracker.isDismissed(isFinishing = true))
            tracker.onModeChanged(true)
            assertTrue(tracker.isDismissed(isFinishing = true))
        }
        assertFalse(PipDismissalTracker().isDismissed(isFinishing = true))
    }
}
