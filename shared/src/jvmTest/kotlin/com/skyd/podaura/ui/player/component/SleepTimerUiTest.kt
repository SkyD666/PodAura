package com.skyd.podaura.ui.player.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skyd.podaura.model.preference.player.BackgroundPlayPreference
import com.skyd.podaura.ui.player.SleepTimer
import com.skyd.podaura.ui.player.SleepTimerOption
import com.skyd.podaura.ui.player.component.state.dialog.DialogState
import com.skyd.podaura.ui.player.component.state.dialog.OnDialogVisibilityChanged
import com.skyd.podaura.ui.player.component.state.dialog.SpeedDialogState
import com.skyd.podaura.ui.player.component.state.dialog.seconds.ForwardSecondsDialogState
import com.skyd.podaura.ui.player.component.state.dialog.seconds.ReplaySecondsDialogState
import com.skyd.podaura.ui.player.component.state.dialog.track.AudioTrackDialogState
import com.skyd.podaura.ui.player.component.state.dialog.track.SubtitleTrackDialogState
import com.skyd.podaura.ui.player.land.controller.bar.TopBar
import org.jetbrains.skia.Image
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class SleepTimerUiTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun timerCanBeChangedAndCancelledFromNarrowAndLandscapePanels() {
        for ((width, height) in listOf(360 to 740, 800 to 360)) {
            runDesktopComposeUiTest(width = width, height = height) {
                val timer = SleepTimer()
                var showDialog by mutableStateOf(false)
                setContent {
                    val sleepTimerState by timer.state.collectAsStateWithLifecycle()
                    val dialogState = remember {
                        DialogState(
                            speedDialogState = { SpeedDialogState.initial },
                            audioTrackDialogState = { AudioTrackDialogState.initial },
                            subtitleTrackDialogState = { SubtitleTrackDialogState.initial },
                            forwardSecondsDialogState = { ForwardSecondsDialogState.initial },
                            replaySecondsDialogState = { ReplaySecondsDialogState.initial },
                            sleepTimerState = { sleepTimerState },
                        )
                    }
                    val onDialogVisibilityChanged = remember {
                        OnDialogVisibilityChanged(
                            onSpeedDialog = {},
                            onSubtitleTrackDialog = {},
                            onAudioTrackDialog = {},
                            onSubtitleSettingDialog = {},
                            onAudioSettingDialog = {},
                            onReplaySecondDialog = {},
                            onForwardSecondDialog = {},
                            onSleepTimerDialog = { showDialog = it },
                        )
                    }
                    CompositionLocalProvider(BackgroundPlayPreference.local provides false) {
                        MaterialTheme(colorScheme = if (width == 360) lightColorScheme() else darkColorScheme()) {
                            Surface(Modifier.fillMaxSize()) {
                                Column {
                                    TopBar(
                                        title = "A long podcast title for a quiet evening",
                                        onExitFullscreen = {},
                                        dialogState = dialogState,
                                        onDialogVisibilityChanged = onDialogVisibilityChanged,
                                    )
                                    SleepTimerButton(
                                        sleepTimerState = { sleepTimerState },
                                        onClick = { showDialog = true },
                                        activeOnly = true,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                if (showDialog) SleepTimerDialog(
                                    sleepTimerState = dialogState.sleepTimerState,
                                    onSelect = timer::set,
                                    onDismissRequest = { onDialogVisibilityChanged.onSleepTimerDialog(false) },
                                )
                            }
                        }
                    }
                }
                onNodeWithContentDescription("Sleep timer").performClick()
                onNodeWithText("Background playback is off. Leaving the player may pause playback or end the session.")
                    .assertIsDisplayed()
                val output = File("build/reports/sleep-timer/panel-$width.png")
                output.parentFile.mkdirs()
                output.writeBytes(Image.makeFromBitmap(onNode(isDialog()).captureToImage().asSkiaBitmap())
                    .encodeToData()!!.bytes)
                onNodeWithText("Playback settings").performScrollTo().performClick()
                onNodeWithText("Background playback").assertIsDisplayed()
                onNodeWithText("Back").performClick()
                onNodeWithText("In 30 minutes").performScrollTo().performClick()
                onNode(isDialog()).assertDoesNotExist()
                runOnIdle { assertEquals(SleepTimerOption.AfterMinutes(30), timer.state.value.option) }
                val selectedState = runOnIdle { timer.state.value }
                waitUntil(timeoutMillis = 5_000L) {
                    onAllNodes(hasContentDescription("Sleep timer, 29:59"))
                        .fetchSemanticsNodes().isNotEmpty()
                }
                runOnIdle { assertEquals(selectedState, timer.state.value) }
                // Reopen through the top-bar status (the mini-player has the same accessible label).
                onAllNodes(hasContentDescription("Sleep timer, 29:59"))[0]
                    .performClick()
                onNodeWithText("When current media ends").performScrollTo().performClick()
                runOnIdle { assertEquals(SleepTimerOption.EndOfMedia, timer.state.value.option) }
                File("build/reports/sleep-timer/status-$width.png").writeBytes(
                    Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).encodeToData()!!.bytes,
                )
                onAllNodes(hasContentDescription("Sleep timer, When current media ends"))[0]
                    .performClick()
                onNodeWithText("Off").performScrollTo().performClick()
                runOnIdle { assertEquals(SleepTimerOption.Off, timer.state.value.option) }
                onNodeWithContentDescription("Sleep timer").assertIsDisplayed()
            }
        }
    }
}
