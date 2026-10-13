package com.skyd.podaura.ui.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidPlaybackEventsTest {
    @Test
    fun directLockUsesTheScreenPreferenceInEitherNotificationOrder() {
        for (broadcastFirst in listOf(false, true)) {
            for (pauseOnScreenOff in listOf(false, true)) {
                for (backgroundEnabled in listOf(false, true)) {
                    var playing = true
                    val policy = PlaybackResumePolicy(
                        independentScreenPlayback = true, pauseOnScreenOff = pauseOnScreenOff,
                    )
                    val events = AndroidPlaybackEvents(
                        policy, screenOff = false, playing = { playing }, ready = { true },
                        backgroundEnabled = { backgroundEnabled }, systemPause = { playing = !it },
                    )
                    if (broadcastFirst) events.updateScreenState(off = true, locked = true)
                    // onStop samples the already-off screen even when its broadcast is delayed.
                    events.setPlayerVisible(visible = false, off = true, locked = true)
                    if (!broadcastFirst) events.updateScreenState(off = true, locked = true)
                    assertEquals(!pauseOnScreenOff, playing)
                    events.setPlayerVisible(visible = true, off = false, locked = false)
                    assertTrue(playing)
                }
            }
        }
    }

    @Test
    fun homePauseIsNotReclassifiedAsDirectLock() {
        var playing = true
        val policy = PlaybackResumePolicy(independentScreenPlayback = true, pauseOnScreenOff = false)
        val events = AndroidPlaybackEvents(
            policy, screenOff = false, playing = { playing }, ready = { true },
            backgroundEnabled = { false }, systemPause = { playing = !it },
        )
        events.setPlayerVisible(visible = false, off = false, locked = false)
        assertFalse(playing)
        events.updateScreenState(off = true, locked = true)
        assertFalse(playing)
        playing = true // A late load also remains subject to the Home pause.
        events.enforcePolicy()
        assertFalse(playing)
        events.setPlayerVisible(visible = true, off = false, locked = false)
        assertTrue(playing)
    }

    @Test
    fun duplicateLockNotificationDoesNotRevokePlayButANewScreenOffDoes() {
        for (broadcastFirst in listOf(false, true)) {
            var playing = true
            val policy = PlaybackResumePolicy(independentScreenPlayback = true)
            val events = AndroidPlaybackEvents(
                policy, screenOff = false, playing = { playing }, ready = { true },
                backgroundEnabled = { false }, systemPause = { playing = !it },
            )
            if (broadcastFirst) events.updateScreenState(off = true, locked = true)
            else events.setPlayerVisible(visible = false, off = true, locked = true)
            assertFalse(playing)
            policy.userAction(allowsResume = true, explicitPlay = true)
            playing = true
            if (broadcastFirst) events.setPlayerVisible(visible = false, off = true, locked = true)
            else events.updateScreenState(off = true, locked = true)
            assertTrue(playing)
            events.updateScreenState(off = false, locked = true)
            assertTrue(playing)
            events.updateScreenState(off = true, locked = true)
            assertFalse(playing)
        }
    }
}
