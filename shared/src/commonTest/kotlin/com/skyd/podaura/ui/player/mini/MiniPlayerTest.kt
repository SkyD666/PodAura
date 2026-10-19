package com.skyd.podaura.ui.player.mini

import com.skyd.podaura.ui.player.PlaybackEnd
import com.skyd.podaura.ui.player.PlaybackEndReason
import com.skyd.podaura.ui.player.coordinator.PlaybackPreparation
import com.skyd.podaura.ui.player.coordinator.PlayerEngineState
import com.skyd.podaura.ui.player.service.PlayerState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MiniPlayerTest {
    @Test
    fun firstLoadShowsSpinnerButErrorsAllowRetry() {
        val empty = PlayerState()
        for (engine in listOf(PlayerEngineState.Initializing, PlayerEngineState.AwaitingMedia, PlayerEngineState.LoadingMedia)) {
            assertTrue(miniPlayerLoading(empty, engine, PlaybackPreparation.Idle))
        }
        assertFalse(miniPlayerLoading(empty, PlayerEngineState.Failed("init"), PlaybackPreparation.Idle))
        assertFalse(miniPlayerLoading(empty, PlayerEngineState.AwaitingMedia, PlaybackPreparation.Failed))
        val failed = empty.copy(loading = true, lastPlaybackEnd = PlaybackEnd(PlaybackEndReason.Error))
        assertFalse(miniPlayerLoading(failed, PlayerEngineState.LoadingMedia, PlaybackPreparation.Idle))
        // A new request must show loading even if the previous media failed.
        assertTrue(miniPlayerLoading(failed, PlayerEngineState.Ready, PlaybackPreparation.Loading))
        assertFalse(miniPlayerLoading(empty, PlayerEngineState.Ready, PlaybackPreparation.Idle))
    }

    @Test
    fun bufferingDoesNotBlockPlaybackControlsOnceTheEngineIsReady() {
        for (paused in listOf(false, true)) {
            val buffering = PlayerState(mediaStarted = true, loading = true, paused = paused)
            assertFalse(miniPlayerLoading(buffering, PlayerEngineState.Ready, PlaybackPreparation.Idle))
            // Preparing a new request still disables controls, even with an existing ready engine.
            assertTrue(miniPlayerLoading(buffering, PlayerEngineState.Ready, PlaybackPreparation.Loading))
        }
    }

    @Test
    fun progressIsZeroWhenDurationIsNotPositive() {
        assertEquals(0f, miniPlayerProgress(position = 10, duration = 0))
        assertEquals(0f, miniPlayerProgress(position = 10, duration = -1))
    }

    @Test
    fun progressIsClampedToValidRange() {
        assertEquals(0f, miniPlayerProgress(position = -10, duration = 100))
        assertEquals(0.5f, miniPlayerProgress(position = 50, duration = 100))
        assertEquals(1f, miniPlayerProgress(position = 110, duration = 100))
    }

    @Test
    fun visibilityRequiresSessionAndVisibleHost() {
        assertTrue(isMiniPlayerVisible(sessionAvailable = true, hostVisible = true))
        assertFalse(isMiniPlayerVisible(sessionAvailable = false, hostVisible = true))
        assertFalse(isMiniPlayerVisible(sessionAvailable = true, hostVisible = false))
    }
}
