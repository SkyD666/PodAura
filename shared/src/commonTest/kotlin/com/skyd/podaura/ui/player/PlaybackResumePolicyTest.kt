package com.skyd.podaura.ui.player

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackResumePolicyTest {
    @Test fun screenLockOverridesBackgroundAndPipAndUnlockResumesOnlyOnce() {
        for (backgroundEnabled in listOf(false, true)) {
            for (pip in listOf(false, true)) {
                val policy = PlaybackResumePolicy()
                policy.setPictureInPictureActive(pip)
                assertTrue(policy.setScreenLocked(true, playing = true, backgroundEnabled))
                policy.enterBackground(playing = false, enabled = backgroundEnabled)
                assertFalse(policy.setPictureInPictureActive(pip))
                assertTrue(policy.pauseWhenRequired(playing = true, ready = true, backgroundEnabled))
                assertFalse(policy.setScreenLocked(true, playing = false, backgroundEnabled))
                val resumesOnUnlock = policy.setScreenLocked(false, playing = false, backgroundEnabled)
                assertTrue(resumesOnUnlock || policy.enterForeground())
                policy.enterForeground()
                assertFalse(policy.setScreenLocked(false, playing = true, backgroundEnabled))
                assertFalse(policy.pauseWhenRequired(playing = true, ready = true, backgroundEnabled))
            }
        }
    }

    @Test fun userPauseAndInitiallyPausedMediaDoNotResumeOnUnlock() {
        for (playing in listOf(false, true)) {
            val policy = PlaybackResumePolicy()
            policy.setScreenLocked(true, playing, backgroundEnabled = true)
            policy.userAction(allowsResume = false)
            // A delayed playing update must not undo the user's pause intent.
            policy.pauseWhenRequired(playing = true, ready = true, backgroundEnabled = true)
            assertFalse(policy.setScreenLocked(false, playing = false, backgroundEnabled = true))
            assertFalse(policy.enterForeground())
        }
        val paused = PlaybackResumePolicy()
        assertFalse(paused.setScreenLocked(true, playing = false, backgroundEnabled = true))
        assertFalse(paused.setScreenLocked(false, playing = false, backgroundEnabled = true))
    }

    @Test fun unlockAndAudioInterruptionCanArriveInEitherOrder() {
        for (unlockFirst in listOf(false, true)) {
            for (shouldResume in listOf(false, true)) {
                val policy = PlaybackResumePolicy()
                policy.setScreenLocked(true, playing = true, backgroundEnabled = true)
                policy.beginInterruption(playing = false)
                if (unlockFirst) {
                    assertFalse(policy.setScreenLocked(false, playing = false, backgroundEnabled = true))
                    kotlin.test.assertEquals(shouldResume, policy.endInterruption(shouldResume, backgroundEnabled = true))
                } else {
                    assertFalse(policy.endInterruption(shouldResume, backgroundEnabled = true))
                    kotlin.test.assertEquals(shouldResume, policy.setScreenLocked(false, playing = false, backgroundEnabled = true))
                }
                assertFalse(policy.enterForeground())
            }
        }
    }

    @Test fun mediaLoadedWhileScreenIsOffPausesWhenReadyAndResumesAfterUnlock() {
        val policy = PlaybackResumePolicy(initiallyScreenLocked = true)
        policy.userAction(allowsResume = true)
        assertFalse(policy.pauseWhenRequired(playing = true, ready = false, backgroundEnabled = true))
        assertTrue(policy.pauseWhenRequired(playing = true, ready = true, backgroundEnabled = true))
        assertTrue(policy.setScreenLocked(false, playing = false, backgroundEnabled = true))
        repeat(3) {
            assertFalse(policy.pauseWhenRequired(playing = true, ready = true, backgroundEnabled = true))
        }
    }

    @Test fun explicitPlayCanRecoverWhenInterruptionEndedNotificationNeverArrives() {
        val policy = PlaybackResumePolicy()
        policy.beginInterruption(playing = true)
        policy.userAction(allowsResume = true, audioSessionActivated = false)
        assertTrue(policy.pauseWhenRequired(playing = true, ready = true, backgroundEnabled = false))
        policy.userAction(allowsResume = true, audioSessionActivated = true)
        repeat(3) {
            assertFalse(policy.pauseWhenRequired(playing = true, ready = true, backgroundEnabled = false))
        }
        assertFalse(policy.endInterruption(shouldResume = true, backgroundEnabled = false))
        // A new interruption must still pause playback after recovery.
        policy.beginInterruption(playing = true)
        assertTrue(policy.pauseWhenRequired(playing = true, ready = true, backgroundEnabled = false))
    }

    @Test fun manualPictureInPictureAllowsBackgroundPlaybackAndPlayCommands() {
        val policy = PlaybackResumePolicy()
        assertFalse(policy.setPictureInPictureActive(true))
        assertFalse(policy.enterBackground(playing = true, enabled = false))
        assertFalse(policy.pauseWhenRequired(playing = true, ready = true, backgroundEnabled = false))
        policy.userAction(allowsResume = true)
        assertFalse(policy.pauseWhenRequired(playing = true, ready = true, backgroundEnabled = false))
        assertFalse(policy.enterForeground())
    }

    @Test fun latePictureInPictureStartOnlyResumesBackgroundPolicyPauses() {
        val policy = PlaybackResumePolicy()
        assertTrue(policy.enterBackground(playing = true, enabled = false))
        assertTrue(policy.setPictureInPictureActive(true))
        assertFalse(policy.setPictureInPictureActive(true))
        assertFalse(policy.enterForeground())

        val userPaused = PlaybackResumePolicy()
        assertTrue(userPaused.enterBackground(playing = true, enabled = false))
        userPaused.userAction()
        assertFalse(userPaused.setPictureInPictureActive(true))

        val initiallyPaused = PlaybackResumePolicy()
        assertFalse(initiallyPaused.enterBackground(playing = false, enabled = false))
        assertFalse(initiallyPaused.setPictureInPictureActive(true))
    }

    @Test fun pictureInPictureStillRespectsInterruptionAndExplicitPause() {
        val policy = PlaybackResumePolicy(initiallyBackground = true)
        policy.setPictureInPictureActive(true)
        policy.beginInterruption(playing = true)
        assertTrue(policy.pauseWhenRequired(playing = true, ready = true, backgroundEnabled = false))
        assertTrue(policy.endInterruption(shouldResume = true, backgroundEnabled = false))
        policy.beginInterruption(playing = true)
        policy.userAction()
        assertFalse(policy.endInterruption(shouldResume = true, backgroundEnabled = false))

        policy.userAction(allowsResume = true)
        policy.beginInterruption(playing = true)
        assertFalse(policy.endInterruption(shouldResume = false, backgroundEnabled = false))
    }

    @Test fun leavingPictureInPictureRestoresBackgroundPolicyWithoutRestartingDismissedPlayback() {
        val policy = PlaybackResumePolicy(initiallyBackground = true)
        policy.setPictureInPictureActive(true)
        policy.userAction()
        assertFalse(policy.setPictureInPictureActive(false))
        assertTrue(policy.pauseWhenRequired(playing = true, ready = true, backgroundEnabled = false))
        assertFalse(policy.enterForeground())
    }

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
