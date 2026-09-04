package com.skyd.podaura.ui.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PressAndHoldSpeedControllerTest {

    @Test
    fun firstSourceAppliesTemporarySpeedAndLastSourceRestoresRegularSpeed() {
        var currentSpeed = 1.5f
        val requestedSpeeds = mutableListOf<Float>()
        val controller = controller(
            currentSpeed = { currentSpeed },
            onSpeedChanged = {
                currentSpeed = it
                requestedSpeeds += it
            },
        )

        controller.start(PressAndHoldSpeedSource.Pointer)
        controller.start(PressAndHoldSpeedSource.Keyboard)
        controller.stop(PressAndHoldSpeedSource.Pointer)

        assertTrue(controller.isActive)
        assertEquals(listOf(3f), requestedSpeeds)

        controller.stop(PressAndHoldSpeedSource.Keyboard)

        assertFalse(controller.isActive)
        assertEquals(listOf(3f, 1.5f), requestedSpeeds)
    }

    @Test
    fun regularSpeedSelectedDuringTemporaryPlaybackIsRestoredOnRelease() {
        var currentSpeed = 1.25f
        val requestedSpeeds = mutableListOf<Float>()
        val controller = controller(
            currentSpeed = { currentSpeed },
            onSpeedChanged = {
                currentSpeed = it
                requestedSpeeds += it
            },
        )

        controller.start(PressAndHoldSpeedSource.Keyboard)
        controller.setRegularSpeed(1.5f)

        assertEquals(listOf(3f), requestedSpeeds)

        controller.stop(PressAndHoldSpeedSource.Keyboard)

        assertEquals(listOf(3f, 1.5f), requestedSpeeds)
    }

    @Test
    fun latestRegularSpeedIsNotLostBeforePlayerStateAcknowledgesIt() {
        val requestedSpeeds = mutableListOf<Float>()
        val controller = controller(
            currentSpeed = { 1f },
            onSpeedChanged = { requestedSpeeds += it },
        )

        controller.setRegularSpeed(1.5f)
        controller.start(PressAndHoldSpeedSource.Keyboard)
        controller.stop(PressAndHoldSpeedSource.Keyboard)

        assertEquals(listOf(1.5f, 3f, 1.5f), requestedSpeeds)
    }

    @Test
    fun cancelAllRestoresLatestRegularSpeedOnlyOnce() {
        var currentSpeed = 1f
        val requestedSpeeds = mutableListOf<Float>()
        val controller = controller(
            currentSpeed = { currentSpeed },
            onSpeedChanged = {
                currentSpeed = it
                requestedSpeeds += it
            },
        )

        controller.start(PressAndHoldSpeedSource.Keyboard)
        controller.start(PressAndHoldSpeedSource.Pointer)
        controller.setRegularSpeed(2f)
        controller.cancelAll()
        controller.cancelAll()

        assertFalse(controller.isActive)
        assertEquals(listOf(3f, 2f), requestedSpeeds)
    }

    @Test
    fun speedAlreadyAtThreeIsPreservedAfterTemporaryPlayback() {
        var currentSpeed = 3f
        val requestedSpeeds = mutableListOf<Float>()
        val controller = controller(
            currentSpeed = { currentSpeed },
            onSpeedChanged = {
                currentSpeed = it
                requestedSpeeds += it
            },
        )

        controller.start(PressAndHoldSpeedSource.Keyboard)
        assertTrue(controller.isActive)
        controller.stop(PressAndHoldSpeedSource.Keyboard)

        assertEquals(listOf(3f, 3f), requestedSpeeds)
    }

    private fun controller(
        currentSpeed: () -> Float,
        onSpeedChanged: (Float) -> Unit,
    ) = PressAndHoldSpeedController(
        currentSpeed = currentSpeed,
        onSpeedChanged = onSpeedChanged,
    )
}
