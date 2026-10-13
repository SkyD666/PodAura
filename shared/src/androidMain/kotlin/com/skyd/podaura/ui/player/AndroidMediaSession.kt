package com.skyd.podaura.ui.player

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.skyd.podaura.ext.flowOf
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.preference.player.BackgroundPlayPreference
import com.skyd.podaura.model.preference.player.PauseOnScreenOffPreference
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import com.skyd.podaura.ui.player.coordinator.isReady
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Service-owned playback policy also observes the screen after the player Activity closes. */
class AndroidMediaSession(context: Context, private val coordinator: PlayerCoordinator) :
    AutoCloseable {
    private val context = context.applicationContext
    private val power = this.context.getSystemService(PowerManager::class.java)
    private val keyguard = this.context.getSystemService(KeyguardManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val policy = PlaybackResumePolicy(
        initiallyBackground = true,
        initiallyScreenLocked = !power.isInteractive || keyguard.isKeyguardLocked,
        independentScreenPlayback = true,
        pauseOnScreenOff = dataStore.getOrDefault(PauseOnScreenOffPreference),
    )
    private var systemCommand = false

    // Seed once when creating the session; the subscription owns all subsequent updates.
    private var backgroundEnabled = dataStore.getOrDefault(BackgroundPlayPreference)
    private val events = AndroidPlaybackEvents(
        policy = policy,
        screenOff = !power.isInteractive,
        playing = { !coordinator.playerState.value.paused },
        ready = { coordinator.engineState.value.isReady },
        backgroundEnabled = { backgroundEnabled },
        systemPause = ::systemPause,
    )
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> updateScreenState(off = true)
                Intent.ACTION_USER_PRESENT -> updateScreenState(locked = false)
                Intent.ACTION_SCREEN_ON -> updateScreenState(off = false)
            }
        }
    }

    init {
        coordinator.onPlaybackCommand = { command, intent ->
            if (!systemCommand) {
                val paused = coordinator.playerState.value.paused
                intent?.let {
                    policy.userAction(it, explicitPlay = command.isExplicitPlay(paused))
                }
            }
        }
        // USER_PRESENT can come from SystemUI under a non-system UID. All three
        // actions are protected system broadcasts, so this receiver must be exported.
        ContextCompat.registerReceiver(this.context, receiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }, ContextCompat.RECEIVER_EXPORTED)
        scope.launch {
            combine(
                coordinator.playerState, coordinator.engineState,
                dataStore.flowOf(BackgroundPlayPreference, PauseOnScreenOffPreference)
            ) { state, engine, (enabled, pauseOnScreenOff) ->
                backgroundEnabled = enabled
                policy.pauseOnScreenOff = pauseOnScreenOff
                policy.pauseWhenRequired(!state.paused, engine.isReady, enabled)
            }.collect { pause -> if (pause) systemPause(true) }
        }
    }

    fun setPlayerVisible(visible: Boolean) {
        events.setPlayerVisible(
            visible = visible,
            off = !power.isInteractive,
            locked = keyguard.isKeyguardLocked
        )
    }

    private fun updateScreenState(
        off: Boolean = !power.isInteractive,
        locked: Boolean = keyguard.isKeyguardLocked,
    ) {
        events.updateScreenState(off, locked)
    }

    fun setPictureInPictureActive(active: Boolean) {
        if (policy.setPictureInPictureActive(active)) systemPause(false)
        events.enforcePolicy()
    }

    private fun systemPause(paused: Boolean) {
        systemCommand = true
        try {
            coordinator.onCommand(PlayerCommand.Paused(paused))
        } finally {
            systemCommand = false
        }
    }

    override fun close() {
        context.unregisterReceiver(receiver)
        coordinator.onPlaybackCommand = null
        scope.cancel()
    }
}
