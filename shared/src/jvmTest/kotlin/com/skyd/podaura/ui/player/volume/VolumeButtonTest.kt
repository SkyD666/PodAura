package com.skyd.podaura.ui.player.volume

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.skyd.podaura.ui.player.land.controller.bar.VolumeButton
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jetbrains.skia.Image

@OptIn(ExperimentalTestApi::class)
class VolumeButtonTest {
    @Test
    fun keyboardCanMuteThenTabIntoSliderAndLeave() = runDesktopComposeUiTest(width = 600, height = 450) {
        val controller = object : VolumeController {
            override val range = 0f..100f
            override var value by mutableFloatStateOf(50f)
            override var muted by mutableStateOf(false)
            override fun toggleMute() { muted = !muted }
        }
        lateinit var mode: InputModeManager
        setContent {
            mode = LocalInputModeManager.current
            MaterialTheme {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                    VolumeButton(controller, true) {}
                }
            }
        }
        runOnIdle { mode.requestInputMode(InputMode.Keyboard) }
        onNodeWithContentDescription("Mute").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithText("50%").assertIsDisplayed()
        onNodeWithContentDescription("Mute").performKeyInput { pressKey(Key.Spacebar) }
        runOnIdle { assertTrue(controller.muted) }
        onNodeWithContentDescription("Unmute").performKeyInput { pressKey(Key.Tab) }
        onNodeWithContentDescription("Volume").assertIsFocused()
        onNodeWithContentDescription("Volume").performKeyInput { pressKey(Key.DirectionUp) }
        runOnIdle { assertTrue(controller.value > 50f) }
        onNodeWithContentDescription("Volume").performKeyInput { pressKey(Key.Escape) }
        onNodeWithContentDescription("Unmute").assertIsFocused()
        onNodeWithContentDescription("Unmute").performKeyInput { pressKey(Key.Tab) }
        onNodeWithContentDescription("Volume").performKeyInput {
            keyDown(Key.ShiftLeft); pressKey(Key.Tab); keyUp(Key.ShiftLeft)
        }
        onNodeWithContentDescription("Unmute").assertIsFocused()
        onNodeWithContentDescription("Unmute").performKeyInput { pressKey(Key.Tab) }
        onNodeWithContentDescription("Volume").assertIsFocused()
        onNodeWithContentDescription("Unmute").performMouseInput { click() }
        runOnIdle { assertTrue(!controller.muted) }
    }

    @Test
    fun hoverShowsActualBoostAndSliderAdjustmentUnmutesWithinUserRange() = runDesktopComposeUiTest(width = 600, height = 450) {
        val controller = object : VolumeController {
            override val range = 0f..100f
            var level by mutableFloatStateOf(150f)
            var silent by mutableStateOf(false)
            override var value: Float
                get() = level
                set(value) { level = value; if (value > 0f) silent = false }
            override val muted get() = silent
            override fun toggleMute() { silent = !silent }
        }
        var active = false
        setContent {
            MaterialTheme {
                Box(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(20.dp),
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    VolumeButton(controller, true) { active = it }
                }
            }
        }
        onNodeWithContentDescription("Mute").performMouseInput { moveTo(center) }
        onNodeWithText("150%").assertIsDisplayed()
        runOnIdle { assertTrue(active); assertEquals(150f, controller.value) }
        onNode(isPopup() and hasAnyDescendant(hasContentDescription("Volume")))
            .saveVolumeImage("volume-popup")
        onNodeWithContentDescription("Mute").saveVolumeImage("icon-high")
        onNodeWithContentDescription("Mute").performClick()
        onNodeWithContentDescription("Unmute").assertExists()
        onNodeWithText("Muted").assertIsDisplayed()
        onNodeWithText("150%").assertDoesNotExist()
        onNode(isPopup() and hasAnyDescendant(hasContentDescription("Volume")))
            .saveVolumeImage("volume-muted")
        onNodeWithContentDescription("Unmute").saveVolumeImage("icon-muted")
        onNodeWithContentDescription("Volume").performSemanticsAction(SemanticsActions.SetProgress) { it(40f) }
        runOnIdle { assertEquals(40f, controller.value); assertTrue(!controller.muted) }
        onNodeWithText("40%").assertIsDisplayed()
        onNodeWithText("Muted").assertDoesNotExist()
        onNodeWithContentDescription("Mute").saveVolumeImage("icon-low")
        onNodeWithContentDescription("Volume").performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
        runOnIdle { assertEquals(0f, controller.value); assertTrue(!controller.muted) }
        onNodeWithText("0%").assertIsDisplayed()
        onNodeWithText("Muted").assertDoesNotExist()
        onNodeWithContentDescription("Unmute").saveVolumeImage("icon-zero")
        onNode(isPopup() and hasAnyDescendant(hasContentDescription("Volume")))
            .saveVolumeImage("volume-zero")
    }
}

private fun SemanticsNodeInteraction.saveVolumeImage(name: String) {
    val output = File("build/reports/player-volume/$name.png")
    output.parentFile.mkdirs()
    output.writeBytes(Image.makeFromBitmap(captureToImage().asSkiaBitmap()).encodeToData()!!.bytes)
}
