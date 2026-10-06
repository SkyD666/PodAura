package com.skyd.podaura.ext

import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class RightClickModifierTest {
    @Test
    fun initialPassHandlesNativeTabClicksAndCancelsWhenPointerLeaves() = runComposeUiTest {
        val enabled = mutableStateOf(true)
        val clickIncrement = mutableStateOf(1)
        val interactionSource = MutableInteractionSource()
        val rightClickSource = MutableInteractionSource()
        var primaryClicks = 0
        var rightClicks = 0
        var hovered = false
        var primaryPressed = false
        var rightPressed = false
        setContent {
            val increment = clickIncrement.value
            hovered = interactionSource.collectIsHoveredAsState().value
            primaryPressed = interactionSource.collectIsPressedAsState().value
            rightPressed = rightClickSource.collectIsPressedAsState().value
            MaterialTheme {
                PrimaryScrollableTabRow(
                    modifier = Modifier.size(180.dp, 48.dp),
                    selectedTabIndex = 0,
                    edgePadding = 0.dp,
                ) {
                    Tab(
                        modifier = Modifier.testTag("tab").onRightClickIfSupported(
                            interactionSource = rightClickSource,
                            enabled = enabled.value,
                            pass = PointerEventPass.Initial,
                            onClick = { rightClicks += increment },
                        ).indication(
                            interactionSource = rightClickSource,
                            indication = LocalIndication.current,
                        ),
                        selected = true,
                        onClick = { primaryClicks++ },
                        text = { Text("Media group") },
                        interactionSource = interactionSource,
                    )
                }
            }
        }
        val tab = onNodeWithTag("tab")
        val idleColor = tab.captureToImage().toPixelMap()[5, 5]
        tab.performMouseInput { moveTo(center) }
        runOnIdle { assertTrue(hovered, "The tab should be hovered before a right-click") }
        val hoverColor = tab.captureToImage().toPixelMap()[5, 5]
        assertNotEquals(idleColor, hoverColor, "Hover should have a visible indication")
        tab.performMouseInput {
            press(MouseButton.Secondary)
        }
        runOnIdle {
            assertTrue(hovered, "A right-button press should preserve hover")
            assertTrue(rightPressed, "A right-button press should provide pressed feedback")
            assertEquals(false, primaryPressed, "A right-click must not enter the primary gesture")
        }
        assertNotEquals(idleColor, tab.captureToImage().toPixelMap()[5, 5])
        assertEquals(0, rightClicks)
        tab.performMouseInput { release(MouseButton.Secondary) }
        runOnIdle { assertTrue(hovered, "A right-button release should preserve hover") }
        runOnIdle { assertEquals(false, rightPressed) }
        assertEquals(1, rightClicks)
        assertEquals(0, primaryClicks)

        tab.performMouseInput { click() }
        assertEquals(1, primaryClicks)
        assertEquals(1, rightClicks)

        tab.performMouseInput {
            moveTo(center)
            press(MouseButton.Secondary)
            moveTo(Offset(-100f, -100f))
            release(MouseButton.Secondary)
        }
        assertEquals(1, rightClicks)
        tab.performMouseInput { click(button = MouseButton.Secondary) }
        assertEquals(2, rightClicks)

        tab.performMouseInput {
            press(MouseButton.Secondary)
            press(MouseButton.Primary)
            release(MouseButton.Secondary)
        }
        assertEquals(2, rightClicks, "Another button press should cancel the right-click")
        tab.performMouseInput { release(MouseButton.Primary) }
        assertEquals(2, rightClicks, "A primary release must not complete a right-click")
        runOnIdle { assertEquals(false, rightPressed) }

        tab.performMouseInput {
            press(MouseButton.Primary)
            press(MouseButton.Secondary)
            release(MouseButton.Secondary)
        }
        assertEquals(3, rightClicks, "A right-click should complete on its own button release")
        tab.performMouseInput { release(MouseButton.Primary) }
        assertEquals(3, rightClicks)

        tab.performMouseInput {
            moveTo(center)
            press(MouseButton.Secondary)
        }
        runOnIdle { clickIncrement.value = 3 }
        tab.performMouseInput { release(MouseButton.Secondary) }
        assertEquals(6, rightClicks)

        runOnIdle { enabled.value = false }
        tab.performMouseInput { click(button = MouseButton.Secondary) }
        assertEquals(6, rightClicks)
    }

    @Test
    fun defaultPassPreservesExistingClickHandlers() = runComposeUiTest {
        var primaryClicks = 0
        var rightClicks = 0
        setContent {
            Box(
                Modifier.size(100.dp)
                    .testTag("target")
                    .clickable { primaryClicks++ }
                    .onRightClickIfSupported { rightClicks++ }
            )
        }
        val target = onNodeWithTag("target")
        target.performMouseInput { click(button = MouseButton.Secondary) }
        assertEquals(1, rightClicks)
        assertEquals(0, primaryClicks)
        target.performMouseInput { click() }
        assertEquals(1, rightClicks)
        assertEquals(1, primaryClicks)
    }
}
