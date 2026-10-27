package com.skyd.podaura.ui.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SleepTimerTest {
    @Test
    fun deadlineSurvivesMediaChangesAndLongPeriodsWithoutChecks() {
        var now = 1_000L
        val timer = SleepTimer { now }
        timer.set(SleepTimerOption.AfterMinutes(30))
        now += 10 * 60_000
        timer.mediaChanged()
        assertFalse(timer.checkExpired())
        assertEquals(20 * 60L, timer.state.value.remainingSeconds(now))
        // Paused/background/buffering sessions do not need to produce playback events.
        now += 25 * 60_000
        assertTrue(timer.checkExpired())
        assertEquals(SleepTimerState(), timer.state.value)
        assertFalse(timer.checkExpired())
    }

    @Test
    fun reselectingTheSameDurationStartsFromNow() {
        var now = 0L
        val timer = SleepTimer { now }
        timer.set(SleepTimerOption.AfterMinutes(30))
        now += 10 * 60_000
        timer.set(SleepTimerOption.AfterMinutes(30))
        now += 20 * 60_000
        // A callback already queued for the old deadline must not expire the replacement.
        assertFalse(timer.checkExpired())
        assertEquals(10 * 60L, timer.state.value.remainingSeconds(now))
        now += 10 * 60_000
        assertTrue(timer.checkExpired())
    }

    @Test
    fun endOfMediaReplacesDeadlineAndWaitsWithoutAKnownDuration() {
        var now = 0L
        val timer = SleepTimer { now }
        timer.set(SleepTimerOption.AfterMinutes(5))
        timer.set(SleepTimerOption.EndOfMedia)
        now += 24 * 60 * 60_000L
        assertFalse(timer.checkExpired())
        assertEquals(SleepTimerOption.EndOfMedia, timer.state.value.option)
        timer.mediaChanged()
        assertEquals(SleepTimerState(), timer.state.value)
    }

    @Test
    fun turningOffCancelsTheDeadlineAndNewSessionsStartOff() {
        var now = 0L
        val timer = SleepTimer { now }
        timer.set(SleepTimerOption.AfterMinutes(5))
        timer.set(SleepTimerOption.Off)
        now += 10 * 60_000
        assertFalse(timer.checkExpired())
        assertEquals(SleepTimerState(), timer.state.value)
        assertEquals(SleepTimerState(), SleepTimer { now }.state.value)
    }

    @Test
    fun expiryUsesTheDeadlineRatherThanTheDisplayedRoundedSeconds() {
        var now = 0L
        val timer = SleepTimer { now }
        timer.set(SleepTimerOption.AfterMinutes(5))
        now = 299_999
        assertFalse(timer.checkExpired())
        assertEquals(1L, timer.remainingMillis)
        assertEquals(1L, timer.state.value.remainingSeconds(now))
        now++
        assertTrue(timer.checkExpired())
    }

    @Test
    fun displayUpdatesDoNotChangeTheTimerOrItsDeadline() {
        var now = 0L
        val timer = SleepTimer { now }
        timer.set(SleepTimerOption.AfterMinutes(5))
        val state = timer.state.value
        now = 1_000L
        assertEquals(299L, state.remainingSeconds(now))
        assertEquals(299_000L, timer.remainingMillis)
        assertEquals(state, timer.state.value)
        now = 300_000L
        assertEquals(0L, state.remainingSeconds(now))
        // Reading the countdown must not consume expiration before the playback actor sees it.
        assertEquals(state, timer.state.value)
        assertTrue(timer.checkExpired())
    }

    @Test
    fun aClockAdjustmentLeavesTheRemainingDelayAvailableForRescheduling() {
        var now = 60_000L
        val timer = SleepTimer { now }
        timer.set(SleepTimerOption.AfterMinutes(5))
        now = 300_000L // Five minutes elapsed, but the wall clock moved back by one minute.
        assertFalse(timer.checkExpired())
        assertEquals(60_000L, timer.remainingMillis)
        now = 360_000L
        assertTrue(timer.checkExpired())
    }

    @Test
    fun expirationWhileSystemPausedClearsAllAutomaticResumes() {
        val policy = PlaybackResumePolicy(independentScreenPlayback = true)
        assertTrue(policy.setScreenLocked(true, playing = true, backgroundEnabled = true))
        policy.beginInterruption(playing = false)
        // The coordinator delivers expiry through the explicit-pause policy callback.
        policy.userAction(allowsResume = false)
        assertFalse(policy.setScreenLocked(false, playing = false, backgroundEnabled = true))
        assertFalse(policy.endInterruption(shouldResume = true, backgroundEnabled = true))
        assertFalse(policy.enterForeground())
        policy.userAction(allowsResume = true, explicitPlay = true)
        assertTrue(policy.enterBackground(playing = true, enabled = false))
        assertTrue(policy.enterForeground())
    }
}
