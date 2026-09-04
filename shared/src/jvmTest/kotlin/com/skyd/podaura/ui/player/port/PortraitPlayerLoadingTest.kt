package com.skyd.podaura.ui.player.port

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.skyd.podaura.ui.component.calculateWindowSizeClass
import com.skyd.podaura.ui.local.LocalWindowSizeClass
import com.skyd.podaura.ui.player.PlayerArticleContextState
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.ui.player.component.state.PlayStateCallback
import com.skyd.podaura.ui.player.component.state.dialog.OnDialogVisibilityChanged
import com.skyd.podaura.ui.player.port.controller.Controller
import com.skyd.podaura.ui.player.service.PlayerState
import kotlin.test.Test
import kotlin.test.assertEquals

class PortraitPlayerLoadingTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun primaryControlIconStaysCenteredDuringLoadingCrossfade() =
        runDesktopComposeUiTest(width = 400, height = 240) {
            mainClock.autoAdvance = false
            var playState by mutableStateOf(
                PlayState(
                    isPlaying = true,
                    isSeeking = false,
                    state = PlayerState(mediaStarted = true, paused = false),
                )
            )

            setContent {
                MaterialTheme {
                    Controller(
                        playState = playState,
                        playStateCallback = EmptyPlayStateCallback,
                        onDialogVisibilityChanged = EmptyDialogVisibilityChanged,
                    )
                }
            }
            waitForIdle()
            val initialCenter = onNodeWithContentDescription(
                label = "Pause",
                useUnmergedTree = true,
            ).fetchSemanticsNode().boundsInRoot.center

            runOnIdle {
                playState = playState.copy(state = playState.state.copy(loading = true))
            }
            mainClock.advanceTimeBy(PLAYER_PRESENTATION_CROSSFADE_MILLIS / 2L)
            waitForIdle()
            val transitionCenter = onNodeWithContentDescription(
                label = "Pause",
                useUnmergedTree = true,
            ).fetchSemanticsNode().boundsInRoot.center

            assertEquals(initialCenter, transitionCenter)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun compactLoadingKeepsBackEnabledAndDisablesPlayerActions() =
        runDesktopComposeUiTest(width = 400, height = 800) {
            var backCount = 0

            setContent {
                TestPlayer(
                    presentationState = PlayerPresentationState.Loading,
                    onBack = { backCount++ },
                )
            }

            onNodeWithContentDescription("Back").assertIsEnabled().performClick()
            onNodeWithContentDescription("More").assertIsNotEnabled()
            onNodeWithContentDescription("Skip previous").assertIsNotEnabled()
            onNodeWithContentDescription("Playlist").assertIsNotEnabled()
            onNodeWithContentDescription("Fullscreen").assertIsNotEnabled()
            runOnIdle { assertEquals(1, backCount) }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun expandedFailureShowsDetailsAndOnlyEnablesRetryAndBack() =
        runDesktopComposeUiTest(width = 1_024, height = 768) {
            var retryCount = 0

            setContent {
                TestPlayer(
                    presentationState = PlayerPresentationState.Failed("Decoder unavailable"),
                    onRetry = { retryCount++ },
                )
            }

            onNodeWithText("Playback failed").assertExists()
            onNodeWithText("Decoder unavailable").assertExists()
            onNodeWithContentDescription("Back").assertIsEnabled()
            onNodeWithContentDescription("Retry").assertIsEnabled().performClick()
            onNodeWithContentDescription("More").assertIsNotEnabled()
            onNodeWithContentDescription("Fullscreen").assertIsNotEnabled()
            runOnIdle { assertEquals(1, retryCount) }
        }

    @Composable
    private fun TestPlayer(
        presentationState: PlayerPresentationState,
        onBack: () -> Unit = {},
        onRetry: () -> Unit = {},
    ) {
        CompositionLocalProvider(
            LocalWindowSizeClass provides calculateWindowSizeClass(),
        ) {
            MaterialTheme {
                PortraitPlayerView(
                    playState = PlayState.initial,
                    articleContextState = PlayerArticleContextState(),
                    playStateCallback = EmptyPlayStateCallback,
                    onDialogVisibilityChanged = EmptyDialogVisibilityChanged,
                    onBack = onBack,
                    onEnterFullscreen = {},
                    playerContent = {},
                    onSetArticleFavorite = {},
                    snackbarHostState = remember { SnackbarHostState() },
                    presentationState = presentationState,
                    onRetry = onRetry,
                )
            }
        }
    }

    private companion object {
        val EmptyPlayStateCallback = PlayStateCallback(
            onPlayStateChanged = {},
            onPlayOrPause = {},
            onSeekTo = {},
            onSeekBy = {},
            onSpeedChanged = {},
            onPreviousMedia = {},
            onNextMedia = {},
            onCycleLoop = {},
            onShuffle = {},
            onPlayFileInPlaylist = {},
            onRemoveFromPlaylist = {},
        )

        val EmptyDialogVisibilityChanged = OnDialogVisibilityChanged(
            onSpeedDialog = {},
            onSubtitleTrackDialog = {},
            onAudioTrackDialog = {},
            onSubtitleSettingDialog = {},
            onAudioSettingDialog = {},
            onReplaySecondDialog = {},
            onForwardSecondDialog = {},
        )
    }
}
