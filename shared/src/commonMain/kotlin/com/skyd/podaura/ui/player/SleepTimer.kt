package com.skyd.podaura.ui.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock

sealed interface SleepTimerOption {
    data object Off : SleepTimerOption
    data object EndOfMedia : SleepTimerOption
    data class AfterMinutes(val minutes: Int) : SleepTimerOption {
        init {
            require(minutes in PRESETS)
        }
    }

    companion object {
        val PRESETS = listOf(5, 10, 15, 30, 45, 60)
    }
}

data class SleepTimerState(
    val option: SleepTimerOption = SleepTimerOption.Off,
    val deadlineMillis: Long? = null,
    internal val pauseGeneration: Long = 0L,
) {
    internal fun remainingMillis(
        nowMillis: Long = Clock.System.now().toEpochMilliseconds()
    ): Long? = deadlineMillis?.let { (it - nowMillis).coerceAtLeast(0) }

    internal fun remainingSeconds(
        nowMillis: Long = Clock.System.now().toEpochMilliseconds()
    ): Long = remainingMillis(nowMillis)?.let { (it + 999) / 1000 } ?: 0
}

/** Owned by the playback engine, independently of UI visibility and media progress. */
internal class SleepTimer(
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val mutableState = MutableStateFlow(SleepTimerState())
    val state = mutableState.asStateFlow()
    val remainingMillis: Long? get() = state.value.remainingMillis(nowMillis())

    fun set(option: SleepTimerOption) {
        mutableState.value = SleepTimerState(
            option = option,
            deadlineMillis = (option as? SleepTimerOption.AfterMinutes)
                ?.let { nowMillis() + it.minutes * 60_000L },
            pauseGeneration = state.value.pauseGeneration,
        )
    }

    fun expire() {
        // Publish expiry and its generation together so queued playback uses one snapshot.
        mutableState.value = SleepTimerState(pauseGeneration = state.value.pauseGeneration + 1)
    }

    fun mediaChanged() {
        if (state.value.option == SleepTimerOption.EndOfMedia) set(SleepTimerOption.Off)
    }
}
