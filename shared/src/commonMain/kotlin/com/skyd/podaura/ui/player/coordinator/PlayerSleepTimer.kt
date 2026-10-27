package com.skyd.podaura.ui.player.coordinator

import com.skyd.podaura.ui.player.SleepTimer
import com.skyd.podaura.ui.player.SleepTimerOption
import com.skyd.podaura.ui.player.mpv.MPVPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Owns sleep-timer scheduling, temporary audio gain and the resulting pause latch.
 * Mutations run on the engine dispatcher, independently of the playback actor's I/O.
 * The host only receives completed pauses; history and platform policy stay with the host.
 */
internal class PlayerSleepTimer(
    private val player: MPVPlayer,
    private val scope: CoroutineScope,
    private val endOfMediaPause: EndOfMediaPause,
    private val onPaused: () -> Unit,
) {
    private val timer = SleepTimer()
    val state = timer.state
    private var job: Job? = null
    private var fade: SleepTimerFade? = null
    private var endTimerPath: String? = null
    var paused = false
        private set

    /** Thread-safe snapshot, taken before reading playback intent or queuing a command. */
    fun pendingPauseGeneration(): Long? = state.value.let { snapshot ->
        snapshot.pauseGeneration.takeIf { snapshot.option is SleepTimerOption.AfterMinutes }
    }

    fun resume(submittedPauseGeneration: Long?): Boolean {
        if (!paused || (submittedPauseGeneration != null &&
                    submittedPauseGeneration != state.value.pauseGeneration)) return false
        paused = false
        return true
    }

    fun set(option: SleepTimerOption) {
        restoreGain()
        timer.set(option)
        schedule()
        endTimerPath = player.path.takeIf { option == SleepTimerOption.EndOfMedia }
        // Keep the EOF guard after expiry until explicit playback or a media change.
        if (!paused || option == SleepTimerOption.EndOfMedia) {
            endOfMediaPause.setEnabled(option == SleepTimerOption.EndOfMedia)
        }
        if (option == SleepTimerOption.EndOfMedia && player.eofReached) expire()
    }

    fun refresh() {
        checkDeadline()
        schedule()
    }

    fun checkDeadline() {
        val currentFade = fade
        if (currentFade != null) {
            player.mpv.setPropertyDouble("volume-gain", currentFade.gain)
            // Only explicit pauses finish early; native EOF/seek pauses keep the deadline.
            if (currentFade.remainingMillis == 0L) expire()
        } else if (timer.remainingMillis == 0L) {
            if (player.paused) expire()
            else {
                fade = SleepTimerFade(player.mpv.getPropertyDouble("volume-gain"))
                schedule()
            }
        }
    }

    fun finishFadeOnPause(): Boolean {
        if (fade == null) return false
        expire()
        return true
    }

    fun mediaChanging() {
        timer.mediaChanged()
        endTimerPath = null
        // Keep the old EOF suppressed until FILE_LOADED/PLAYBACK_RESTART selects the target.
    }

    fun mediaStarted(path: String?) {
        if (state.value.option == SleepTimerOption.EndOfMedia && endTimerPath != path) {
            mediaChanging()
        }
    }

    fun mediaEnded(path: String?) {
        if (state.value.option == SleepTimerOption.EndOfMedia && endTimerPath == path) expire()
    }

    fun restoreEndOfMediaOptions() {
        if (!paused && state.value.option != SleepTimerOption.EndOfMedia) {
            endOfMediaPause.setEnabled(false)
        }
    }

    /** May also be called by the UI while requesting engine shutdown. */
    fun cancelJob() {
        job?.cancel()
    }

    /** Called on the engine dispatcher before the native player is destroyed. */
    fun close() {
        cancelJob()
        if (fade != null) {
            player.paused = true
            restoreGain()
        }
        timer.set(SleepTimerOption.Off)
    }

    private fun schedule() {
        cancelJob()
        job = scope.launch {
            while (isActive) {
                val remaining = fade?.remainingMillis?.coerceAtMost(20L)
                    ?: timer.remainingMillis ?: break
                delay(remaining)
                checkDeadline()
            }
        }
    }

    private fun restoreGain() {
        fade?.let { player.mpv.setPropertyDouble("volume-gain", it.originalGain) }
        fade = null
    }

    private fun expire() {
        cancelJob()
        paused = true
        player.paused = true
        // Restore after pausing to avoid a final burst, or a quiet subsequent play.
        restoreGain()
        timer.expire()
        endTimerPath = null
        onPaused()
    }
}
