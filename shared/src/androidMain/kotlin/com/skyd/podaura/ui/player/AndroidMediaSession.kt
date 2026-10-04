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
    )
    private var systemCommand = false
    private val backgroundEnabled get() = dataStore.getOrDefault(BackgroundPlayPreference)
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val locked = when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> true
                Intent.ACTION_USER_PRESENT -> false
                Intent.ACTION_SCREEN_ON -> keyguard.isKeyguardLocked
                else -> return
            }
            setScreenLocked(locked)
        }
    }

    init {
        coordinator.onPlaybackCommand = { command ->
            if (!systemCommand) command.playbackIntent(coordinator.playerState.value.paused)
                ?.let { policy.userAction(it) }
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
                dataStore.flowOf(BackgroundPlayPreference)
            ) { state, engine, enabled ->
                policy.pauseWhenRequired(!state.paused, engine.isReady, enabled)
            }.collect { pause -> if (pause) systemPause(true) }
        }
    }

    fun setPlayerVisible(visible: Boolean) {
        if (visible) {
            setScreenLocked(!power.isInteractive || keyguard.isKeyguardLocked)
            if (policy.enterForeground()) systemPause(false)
        } else if (policy.enterBackground(
                !coordinator.playerState.value.paused,
                backgroundEnabled
            )
        ) {
            systemPause(true)
        }
    }

    private fun setScreenLocked(locked: Boolean) {
        if (policy.setScreenLocked(
                locked,
                !coordinator.playerState.value.paused,
                backgroundEnabled
            )
        ) {
            systemPause(locked)
        }
    }

    fun setPictureInPictureActive(active: Boolean) {
        if (policy.setPictureInPictureActive(active)) systemPause(false)
        if (policy.pauseWhenRequired(
                !coordinator.playerState.value.paused,
                coordinator.engineState.value.isReady, backgroundEnabled
            )
        ) systemPause(true)
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
