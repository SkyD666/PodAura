package com.skyd.podaura.ui.player.component.dialog

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.ui.player.component.state.dialog.SpeedDialogCallback
import com.skyd.podaura.ui.player.component.state.dialog.SpeedDialogState
import org.jetbrains.skia.Image
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalTestApi::class)
class SpeedDialogTest {
    @Test
    fun presetClicksToleratePointerJitterButDeliberateMouseDragsStillFineTune() =
        runDesktopComposeUiTest(width = 390, height = 720) {
            val applied = mutableListOf<Float>()
            var touchSlop = 0f
            var tickSpacing = 0f
            setContent {
                touchSlop = LocalViewConfiguration.current.touchSlop
                tickSpacing = with(LocalDensity.current) { 12.dp.toPx() }
                MaterialTheme {
                    SpeedDialog(
                        onDismissRequest = {},
                        playState = { PlayState.initial },
                        speedDialogState = { SpeedDialogState(true) },
                        speedDialogCallback = SpeedDialogCallback { applied += it },
                    )
                }
            }
            val preset = onNode(hasText("1.25×") and hasClickAction())
            preset.performMouseInput {
                moveTo(center)
                press()
                moveTo(center + Offset(touchSlop / 2f, 0f))
                release()
                exit()
            }
            preset.assertIsSelected()
            onNode(hasText("1×") and hasClickAction()).assertIsNotSelected()
            onNodeWithText("Swipe to fine-tune").assertIsDisplayed()
            runOnIdle { assertEquals(listOf(1.25f), applied) }
            preset.performTouchInput {
                down(center)
                moveBy(Offset(-touchSlop / 2f, 0f))
                up()
            }
            preset.assertIsSelected()
            // A single-select group cannot deselect its current preset.
            runOnIdle { assertEquals(listOf(1.25f), applied) }

            val control = onNodeWithContentDescription("Speed")
            val start = preset.fetchSemanticsNode().boundsInRoot.center -
                control.fetchSemanticsNode().boundsInRoot.topLeft
            control.performMouseInput {
                moveTo(start)
                press()
                moveTo(start - Offset(touchSlop + tickSpacing * 2f, 0f))
            }
            onNodeWithText("Swipe to fine-tune").assertDoesNotExist()
            onNodeWithText("1.35×").assertIsDisplayed()
            runOnIdle { assertEquals(listOf(1.25f), applied) }
            control.performMouseInput { release() }
            runOnIdle { assertEquals(listOf(1.25f, 1.35f), applied) }
        }

