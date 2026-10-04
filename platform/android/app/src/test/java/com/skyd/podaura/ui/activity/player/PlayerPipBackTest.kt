package com.skyd.podaura.ui.activity.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerPipBackTest {
    @Test
    fun successfulPipEntryKeepsThePlayerActivity() {
        var requested = false
        assertTrue(enterPipOnPlayerBack(shouldEnterPip = true, inPipMode = false) {
            requested = true
            true
        })
        assertTrue(requested)
    }

    @Test
    fun disabledAutoEntryAndPipDismissalFinishWithoutRequestingAnotherWindow() {
        var requests = 0
        for ((enabled, active) in listOf(false to false, true to true)) {
            assertFalse(enterPipOnPlayerBack(shouldEnterPip = enabled, inPipMode = active) {
                requests++
                true
            })
        }
        assertEquals(0, requests)
    }

    @Test
    fun rejectedAndFailedPipRequestsStillCompleteBackNavigation() {
        assertFalse(enterPipOnPlayerBack(shouldEnterPip = true, inPipMode = false) { false })
        assertFalse(enterPipOnPlayerBack(shouldEnterPip = true, inPipMode = false) {
            throw IllegalStateException("PiP is unavailable")
        })
    }
}
