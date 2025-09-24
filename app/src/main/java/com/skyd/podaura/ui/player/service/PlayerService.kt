package com.skyd.podaura.ui.player.service

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.skyd.podaura.BuildConfig
import com.skyd.podaura.appContext
import com.skyd.podaura.ui.player.PlayerCommand
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel


class PlayerService : Service() {
    private val lifecycleScope = CoroutineScope(Dispatchers.Main)
    val playerCoordinator = PlayerCoordinator()
    private val playerNotificationReceiver = PlayerNotificationReceiver()
    private val binder = PlayerServiceBinder()
    private val sessionManager = MediaSessionManager(
        context = appContext,
        playerModel = playerCoordinator.model,
        callback = createMediaSessionCallback(),
    )

    override fun onCreate() {
        super.onCreate()
        ContextCompat.registerReceiver(
            this,
            playerNotificationReceiver,
            IntentFilter().apply {
                addAction(PLAY_ACTION)
                addAction(PREVIOUS_ACTION)
                addAction(NEXT_ACTION)
                addAction(LOOP_ACTION)
                addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                addAction(CLOSE_ACTION)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        playerCoordinator.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                stopSelf()
            }
        })
    }

    override fun onDestroy() {
        sessionManager.onDestroy()
        lifecycleScope.cancel()
        unregisterReceiver(playerNotificationReceiver)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        sessionManager.startForegroundService(this)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    inner class PlayerServiceBinder : Binder() {
        fun getService(): PlayerService = this@PlayerService
    }

    private fun createMediaSessionCallback() = object : MediaSessionCompat.Callback() {
        fun sendBroadcastWithPackage(action: String) {
            sendBroadcast(Intent(action).apply { `package` = packageName })
        }

        override fun onPause() {
            playerCoordinator.player.paused = true
        }

        override fun onPlay() {
            playerCoordinator.player.paused = false
        }

        override fun onSeekTo(pos: Long) {
            playerCoordinator.player.timePos = (pos / 1000).toInt()
        }

        override fun onSkipToNext() = sendBroadcastWithPackage(NEXT_ACTION)
        override fun onSkipToPrevious() = sendBroadcastWithPackage(PREVIOUS_ACTION)
        override fun onSetRepeatMode(repeatMode: Int) {
            when (repeatMode) {
                PlaybackStateCompat.REPEAT_MODE_ALL -> playerCoordinator.player.loopPlaylist()
                PlaybackStateCompat.REPEAT_MODE_ONE -> playerCoordinator.player.loopFile()
                PlaybackStateCompat.REPEAT_MODE_INVALID,
                PlaybackStateCompat.REPEAT_MODE_GROUP,
                PlaybackStateCompat.REPEAT_MODE_NONE -> playerCoordinator.player.loopNo()
            }
        }

        override fun onSetShuffleMode(shuffleMode: Int) {
            playerCoordinator.player.shuffle(shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL)
        }

        override fun onCustomAction(action: String?, extras: Bundle?) {
            when (action) {
                LOOP_ACTION -> sendBroadcastWithPackage(LOOP_ACTION)
                CLOSE_ACTION -> sendBroadcastWithPackage(CLOSE_ACTION)
            }
        }
    }

    inner class PlayerNotificationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return

            when (intent.action) {
                AudioManager.ACTION_AUDIO_BECOMING_NOISY ->
                    playerCoordinator.onCommand(PlayerCommand.Paused(true))

                PLAY_ACTION -> playerCoordinator.onCommand(PlayerCommand.PlayOrPause)
                PREVIOUS_ACTION -> playerCoordinator.onCommand(PlayerCommand.PreviousMedia)
                NEXT_ACTION -> playerCoordinator.onCommand(PlayerCommand.NextMedia)
                LOOP_ACTION -> playerCoordinator.onCommand(PlayerCommand.CycleLoop)
                CLOSE_ACTION -> {
                    playerCoordinator.onCommand(PlayerCommand.Destroy)
                    context?.sendBroadcast(Intent(FINISH_PLAY_ACTIVITY_ACTION).apply {
                        `package` = context.packageName
                    })
                }
            }
        }
    }

    companion object {
        const val PLAY_ACTION = BuildConfig.APPLICATION_ID + ".PlayerPlay"
        const val CLOSE_ACTION = BuildConfig.APPLICATION_ID + ".PlayerClose"
        const val PREVIOUS_ACTION = BuildConfig.APPLICATION_ID + ".PlayerPrevious"
        const val NEXT_ACTION = BuildConfig.APPLICATION_ID + ".PlayerNext"
        const val LOOP_ACTION = BuildConfig.APPLICATION_ID + ".PlayerLoop"
        const val FINISH_PLAY_ACTIVITY_ACTION = BuildConfig.APPLICATION_ID + ".FinishPlayActivity"

        fun createIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(action).apply {
                setPackage(BuildConfig.APPLICATION_ID)
            }
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }
    }
}