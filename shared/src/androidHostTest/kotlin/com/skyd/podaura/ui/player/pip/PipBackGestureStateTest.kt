package com.skyd.podaura.ui.player.pip

import androidx.activity.BackEventCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PipBackGestureStateTest {
    @Test
    fun cancelledGestureRestoresPlayerWithoutCommittingAndCanBeRetried() = runTest {
        val state = PipBackGestureState()
        var committed = false
        assertFailsWith<CancellationException> {
            state.collect(flow {
                emit(event(0.6f, BackEventCompat.EDGE_RIGHT))
                assertEquals(0.6f, state.progress)
                assertEquals(BackEventCompat.EDGE_RIGHT, state.swipeEdge)
                assertTrue(state.inProgress)
                throw CancellationException("Gesture cancelled")
            })
            committed = true
        }
        assertFalse(committed)
        assertFalse(state.inProgress)
        assertEquals(0f, state.progress)

        state.collect(flowOf(event(0.8f, BackEventCompat.EDGE_LEFT)))
        assertEquals(0.8f, state.progress)
        assertEquals(BackEventCompat.EDGE_LEFT, state.swipeEdge)
    }

    @Test
    fun buttonBackCompletesWithoutLeavingAGestureTransform() = runTest {
        val state = PipBackGestureState()
        state.collect(flowOf(event(0.5f, BackEventCompat.EDGE_LEFT)))
        state.collect(emptyFlow())
        assertEquals(0f, state.progress)
    }

    private fun event(progress: Float, edge: Int) = BackEventCompat(
        touchX = 0f,
        touchY = 0f,
        progress = progress,
        swipeEdge = edge,
    )
}
