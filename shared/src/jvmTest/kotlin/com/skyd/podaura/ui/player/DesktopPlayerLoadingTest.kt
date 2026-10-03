package com.skyd.podaura.ui.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.skyd.podaura.ui.player.coordinator.PlayerEngineState
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopPlayerLoadingTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun surfaceBootstrapsBeforeFileLoadedEvenWithoutVideoContent() = runDesktopComposeUiTest {
        var engineState: PlayerEngineState by mutableStateOf(PlayerEngineState.Initializing)
        val fileLoaded = CompletableDeferred<Unit>()
        var surfaceMounts = 0
        var surfaceDisposals = 0

        setContent {
            DesktopPlayerContent(
                engineState = engineState,
                modifier = Modifier.fillMaxSize(),
                bootstrapSurface = { modifier ->
                    DisposableEffect(Unit) {
                        surfaceMounts++
                        onDispose { surfaceDisposals++ }
                    }
                    LaunchedEffect(Unit) {
                        fileLoaded.await()
                        engineState = PlayerEngineState.Ready
                    }
                    Text("Bootstrap surface", modifier)
                },
                // Audio's normal content never mounts a video surface.
                content = { Text("Audio thumbnail") },
            )
        }

        onNodeWithText("Bootstrap surface").assertDoesNotExist()
        runOnIdle { engineState = PlayerEngineState.AwaitingMedia }
        onNodeWithText("Bootstrap surface").assertExists()
        runOnIdle { engineState = PlayerEngineState.LoadingMedia }
        onNodeWithText("Bootstrap surface").assertExists()
        runOnIdle {
            assertEquals(1, surfaceMounts)
            assertEquals(0, surfaceDisposals)
            fileLoaded.complete(Unit)
        }
        onNodeWithText("Bootstrap surface").assertDoesNotExist()
        onNodeWithText("Audio thumbnail").assertExists()
        runOnIdle {
            assertEquals(PlayerEngineState.Ready, engineState)
            assertEquals(1, surfaceDisposals)
        }

        for (state in listOf(
            PlayerEngineState.Failed("Initialization failed"),
            PlayerEngineState.Destroyed,
            PlayerEngineState.Initializing,
        )) {
            runOnIdle { engineState = state }
            onNodeWithText("Bootstrap surface").assertDoesNotExist()
        }
    }
}
