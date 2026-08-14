package com.skyd.podaura.ui.window

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlayerWindowKeyEventTest {

    @Test
    fun leftKeySelectsBackwardSeek() {
        assertEquals(
            PlayerKeyboardAction.SeekBackward,
            keyboardAction(key = Key.DirectionLeft),
        )
    }

    @Test
    fun rightKeySelectsForwardSeek() {
        assertEquals(
            PlayerKeyboardAction.SeekForward,
            keyboardAction(key = Key.DirectionRight),
        )
    }

    @Test
    fun repeatedKeyDownEventsRemainHandled() {
        assertEquals(PlayerKeyboardAction.SeekForward, keyboardAction(key = Key.DirectionRight))
        assertEquals(PlayerKeyboardAction.SeekForward, keyboardAction(key = Key.DirectionRight))
    }

    @Test
    fun spaceKeyDownIsConsumedWithoutTogglingPlayback() {
        assertEquals(
            PlayerKeyboardAction.ConsumeSpaceKeyDown,
            keyboardAction(key = Key.Spacebar),
        )
        assertEquals(
            PlayerKeyboardAction.ConsumeSpaceKeyDown,
            keyboardAction(key = Key.Spacebar),
        )
    }

    @Test
    fun spaceKeyUpTogglesPlayPause() {
        assertEquals(
            PlayerKeyboardAction.TogglePlayPause,
            keyboardAction(key = Key.Spacebar, type = KeyEventType.KeyUp),
        )
    }

    @Test
    fun keyUpIsIgnored() {
        assertNull(
            keyboardAction(
                key = Key.DirectionRight,
                type = KeyEventType.KeyUp,
            )
        )
    }

    @Test
    fun nonDirectionKeyIsIgnored() {
        assertNull(keyboardAction(key = Key.Enter))
    }

    @Test
    fun modifiedDirectionKeysAreIgnored() {
        assertNull(keyboardAction(key = Key.DirectionLeft, isCtrlPressed = true))
        assertNull(keyboardAction(key = Key.DirectionLeft, isAltPressed = true))
        assertNull(keyboardAction(key = Key.DirectionRight, isMetaPressed = true))
        assertNull(keyboardAction(key = Key.DirectionRight, isShiftPressed = true))
    }

    @Test
    fun seekActionsAreUnavailableWhenMediaIsNotStarted() {
        assertEquals(
            false,
            PlayerKeyboardAction.SeekBackward.isAvailable(mediaStarted = false),
        )
        assertEquals(
            false,
            PlayerKeyboardAction.SeekForward.isAvailable(mediaStarted = false),
        )
    }

    @Test
    fun spaceActionsRemainAvailableWhenMediaIsNotStarted() {
        assertEquals(
            true,
            PlayerKeyboardAction.ConsumeSpaceKeyDown.isAvailable(mediaStarted = false),
        )
        assertEquals(
            true,
            PlayerKeyboardAction.TogglePlayPause.isAvailable(mediaStarted = false),
        )
    }

    private fun keyboardAction(
        key: Key,
        type: KeyEventType = KeyEventType.KeyDown,
        isCtrlPressed: Boolean = false,
        isMetaPressed: Boolean = false,
        isAltPressed: Boolean = false,
        isShiftPressed: Boolean = false,
    ): PlayerKeyboardAction? = playerKeyboardActionForKeyEvent(
        event = KeyEvent(
            key = key,
            type = type,
            isCtrlPressed = isCtrlPressed,
            isMetaPressed = isMetaPressed,
            isAltPressed = isAltPressed,
            isShiftPressed = isShiftPressed,
        ),
    )
}
