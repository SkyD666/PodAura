package com.skyd.podaura.ui.player.coordinator

import com.skyd.podaura.ui.player.PlayerEvent
import com.skyd.podaura.ui.player.service.PlayerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class PlayerModelTest {
    @Test
    fun shutdownClearsTheCurrentPlaybackSession() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val model = PlayerModel()
            model.onEvent(PlayerEvent.StartFile("episode.mp3"))
            model.onEvent(PlayerEvent.Position(42))
            model.onEvent(PlayerEvent.Paused(false))
            advanceUntilIdle()

            model.onEvent(PlayerEvent.Shutdown)
            advanceUntilIdle()

            assertEquals(PlayerState(), model.playerState.value)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun startingANewFileClearsMediaSpecificStateFromThePreviousFile() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val model = PlayerModel()
            model.onEvent(PlayerEvent.StartFile("first.mp3"))
            model.onEvent(PlayerEvent.Seekable(true))
            model.onEvent(PlayerEvent.Duration(120L))
            model.onEvent(PlayerEvent.Position(80L))
            model.onEvent(PlayerEvent.MediaTitle("Old title"))
            model.onEvent(PlayerEvent.Artist("Old artist"))
            model.onEvent(PlayerEvent.Album("Old album"))
            advanceUntilIdle()

            model.onEvent(PlayerEvent.StartFile("second.mp3"))
            advanceUntilIdle()

            val state = model.playerState.value
            assertEquals("second.mp3", state.path)
            assertEquals(0L, state.position)
            assertEquals(0L, state.duration)
            assertFalse(state.seekable)
            assertNull(state.mediaTitle)
            assertNull(state.artist)
            assertNull(state.album)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
