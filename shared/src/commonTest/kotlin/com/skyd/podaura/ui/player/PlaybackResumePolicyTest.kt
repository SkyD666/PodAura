package com.skyd.podaura.ui.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackResumePolicyTest {
    @Test fun eofSkipsPermitSystemResumeWithoutGrantingExplicitPlayback() {
        for (command in listOf(PlayerCommand.NextMedia, PlayerCommand.PreviousMedia)) {
            for (eofReached in listOf(false, true)) {
                val intent = command.playbackIntent(paused = true, eofReached = eofReached) == true
                assertEquals(eofReached, intent)
                val policy = PlaybackResumePolicy()
                policy.userAction(intent, explicitPlay = command.isExplicitPlay(paused = true))
                assertEquals(intent, policy.enterBackground(playing = intent, enabled = false))
                assertEquals(intent, policy.enterForeground())

                val locked = PlaybackResumePolicy(initiallyBackground = true, initiallyScreenLocked = true)
                locked.userAction(intent, explicitPlay = command.isExplicitPlay(paused = true))
                // A skip at EOF can start playback, but cannot bypass either automatic policy.
                assertTrue(locked.pauseWhenRequired(true, true, backgroundEnabled = true))
                val background = PlaybackResumePolicy(initiallyBackground = true)
                background.userAction(intent, explicitPlay = command.isExplicitPlay(paused = true))
                assertTrue(background.pauseWhenRequired(true, true, backgroundEnabled = false))
            }
        }
    }

    @Test fun automaticScreenAndBackgroundPlaybackFollowBothSettings() {
        for (android in listOf(false, true)) {
            for (pauseOnScreenOff in listOf(false, true)) {
                for (backgroundEnabled in listOf(false, true)) {
                    val locked = PlaybackResumePolicy(
                        independentScreenPlayback = android,
                        pauseOnScreenOff = pauseOnScreenOff,
                    )
                    val pausedOnLock = locked.setScreenLocked(true, playing = true, backgroundEnabled)
                    val pausedOnBackground = locked.enterBackground(!pausedOnLock, backgroundEnabled)
                    val shouldPause = pauseOnScreenOff || (!android && !backgroundEnabled)
                    assertEquals(shouldPause, pausedOnLock || pausedOnBackground)
                    // Loading the next item must apply the same rules.
                    assertEquals(shouldPause, locked.pauseWhenRequired(true, true, backgroundEnabled))

                    val home = PlaybackResumePolicy(
                        independentScreenPlayback = android,
                        pauseOnScreenOff = pauseOnScreenOff,
                    )
                    assertEquals(!backgroundEnabled, home.enterBackground(true, backgroundEnabled))
                }
            }
        }
    }

    @Test fun androidHomePauseIsNotUndoneByLockingAndUnlocking() {
        val policy = PlaybackResumePolicy(independentScreenPlayback = true, pauseOnScreenOff = false)
        assertTrue(policy.enterBackground(playing = true, enabled = false))
        assertFalse(policy.setScreenLocked(true, playing = false, backgroundEnabled = false))
        // A deferred load must not escape the earlier Home pause once the screen is off.
        assertTrue(policy.pauseWhenRequired(true, true, backgroundEnabled = false))
        assertFalse(policy.setScreenLocked(false, playing = false, backgroundEnabled = false))
        assertTrue(policy.enterForeground())
        assertFalse(policy.enterForeground())
    }

    @Test fun androidUnlockRestoresBackgroundRestrictionAfterScreenPlayback() {
        val policy = PlaybackResumePolicy(independentScreenPlayback = true, pauseOnScreenOff = false)
        assertFalse(policy.setScreenLocked(true, playing = true, backgroundEnabled = false))
        assertFalse(policy.enterBackground(playing = true, enabled = false))
        assertFalse(policy.setScreenLocked(false, playing = true, backgroundEnabled = false))
        assertTrue(policy.pauseWhenRequired(true, true, backgroundEnabled = false))
        assertTrue(policy.enterForeground())
    }

    @Test fun explicitPlaySurvivesQueueAdvancementButNotTheNextAndroidScreenOff() {
        val policy = PlaybackResumePolicy(independentScreenPlayback = true)
        assertTrue(policy.setScreenLocked(true, playing = true, backgroundEnabled = false))
        policy.enterBackground(playing = false, enabled = false)
        policy.userAction(allowsResume = true, explicitPlay = true)
        repeat(3) {
            assertFalse(policy.pauseWhenRequired(true, true, backgroundEnabled = false))
        }
        // Changing the playing item does not revoke the explicit Play permission.
        policy.userAction(allowsResume = true)
        assertFalse(policy.pauseWhenRequired(true, true, backgroundEnabled = false))
        // The lit lock screen can turn off again without USER_PRESENT in between.
        policy.onScreenOff()
        assertFalse(policy.setScreenLocked(true, playing = true, backgroundEnabled = false))
        assertTrue(policy.pauseWhenRequired(true, true, backgroundEnabled = false))
    }

    @Test fun iosExplicitPlayLastsUntilForegroundOrUserPauseDespiteLateLockNotifications() {
        for (userPause in listOf(false, true)) {
            val policy = PlaybackResumePolicy()
            assertTrue(policy.enterBackground(playing = true, enabled = false))
            policy.userAction(allowsResume = true, explicitPlay = true)
            repeat(2) {
                assertFalse(policy.setScreenLocked(true, playing = true, backgroundEnabled = false))
                assertFalse(policy.pauseWhenRequired(true, true, backgroundEnabled = false))
                assertFalse(policy.setScreenLocked(false, playing = true, backgroundEnabled = false))
            }
            if (userPause) {
                policy.userAction(allowsResume = false)
                assertTrue(policy.pauseWhenRequired(true, true, backgroundEnabled = false))
                assertFalse(policy.enterForeground())
            } else {
                assertFalse(policy.enterForeground())
                assertTrue(policy.enterBackground(playing = true, enabled = false))
            }
        }
    }

    @Test fun explicitBackgroundPlayDoesNotPermitFutureAndroidLocks() {
        val policy = PlaybackResumePolicy(independentScreenPlayback = true)
        assertTrue(policy.enterBackground(playing = true, enabled = false))
        policy.userAction(allowsResume = true, explicitPlay = true)
        assertFalse(policy.pauseWhenRequired(true, true, backgroundEnabled = false))
        assertTrue(policy.setScreenLocked(true, playing = true, backgroundEnabled = false))
    }

    @Test fun interruptionStillPausesExplicitScreenPlaybackAndHonorsUserPause() {
        for (android in listOf(false, true)) {
            val policy = PlaybackResumePolicy(initiallyScreenLocked = true, independentScreenPlayback = android)
            policy.enterBackground(playing = false, enabled = false)
            policy.userAction(allowsResume = true, explicitPlay = true)
            policy.beginInterruption(playing = true)
            assertTrue(policy.pauseWhenRequired(true, true, backgroundEnabled = false))
            assertTrue(policy.endInterruption(shouldResume = true, backgroundEnabled = false))
            policy.beginInterruption(playing = true)
            policy.userAction(allowsResume = false)
            assertFalse(policy.endInterruption(shouldResume = true, backgroundEnabled = false))
            assertFalse(policy.setScreenLocked(false, playing = false, backgroundEnabled = false))
            assertFalse(policy.enterForeground())
        }
    }

    @Test fun onlyPlayCommandsGrantPermissionAndPausedTrackChangesRemainPaused() {
        for (command in listOf(PlayerCommand.Paused(false), PlayerCommand.PlayOrPause)) {
            assertTrue(command.isExplicitPlay(paused = true))
        }
        assertFalse(PlayerCommand.Paused(true).isExplicitPlay(paused = false))
        assertFalse(PlayerCommand.PlayOrPause.isExplicitPlay(paused = false))
        for (command in listOf(
            PlayerCommand.NextMedia, PlayerCommand.PreviousMedia,
            PlayerCommand.LoadList(emptyList()), PlayerCommand.PlayFileInPlaylist("episode"),
        )) {
            val policy = PlaybackResumePolicy(initiallyScreenLocked = true)
            policy.userAction(
                allowsResume = command.playbackIntent(paused = true) == true,
                explicitPlay = command.isExplicitPlay(paused = true),
            )
            assertTrue(policy.pauseWhenRequired(true, true, backgroundEnabled = true))
        }
        assertEquals(false, PlayerCommand.NextMedia.playbackIntent(paused = true))
        assertEquals(false, PlayerCommand.PreviousMedia.playbackIntent(paused = true))
        assertEquals(true, PlayerCommand.NextMedia.playbackIntent(paused = false))
    }

    @Test fun delayedAudioActivationPreservesForegroundResumeAndUserPause() {
        val policy = PlaybackResumePolicy()
        policy.userAction(allowsResume = true)
        assertTrue(policy.enterBackground(playing = true, enabled = false))
        policy.audioSessionActivated()
        assertTrue(policy.enterForeground())

        policy.beginInterruption(playing = true)
        policy.userAction(allowsResume = false)
        policy.audioSessionActivated()
        assertTrue(policy.pauseWhenRequired(playing = true, ready = true, backgroundEnabled = true))
        assertFalse(policy.endInterruption(shouldResume = true, backgroundEnabled = true))
    }

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
