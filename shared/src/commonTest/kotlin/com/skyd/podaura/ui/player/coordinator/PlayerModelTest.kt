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
}
