package com.skyd.podaura.ui.window

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import com.skyd.podaura.ext.hasModifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal class PlayerRightArrowKeyHandler(
    private val coroutineScope: CoroutineScope,
    private val longPressTimeoutMillis: Long,
    private val rapidTapTimeoutMillis: Long,
    private val mediaStarted: () -> Boolean,
    private val currentMediaPath: () -> String?,
    private val currentPosition: () -> Long,
    private val forwardSeconds: () -> Int,
    private val onSeekTo: (Long) -> Unit,
    private val onLongPressStart: () -> Unit,
    private val onLongPressEnd: () -> Unit,
) {
    private var state = PressState.Idle
    private var longPressJob: Job? = null
    private var rapidTapResetJob: Job? = null
    private var pressedMediaPath: String? = null
    private var lastRequestedSeekPosition: Long? = null

    fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.key != Key.DirectionRight) return false

        return when (event.type) {
            KeyEventType.KeyDown -> onKeyDown(event.hasModifier)
            KeyEventType.KeyUp -> onKeyUp()
            else -> false
        }
    }

    fun cancel(suppressUntilKeyUp: Boolean = false) {
        val previousState = state
        longPressJob?.cancel()
        longPressJob = null
        clearRapidTapState()
        pressedMediaPath = null
        state = if (suppressUntilKeyUp && previousState != PressState.Idle) {
            PressState.Suppressed
        } else {
            PressState.Idle
        }
        if (previousState == PressState.LongPressing) onLongPressEnd()
    }

    private fun onKeyDown(hasModifier: Boolean): Boolean = when (state) {
        PressState.Idle -> {
            if (hasModifier || !mediaStarted()) {
                state = PressState.Suppressed
                false
            } else {
                state = PressState.Pending
                pressedMediaPath = currentMediaPath()
                longPressJob = coroutineScope.launch {
                    delay(longPressTimeoutMillis.milliseconds)
                    longPressJob = null
                    if (state != PressState.Pending) return@launch
                    if (!isSamePlayableMedia()) {
                        state = PressState.Suppressed
                        pressedMediaPath = null
                        return@launch
                    }
                    clearRapidTapState()
                    state = PressState.LongPressing
                    onLongPressStart()
                }
                true
            }
        }

        PressState.Pending,
        PressState.LongPressing -> true

        PressState.Suppressed -> false
    }

    private fun onKeyUp(): Boolean {
        val previousState = state
        longPressJob?.cancel()
        longPressJob = null
        state = PressState.Idle

        return when (previousState) {
            PressState.Pending -> {
                val shouldSeek = isSamePlayableMedia()
                pressedMediaPath = null
                if (shouldSeek) seekForward()
                true
            }

            PressState.LongPressing -> {
                pressedMediaPath = null
                onLongPressEnd()
                true
            }

            PressState.Idle,
            PressState.Suppressed -> {
                pressedMediaPath = null
                false
            }
        }
    }

    private fun isSamePlayableMedia(): Boolean =
        mediaStarted() && currentMediaPath() == pressedMediaPath

    private fun seekForward() {
        val currentPosition = currentPosition()
        val basePosition = maxOf(currentPosition, lastRequestedSeekPosition ?: currentPosition)
        val targetPosition = basePosition + forwardSeconds()
        lastRequestedSeekPosition = targetPosition
        onSeekTo(targetPosition)

        rapidTapResetJob?.cancel()
        rapidTapResetJob = coroutineScope.launch {
            delay(rapidTapTimeoutMillis.milliseconds)
            lastRequestedSeekPosition = null
            rapidTapResetJob = null
        }
    }

    private fun clearRapidTapState() {
        rapidTapResetJob?.cancel()
        rapidTapResetJob = null
        lastRequestedSeekPosition = null
    }

    private enum class PressState {
        Idle,
        Pending,
        LongPressing,
        Suppressed,
    }
}
