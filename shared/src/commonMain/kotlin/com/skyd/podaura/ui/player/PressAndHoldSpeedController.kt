package com.skyd.podaura.ui.player

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

internal enum class PressAndHoldSpeedSource {
    Pointer,
    Keyboard,
}

internal val LocalPressAndHoldSpeedController =
    staticCompositionLocalOf<PressAndHoldSpeedController?> { null }

@Stable
internal class PressAndHoldSpeedController(
    private val currentSpeed: () -> Float,
    private val onSpeedChanged: (Float) -> Unit,
) {
    private val activeSources = mutableSetOf<PressAndHoldSpeedSource>()
    private var regularSpeed: Float? = null

    var isActive by mutableStateOf(false)
        private set

    fun start(source: PressAndHoldSpeedSource) {
        if (!activeSources.add(source)) return

        if (activeSources.size == 1) {
            if (regularSpeed == null) regularSpeed = currentSpeed()
            isActive = true
            onSpeedChanged(HOLD_SPEED)
        }
    }

    fun stop(source: PressAndHoldSpeedSource) {
        if (!activeSources.remove(source) || activeSources.isNotEmpty()) return

        restoreRegularSpeed()
    }

    fun setRegularSpeed(speed: Float) {
        regularSpeed = speed
        if (activeSources.isEmpty()) {
            onSpeedChanged(speed)
        }
    }

    fun cancelAll() {
        if (activeSources.isEmpty()) return

        activeSources.clear()
        restoreRegularSpeed()
    }

    private fun restoreRegularSpeed() {
        val speed = regularSpeed ?: currentSpeed()
        isActive = false
        onSpeedChanged(speed)
    }

    companion object {
        const val HOLD_SPEED = 3f
    }
}