    @Test
    fun presetsAndFirstSwipeCommitAtTheRightTimeAndReopeningRestoresPresets() =
        runDesktopComposeUiTest(width = 390, height = 720) {
            var show by mutableStateOf(true)
            var playState by mutableStateOf(PlayState.initial.copy(state = PlayState.initial.state.copy(speed = 1.35f)))
            val applied = mutableListOf<Float>()
            var tickSpacing = 0f
            var touchSlop = 0f
            var densityScale by mutableStateOf(1f)
            setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density * densityScale)) {
                    tickSpacing = with(LocalDensity.current) { 12.dp.toPx() }
                    touchSlop = LocalViewConfiguration.current.touchSlop
                    MaterialTheme {
                        SpeedDialog(
                            onDismissRequest = { show = false },
                            playState = { playState },
                            speedDialogState = { SpeedDialogState(show) },
                            speedDialogCallback = SpeedDialogCallback {
                                applied += it
                                playState = playState.copy(state = playState.state.copy(speed = it))
                            },
                        )
                    }
                }
            }
            onNodeWithText("1.35×").assertIsDisplayed()
            onNode(hasText("1.25×") and hasClickAction()).performTouchInput { click() }
            onNode(hasText("1.25×") and hasClickAction()).assertIsSelected()
            runOnIdle { assertEquals(listOf(1.25f), applied) }
            val control = onNodeWithContentDescription("Speed")
            val presetBounds = onNode(hasText("1.5×") and hasClickAction()).fetchSemanticsNode().boundsInRoot
            val controlBounds = control.fetchSemanticsNode().boundsInRoot
            // Start on a clickable preset: crossing touch slop must cancel its click without
            // cancelling the parent gesture when the presets disappear.
            control.performTouchInput {
                down(presetBounds.center - controlBounds.topLeft)
                moveBy(Offset(-touchSlop - 1f, 0f))
            }
            onNodeWithText("Swipe to fine-tune").assertDoesNotExist()
            onNodeWithText("1.25×").assertIsDisplayed()
            runOnIdle { assertEquals(listOf(1.25f), applied) }
            mainClock.advanceTimeBy(1_000)
            val beforeFineMovement = control.pixels()
            control.performTouchInput { moveBy(Offset(-tickSpacing / 6f, 0f)) }
            onNodeWithText("1.25×").assertIsDisplayed()
            val afterFineMovement = control.pixels()
            assertFalse(beforeFineMovement.contentEquals(afterFineMovement),
                "The ruler must move within a 0.05 step, even while the selected speed is unchanged")
            control.performTouchInput { moveBy(Offset(-tickSpacing * (3f - 1f / 6f), 0f)) }
            onNodeWithText("1.4×").assertIsDisplayed()
            runOnIdle { assertEquals(listOf(1.25f), applied) }
            control.performTouchInput { up() }
            runOnIdle { assertEquals(listOf(1.25f, 1.4f), applied) }
            onNodeWithText("Swipe to fine-tune").assertDoesNotExist()

            // The ruler follows the finger: dragging right lowers the speed.
            control.performTouchInput {
                down(center)
                moveBy(Offset(touchSlop + tickSpacing * 2f, 0f))
            }
            onNodeWithText("1.3×").assertIsDisplayed()
            runOnIdle { assertEquals(listOf(1.25f, 1.4f), applied) }
            control.performTouchInput { up() }
            runOnIdle { assertEquals(listOf(1.25f, 1.4f, 1.3f), applied) }

            // A real pointer-input restart cancels the draft; desktop's injected cancel is a no-op.
            control.performTouchInput {
                down(center)
                moveBy(Offset(-touchSlop - tickSpacing * 2f, 0f))
            }
            onNodeWithText("1.4×").assertIsDisplayed()
            runOnIdle { densityScale = 1.1f }
            onNodeWithText("1.3×").assertIsDisplayed()
            control.performTouchInput { up() }
            runOnIdle { assertEquals(listOf(1.25f, 1.4f, 1.3f), applied) }

            runOnIdle { show = false }
            onNode(isDialog()).assertDoesNotExist()
            runOnIdle { show = true }
            onNodeWithText("Swipe to fine-tune").assertIsDisplayed()
            onNodeWithText("1.3×").assertIsDisplayed()
            onNodeWithContentDescription("Reset").performClick()
            runOnIdle { assertEquals(1f, applied.last()) }
        }

    @Test
    fun smallReleaseOffsetAnimatesBeforeItSettles() = runDesktopComposeUiTest(width = 360, height = 740) {
        var touchSlop = 0f
        var tickSpacing = 0f
        val applied = mutableListOf<Float>()
        setContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            tickSpacing = with(LocalDensity.current) { 12.dp.toPx() }
            MaterialTheme {
                SpeedDialog(
                    onDismissRequest = {},
                    playState = { PlayState.initial.copy(state = PlayState.initial.state.copy(speed = 1.25f)) },
                    speedDialogState = { SpeedDialogState(true) },
                    speedDialogCallback = SpeedDialogCallback { applied += it },
                )
            }
        }
        val control = onNodeWithContentDescription("Speed")
        control.performTouchInput {
            down(center)
            moveBy(Offset(-touchSlop - tickSpacing / 6f, 0f))
        }
        mainClock.advanceTimeBy(1_000)
        onNodeWithText("1.25×").assertIsDisplayed()
        mainClock.autoAdvance = false
        control.performTouchInput { up() }
        mainClock.advanceTimeBy(32)
        val settling = control.pixels()
        mainClock.advanceTimeBy(1_000)
        val settled = control.pixels()
        assertFalse(settling.contentEquals(settled),
            "Releasing less than 0.01x from a tick must still settle smoothly")
        runOnIdle { assertEquals(listOf(1.25f), applied) }
        mainClock.autoAdvance = true
    }

    @Test
    fun keyboardAndAccessibilityReachTheFullRangeAcrossSizesAndThemes() {
        for ((name, width, height, fontScale) in listOf(
            LayoutCase("phone-light", 360, 740),
            LayoutCase("desktop-dark", 900, 600),
            LayoutCase("large-type", 360, 740, 2f),
            LayoutCase("narrow-large-type", 300, 900, 2f),
        )) runDesktopComposeUiTest(width = width, height = height) {
            var applied = 1f
            setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                    MaterialTheme(colorScheme = if (name == "desktop-dark") darkColorScheme() else lightColorScheme()) {
                        SpeedDialog(
                            onDismissRequest = {},
                            playState = { PlayState.initial },
                            speedDialogState = { SpeedDialogState(true) },
                            speedDialogCallback = SpeedDialogCallback { applied = it },
                        )
                    }
                }
            }
            for (label in listOf("0.75×", "1×", "1.25×", "1.5×", "2×")) {
                onNode(hasText(label) and hasClickAction()).assertIsDisplayed()
            }
            val output = File("build/reports/speed-dialog/$name-presets.png")
            output.parentFile.mkdirs()
            output.writeBytes(Image.makeFromBitmap(onNode(isDialog()).captureToImage().asSkiaBitmap()).encodeToData()!!.bytes)
            val control = onNodeWithContentDescription("Speed")
            val presetBounds = control.fetchSemanticsNode().boundsInRoot
            onNode(hasText("1.25×") and hasClickAction())
                .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            control.performKeyInput { keyDown(Key.DirectionRight); keyUp(Key.DirectionRight) }
            control.assertIsFocused()
            runOnIdle { assertEquals(1.05f, applied) }
            control.performKeyInput { keyDown(Key.DirectionRight); keyUp(Key.DirectionRight) }
            runOnIdle { assertEquals(1.1f, applied) }
            assertEquals(presetBounds, control.fetchSemanticsNode().boundsInRoot,
                "Switching wrapped presets to the ruler must keep the control in place")
            control.performSemanticsAction(SemanticsActions.SetProgress) { it(1.37f) }
            runOnIdle { assertEquals(1.35f, applied) }
            control.performKeyInput { keyDown(Key.DirectionRight); keyUp(Key.DirectionRight) }
            runOnIdle { assertEquals(1.4f, applied) }
            control.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "1.4×"))
            File("build/reports/speed-dialog/$name-ruler.png").writeBytes(
                Image.makeFromBitmap(onNode(isDialog()).captureToImage().asSkiaBitmap()).encodeToData()!!.bytes,
            )
            control.performKeyInput { keyDown(Key.MoveEnd); keyUp(Key.MoveEnd) }
            control.performKeyInput { keyDown(Key.DirectionRight); keyUp(Key.DirectionRight) }
            runOnIdle { assertEquals(3f, applied) }
            control.performKeyInput { keyDown(Key.MoveHome); keyUp(Key.MoveHome) }
            control.performKeyInput { keyDown(Key.DirectionLeft); keyUp(Key.DirectionLeft) }
            runOnIdle { assertEquals(0.25f, applied) }
            onNodeWithContentDescription("Reset").performClick()
            runOnIdle { assertEquals(1f, applied) }
            onNodeWithText("Swipe to fine-tune").assertDoesNotExist()
        }
    }

    private data class LayoutCase(val name: String, val width: Int, val height: Int, val fontScale: Float = 1f)

    private fun SemanticsNodeInteraction.pixels(): IntArray {
        val bitmap = captureToImage()
        return IntArray(bitmap.width * bitmap.height).also { bitmap.readPixels(it) }
    }
}
