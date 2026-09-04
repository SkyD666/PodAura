package com.skyd.podaura.ui.window

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayerWindowKeyEventTest {

    @Test
    fun leftKeySelectsBackwardSeek() {
        assertEquals(
            PlayerKeyboardAction.SeekBackward,
            keyboardAction(key = Key.DirectionLeft),
        )
    }

    @Test
    fun rightKeyIsReservedForStatefulHandling() {
        assertNull(keyboardAction(key = Key.DirectionRight))
        assertNull(keyboardAction(key = Key.DirectionRight, type = KeyEventType.KeyUp))
    }

    @Test
    fun shortRightPressSeeksOnlyWhenReleased() = runTest {
        val fixture = rightArrowFixture()

        assertTrue(fixture.handler.onKeyEvent(rightKeyEvent()))
        advanceTimeBy(LONG_PRESS_TIMEOUT_MILLIS - 1)
        runCurrent()
        assertEquals(emptyList(), fixture.events)

        assertTrue(
            fixture.handler.onKeyEvent(rightKeyEvent(type = KeyEventType.KeyUp))
        )

        assertEquals(listOf(Event.Seek), fixture.events)
        assertEquals(listOf(110L), fixture.seekTargets)
    }

    @Test
    fun rapidShortRightPressesAccumulateWhilePlayerPositionIsStale() = runTest {
        val fixture = rightArrowFixture()

        repeat(3) {
            fixture.handler.onKeyEvent(rightKeyEvent())
            fixture.handler.onKeyEvent(rightKeyEvent(type = KeyEventType.KeyUp))
            advanceTimeBy(50)
        }

        assertEquals(listOf(110L, 120L, 130L), fixture.seekTargets)
    }

    @Test
    fun rightSeekBurstUsesNewPlayerPositionAfterRapidTapTimeout() = runTest {
        val fixture = rightArrowFixture()

        fixture.handler.onKeyEvent(rightKeyEvent())
        fixture.handler.onKeyEvent(rightKeyEvent(type = KeyEventType.KeyUp))
        advanceTimeBy(RAPID_TAP_TIMEOUT_MILLIS)
        runCurrent()
        fixture.position = 40L
        fixture.handler.onKeyEvent(rightKeyEvent())
        fixture.handler.onKeyEvent(rightKeyEvent(type = KeyEventType.KeyUp))

        assertEquals(listOf(110L, 50L), fixture.seekTargets)
    }

    @Test
    fun rapidRightPressUsesPlayerPositionWhenItAdvancesPastPendingTarget() = runTest {
        val fixture = rightArrowFixture()

        fixture.handler.onKeyEvent(rightKeyEvent())
        fixture.handler.onKeyEvent(rightKeyEvent(type = KeyEventType.KeyUp))
        fixture.position = 115L
        fixture.handler.onKeyEvent(rightKeyEvent())
        fixture.handler.onKeyEvent(rightKeyEvent(type = KeyEventType.KeyUp))

        assertEquals(listOf(110L, 125L), fixture.seekTargets)
    }

    @Test
    fun longRightPressStartsTemporarySpeedAndNeverSeeks() = runTest {
        val fixture = rightArrowFixture()

        assertTrue(fixture.handler.onKeyEvent(rightKeyEvent()))
        assertTrue(fixture.handler.onKeyEvent(rightKeyEvent()))
        advanceTimeBy(LONG_PRESS_TIMEOUT_MILLIS)
        runCurrent()

        assertEquals(listOf(Event.LongPressStart), fixture.events)
        assertTrue(fixture.handler.onKeyEvent(rightKeyEvent()))
        assertTrue(
            fixture.handler.onKeyEvent(rightKeyEvent(type = KeyEventType.KeyUp))
        )
        assertEquals(
            listOf(Event.LongPressStart, Event.LongPressEnd),
            fixture.events,
        )
    }

    @Test
    fun pendingPressCancellationDoesNotSeekAndCanWaitForPhysicalKeyUp() = runTest {
        val fixture = rightArrowFixture()

        assertTrue(fixture.handler.onKeyEvent(rightKeyEvent()))
        fixture.handler.cancel(suppressUntilKeyUp = true)
        advanceTimeBy(LONG_PRESS_TIMEOUT_MILLIS)
        runCurrent()

        assertFalse(fixture.handler.onKeyEvent(rightKeyEvent()))
        assertFalse(
            fixture.handler.onKeyEvent(rightKeyEvent(type = KeyEventType.KeyUp))
        )
        assertEquals(emptyList(), fixture.events)

        assertTrue(fixture.handler.onKeyEvent(rightKeyEvent()))
        assertTrue(
            fixture.handler.onKeyEvent(rightKeyEvent(type = KeyEventType.KeyUp))
        )
        assertEquals(listOf(Event.Seek), fixture.events)
    }

    @Test
    fun activeLongPressCancellationEndsTemporarySpeedWithoutSeeking() = runTest {
        val fixture = rightArrowFixture()

        fixture.handler.onKeyEvent(rightKeyEvent())
        advanceTimeBy(LONG_PRESS_TIMEOUT_MILLIS)
        runCurrent()
        fixture.handler.cancel()

        assertEquals(
            listOf(Event.LongPressStart, Event.LongPressEnd),
            fixture.events,
        )
    }

    @Test
    fun initialModifierStateAppliesUntilRightKeyIsReleased() = runTest {
        val fixture = rightArrowFixture()

        assertFalse(
            fixture.handler.onKeyEvent(rightKeyEvent(isShiftPressed = true))
        )
        assertFalse(fixture.handler.onKeyEvent(rightKeyEvent()))
        assertFalse(
            fixture.handler.onKeyEvent(rightKeyEvent(type = KeyEventType.KeyUp))
        )
        assertEquals(emptyList(), fixture.events)

        assertTrue(fixture.handler.onKeyEvent(rightKeyEvent()))
        assertTrue(
            fixture.handler.onKeyEvent(
                rightKeyEvent(type = KeyEventType.KeyUp, isShiftPressed = true)
            )
        )
        assertEquals(listOf(Event.Seek), fixture.events)
    }

    @Test
    fun pressThatStartsBeforeMediaIsReadyStaysSuppressedUntilKeyUp() = runTest {
        val fixture = rightArrowFixture().apply { mediaStarted = false }

        assertFalse(fixture.handler.onKeyEvent(rightKeyEvent()))
        fixture.mediaStarted = true
        assertFalse(fixture.handler.onKeyEvent(rightKeyEvent()))
        assertFalse(
            fixture.handler.onKeyEvent(rightKeyEvent(type = KeyEventType.KeyUp))
        )
        assertEquals(emptyList(), fixture.events)

        assertTrue(fixture.handler.onKeyEvent(rightKeyEvent()))
        assertTrue(
            fixture.handler.onKeyEvent(rightKeyEvent(type = KeyEventType.KeyUp))
        )
        assertEquals(listOf(Event.Seek), fixture.events)
    }

    @Test
    fun mediaChangeBeforeReleaseConsumesPressWithoutSeeking() = runTest {
        val fixture = rightArrowFixture()

        assertTrue(fixture.handler.onKeyEvent(rightKeyEvent()))
        fixture.mediaPath = "second"

        assertTrue(
            fixture.handler.onKeyEvent(rightKeyEvent(type = KeyEventType.KeyUp))
        )
        assertEquals(emptyList(), fixture.events)
    }

    @Test
    fun spaceKeyDownIsConsumedWithoutTogglingPlayback() {
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
    fun nonDirectionKeyIsIgnored() {
        assertNull(keyboardAction(key = Key.Enter))
    }

    @Test
    fun modifiedStatelessKeysAreIgnored() {
        assertNull(keyboardAction(key = Key.DirectionLeft, isCtrlPressed = true))
        assertNull(keyboardAction(key = Key.DirectionLeft, isAltPressed = true))
        assertNull(keyboardAction(key = Key.Spacebar, isMetaPressed = true))
        assertNull(keyboardAction(key = Key.Spacebar, isShiftPressed = true))
    }

    @Test
    fun backwardSeekIsUnavailableWhenMediaIsNotStarted() {
        assertFalse(PlayerKeyboardAction.SeekBackward.isAvailable(mediaStarted = false))
    }

    @Test
    fun spaceActionsRemainAvailableWhenMediaIsNotStarted() {
        assertTrue(
            PlayerKeyboardAction.ConsumeSpaceKeyDown.isAvailable(mediaStarted = false)
        )
        assertTrue(
            PlayerKeyboardAction.TogglePlayPause.isAvailable(mediaStarted = false)
        )
    }

    private fun TestScope.rightArrowFixture() = RightArrowFixture(this)

    private class RightArrowFixture(scope: TestScope) {
        var mediaStarted = true
        var mediaPath = "first"
        var position = 100L
        var forwardSeconds = 10
        val events = mutableListOf<Event>()
        val seekTargets = mutableListOf<Long>()
        val handler = PlayerRightArrowKeyHandler(
            coroutineScope = scope,
            longPressTimeoutMillis = LONG_PRESS_TIMEOUT_MILLIS,
            rapidTapTimeoutMillis = RAPID_TAP_TIMEOUT_MILLIS,
            mediaStarted = { mediaStarted },
            currentMediaPath = { mediaPath },
            currentPosition = { position },
            forwardSeconds = { forwardSeconds },
            onSeekTo = {
                seekTargets += it
                events += Event.Seek
            },
            onLongPressStart = { events += Event.LongPressStart },
            onLongPressEnd = { events += Event.LongPressEnd },
        )
    }

    private enum class Event {
        Seek,
        LongPressStart,
        LongPressEnd,
    }

    private fun rightKeyEvent(
        type: KeyEventType = KeyEventType.KeyDown,
        isCtrlPressed: Boolean = false,
        isMetaPressed: Boolean = false,
        isAltPressed: Boolean = false,
        isShiftPressed: Boolean = false,
    ) = KeyEvent(
        key = Key.DirectionRight,
        type = type,
        isCtrlPressed = isCtrlPressed,
        isMetaPressed = isMetaPressed,
        isAltPressed = isAltPressed,
        isShiftPressed = isShiftPressed,
    )

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

    private companion object {
        const val LONG_PRESS_TIMEOUT_MILLIS = 500L
        const val RAPID_TAP_TIMEOUT_MILLIS = 300L
    }
}
