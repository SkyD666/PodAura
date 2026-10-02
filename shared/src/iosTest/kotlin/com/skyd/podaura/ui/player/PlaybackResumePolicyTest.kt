package com.skyd.podaura.ui.player

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackResumePolicyTest {
    @Test fun backgroundPolicyIsReappliedWhenLoadingFinishes() {
        val policy = PlaybackResumePolicy()
        assertFalse(policy.enterBackground(playing = false, enabled = false))
        assertFalse(policy.pauseWhenRequired(playing = true, ready = false, backgroundEnabled = false))
        assertTrue(policy.pauseWhenRequired(playing = true, ready = true, backgroundEnabled = false))
        assertFalse(policy.pauseWhenRequired(playing = false, ready = true, backgroundEnabled = false))
        assertTrue(policy.enterForeground())
        assertFalse(policy.enterForeground())

        // A pause submitted before Ready was discarded; readiness must reapply it.
        assertTrue(policy.enterBackground(playing = true, enabled = false))
        assertFalse(policy.pauseWhenRequired(playing = true, ready = false, backgroundEnabled = false))
        assertTrue(policy.pauseWhenRequired(playing = true, ready = true, backgroundEnabled = false))
        policy.userAction()
        // The actor may still report playing until the user's pause command is processed.
        assertTrue(policy.pauseWhenRequired(playing = true, ready = true, backgroundEnabled = false))
        assertFalse(policy.enterForeground())
    }

    @Test fun lateLoadsRespectTheCurrentApplicationStateAndPreference() {
        val policy = PlaybackResumePolicy(initiallyBackground = true)
        assertFalse(policy.pauseWhenRequired(playing = true, ready = true, backgroundEnabled = true))
        assertTrue(policy.pauseWhenRequired(playing = true, ready = true, backgroundEnabled = false))
        policy.enterForeground()
        assertFalse(policy.pauseWhenRequired(playing = true, ready = true, backgroundEnabled = false))
        policy.beginInterruption(playing = false)
        assertTrue(policy.pauseWhenRequired(playing = true, ready = true, backgroundEnabled = true))
        assertTrue(policy.endInterruption(shouldResume = true, backgroundEnabled = true))
    }

    @Test fun onlySystemPausedPlaybackResumes() {
        val policy = PlaybackResumePolicy()
        assertTrue(policy.enterBackground(playing = true, enabled = false))
        assertTrue(policy.enterForeground())
        assertFalse(policy.enterForeground())
        assertFalse(policy.enterBackground(playing = false, enabled = false))
        assertFalse(policy.enterForeground())
        assertFalse(policy.enterBackground(playing = true, enabled = true))
    }

    @Test fun userPauseOrDisconnectedHeadphonesCancelPendingResume() {
        val policy = PlaybackResumePolicy()
        policy.beginInterruption(playing = true)
        policy.userAction()
        assertFalse(policy.endInterruption(shouldResume = true, backgroundEnabled = true))
        policy.enterBackground(playing = true, enabled = false)
        policy.userAction()
        assertFalse(policy.enterForeground())
    }

    @Test fun overlappingBackgroundAndInterruptionDoNotResumeEarly() {
        val policy = PlaybackResumePolicy()
        policy.enterBackground(playing = true, enabled = false)
        policy.beginInterruption(playing = false)
        assertFalse(policy.enterForeground())
        assertTrue(policy.endInterruption(shouldResume = true, backgroundEnabled = false))
        policy.beginInterruption(playing = true)
        policy.enterBackground(playing = false, enabled = false)
        assertFalse(policy.endInterruption(shouldResume = true, backgroundEnabled = false))
        assertTrue(policy.enterForeground())
    }

    @Test fun systemDenialAndDuplicateNotificationsCannotRestartPlayback() {
        val policy = PlaybackResumePolicy()
        policy.beginInterruption(playing = true)
        policy.beginInterruption(playing = false)
        assertFalse(policy.endInterruption(shouldResume = false, backgroundEnabled = true))
        assertFalse(policy.endInterruption(shouldResume = true, backgroundEnabled = true))
    }
}
