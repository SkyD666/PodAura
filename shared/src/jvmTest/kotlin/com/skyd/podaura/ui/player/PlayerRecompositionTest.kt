package com.skyd.podaura.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import com.skyd.podaura.ui.player.service.PlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerRecompositionTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun positionTicksOnlyRecomposeTheProgressLeaf() = runComposeUiTest {
        val source = MutableStateFlow(PlayerState(duration = 120L))
        var staticCompositions = 0
        var progressCompositions = 0

        setContent {
            StaticPlayerProbe(source) { staticCompositions++ }
            ProgressProbe(source) { progressCompositions++ }
        }
        waitForIdle()
        assertEquals(1, staticCompositions)
        assertEquals(1, progressCompositions)

        runOnIdle { source.value = source.value.copy(position = 1L) }
        waitForIdle()
        assertEquals(1, staticCompositions)
        assertEquals(2, progressCompositions)

        runOnIdle { source.value = source.value.copy(position = 2L, buffer = 10) }
        waitForIdle()
        assertEquals(1, staticCompositions)
        assertEquals(3, progressCompositions)

        runOnIdle { source.value = source.value.copy(paused = false) }
        waitForIdle()
        assertEquals(2, staticCompositions)
        assertEquals(3, progressCompositions)
    }

    @Composable
    private fun StaticPlayerProbe(source: MutableStateFlow<PlayerState>, composed: () -> Unit) {
        val flow = remember(source) { source.withoutHotPlayerValues() }
        val state by flow.collectAsState(source.value)
        state.paused
        SideEffect(composed)
    }

    @Composable
    private fun ProgressProbe(source: MutableStateFlow<PlayerState>, composed: () -> Unit) {
        val flow = remember(source) { source.playerProgressValues() }
        val progress by flow.collectAsState(
            PlayerProgress(source.value.position, source.value.duration, source.value.buffer)
        )
        progress.position
        SideEffect(composed)
    }
}
