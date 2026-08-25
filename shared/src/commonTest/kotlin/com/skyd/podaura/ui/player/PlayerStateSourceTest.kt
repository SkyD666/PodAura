package com.skyd.podaura.ui.player

import com.skyd.podaura.ui.player.service.PlayerState
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerStateSourceTest {
    @Test
    fun hotValuesDoNotPropagateIntoTheStaticPlayerTree() = runTest {
        val initial = PlayerState(duration = 120L)
        val emissions = flowOf(
            initial,
            initial.copy(position = 1L),
            initial.copy(position = 2L, buffer = 20),
            initial.copy(position = 2L, buffer = 20, zoom = 2f, rotate = 90f),
            initial.copy(position = 2L, buffer = 20, zoom = 2f, rotate = 90f, paused = false),
        ).withoutHotPlayerValues().toList()

        assertEquals(2, emissions.size)
        assertEquals(initial, emissions[0])
        assertEquals(initial.copy(paused = false), emissions[1])
    }

    @Test
    fun progressPropagatesExactlyOncePerDistinctProgressValue() = runTest {
        val initial = PlayerState(duration = 120L)
        val emissions = flowOf(
            initial,
            initial.copy(paused = false),
            initial.copy(position = 1L),
            initial.copy(position = 1L, zoom = 2f),
            initial.copy(position = 1L, buffer = 15),
        ).playerProgressValues().toList()

        assertEquals(
            listOf(
                PlayerProgress(position = 0L, duration = 120L, buffer = 0),
                PlayerProgress(position = 1L, duration = 120L, buffer = 0),
                PlayerProgress(position = 1L, duration = 120L, buffer = 15),
            ),
            emissions,
        )
    }
}
