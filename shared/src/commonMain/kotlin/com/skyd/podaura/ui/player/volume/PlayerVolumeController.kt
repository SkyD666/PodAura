package com.skyd.podaura.ui.player.volume

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import co.touchlab.kermit.Logger
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.ui.player.LocalPlayerSession
import com.skyd.podaura.ui.player.PlayerCommand
import com.skyd.podaura.ui.player.mpv.MPV
import com.skyd.podaura.ui.player.mpv.MPVPlayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

internal data class VolumeState(
    val volume: Float = 100f,
    val muted: Boolean = false,
    val lastAudible: Float = 100f,
    val available: Boolean = true,
) {
    fun withVolume(value: Float): VolumeState = if (!value.isFinite() || value < 0f) this
    else copy(volume = value, lastAudible = value.takeIf { it > 0f } ?: lastAudible)

    fun toggledMute(): VolumeState = when {
        volume <= 0f -> copy(volume = lastAudible.coerceAtMost(100f), muted = false)
        else -> copy(muted = !muted)
    }
}

/** All engine access is confined to PlayerCoordinator's command/lifecycle dispatcher. */
internal class PlayerVolumeController(
    private val preferences: () -> DataStore<Preferences> = { dataStore },
) {
    private val mutableState = MutableStateFlow(VolumeState())
    val state = mutableState.asStateFlow()
    private var backend: PlayerVolumeBackend? = null
    val needsPolling: Boolean get() = backend?.needsPolling == true
    private var persistence: Job? = null
    private val volumeKey = floatPreferencesKey("playerVolume")
    private val muteKey = booleanPreferencesKey("playerMuted")
    private val lastAudibleKey = floatPreferencesKey("playerLastAudibleVolume")

    fun attach(mpv: MPV) = attach(createPlayerVolumeBackend(mpv))

    internal fun attach(newBackend: PlayerVolumeBackend) {
        check(backend == null) { "Volume controller is already attached" }
        try {
            val saved = runBlocking { preferences().data.first() }
            mutableState.value = VolumeState(
                muted = saved[muteKey] ?: saved[booleanPreferencesKey("audioOutputMuted")] ?: false,
                lastAudible = (saved[lastAudibleKey]
                    ?: saved[floatPreferencesKey("audioOutputLastAudibleVolume")])
                    ?.takeIf { it.isFinite() && it > 0f } ?: 100f,
            ).withVolume(
                saved[volumeKey] ?: saved[floatPreferencesKey("audioOutputVolume")] ?: 100f
            )
            newBackend.initialize(state.value)
            backend = newBackend
            refresh()
            persistence = CoroutineScope(Dispatchers.IO).launch {
                state.collect { value -> save(value) }
            }
        } catch (error: Throwable) {
            backend = null
            newBackend.close()
            throw error
        }
    }

    /** Read actual properties, rather than replaying stale queued property-event payloads. */
    fun refresh() {
        val current = backend ?: return
        val actual = current.read()
        mutableState.value = if (actual == null) state.value.copy(available = false)
        else state.value.withVolume(actual.volume).copy(muted = actual.muted, available = true)
    }

    fun detach() {
        val current = backend ?: return
        try {
            // Capture changes even if their property notification has not reached the actor.
            refresh()
        } finally {
            backend = null
            try {
                current.close()
            } finally {
                runBlocking {
                    persistence?.cancelAndJoin()
                    persistence = null
                    save(state.value)
                }
            }
        }
    }

    private suspend fun save(value: VolumeState) {
        try {
            preferences().edit {
                it[volumeKey] = value.volume
                it[muteKey] = value.muted
                it[lastAudibleKey] = value.lastAudible
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Logger.w(throwable = error, tag = "PlayerVolumeController") { "Could not save volume" }
        }
    }

    fun setVolume(value: Float) {
        if (!value.isFinite() || backend == null) return
        refresh()
        val volume = value.coerceIn(0f, 100f)
        apply(state.value.withVolume(volume).copy(muted = volume <= 0f && state.value.muted))
    }

    fun toggleMute() {
        if (backend == null) return
        refresh()
        apply(state.value.toggledMute())
    }

    private fun apply(value: VolumeState) {
        val current = backend ?: return
        current.apply(value)
        mutableState.value = value
        refresh()
    }
}

@Composable
internal fun rememberPlayerVolumeController(): VolumeController {
    val coordinator = LocalPlayerSession.current?.coordinator
    val controller = MPVPlayer.instance.volumeController
    val state by controller.state.collectAsState()
    // Capture the State holder so long-lived gesture callbacks see new values as well.
    return remember(controller, coordinator) {
        object : VolumeController {
            override val range = 0f..100f
            override var value: Float
                get() = state.volume
                set(value) {
                    coordinator?.onCommand(PlayerCommand.SetVolume(value))
                }
            override val available: Boolean get() = state.available
            override val muted: Boolean get() = state.muted
            override fun toggleMute() {
                coordinator?.onCommand(PlayerCommand.ToggleMute)
            }
        }
    }
}
