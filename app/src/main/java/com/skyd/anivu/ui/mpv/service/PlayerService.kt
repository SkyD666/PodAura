package com.skyd.anivu.ui.mpv.service

import android.app.Application
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.skyd.anivu.BuildConfig
import com.skyd.anivu.appContext
import com.skyd.anivu.model.bean.MediaPlayHistoryBean
import com.skyd.anivu.model.repository.PlayerRepository
import com.skyd.anivu.ui.mpv.MPVPlayer
import com.skyd.anivu.ui.mpv.PlayerCommand
import com.skyd.anivu.ui.mpv.PlayerEvent
import com.skyd.anivu.ui.mpv.resolveUri
import dagger.hilt.android.AndroidEntryPoint
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.pow

@AndroidEntryPoint
class PlayerService : Service() {
    @Inject
    lateinit var playerRepo: PlayerRepository

    private val lifecycleScope = CoroutineScope(Dispatchers.Main)

    private val playerNotificationReceiver = PlayerNotificationReceiver()
    private val binder = PlayerServiceBinder()
    var uri: Uri = Uri.EMPTY
        private set
    val player = MPVPlayer.getInstance(appContext as Application)
    private val sessionManager = MediaSessionManager(appContext, createMediaSessionCallback())
    private val notificationManager = PlayerNotificationManager(appContext, sessionManager)
    val playerState get() = sessionManager.playerState

    private val observers = mutableSetOf<Observer>()

    private val mpvObserver = object : MPVLib.EventObserver {
        override fun eventProperty(property: String) {
            when (property) {
                "aid" -> sendEvent(PlayerEvent.AudioTrackChanged(player.aid))
                "sid" -> sendEvent(PlayerEvent.SubtitleTrackChanged(player.sid))
                "video-zoom" -> sendEvent(PlayerEvent.Zoom(2.0.pow(player.videoZoom).toFloat()))
                "video-pan-x" -> sendEvent(
                    PlayerEvent.VideoOffsetX((player.videoPanX * (player.videoDW ?: 0)).toFloat())
                )

                "video-pan-y" -> sendEvent(
                    PlayerEvent.VideoOffsetY((player.videoPanY * (player.videoDH ?: 0)).toFloat())
                )

                "speed" -> sendEvent(PlayerEvent.Speed(player.playbackSpeed.toFloat()))
                "track-list" -> {
                    player.loadTracks()
                    sendEvent(PlayerEvent.AllSubtitleTracks(player.subtitleTrack))
                    sendEvent(PlayerEvent.AllAudioTracks(player.audioTrack))
                }

                "demuxer-cache-duration" -> sendEvent(PlayerEvent.Buffer(player.demuxerCacheDuration.toInt()))
                "loop-file", "loop-playlist" -> Unit//sendEvent(PlayerEvent.Buffer(player.getRepeat()))
                "metadata" -> {
                    sendEvent(PlayerEvent.Artist(player.artist))
                    sendEvent(PlayerEvent.Album(player.album))
                }
            }
        }

        override fun eventProperty(property: String, value: Long) {
            when (property) {
                "aid" -> sendEvent(PlayerEvent.AudioTrackChanged(value.toInt()))
                "sid" -> sendEvent(PlayerEvent.SubtitleTrackChanged(value.toInt()))
                "time-pos" -> sendEvent(PlayerEvent.Position(value))
                "duration" -> sendEvent(PlayerEvent.Duration(value))
                "video-rotate" -> sendEvent(PlayerEvent.Rotate(value.toFloat()))
                "playlist-pos" -> sendEvent(PlayerEvent.PlaylistPosition(value.toInt()))
                "playlist-count" -> sendEvent(PlayerEvent.PlaylistCount(value.toInt()))
            }
        }

        override fun eventProperty(property: String, value: Boolean) {
            when (property) {
                "pause" -> sendEvent(PlayerEvent.Paused(value))
                "paused-for-cache" -> sendEvent(PlayerEvent.PausedForCache(value))
                "shuffle" -> sendEvent(PlayerEvent.Shuffle(value))
                "idle-active" -> sendEvent(PlayerEvent.Idling(value))
            }
        }

        override fun eventProperty(property: String, value: String) {
            when (property) {
                "media-title" -> sendEvent(PlayerEvent.MediaTitle(value))
            }
        }

        override fun event(eventId: Int) {
            when (eventId) {
                MPVLib.mpvEventId.MPV_EVENT_SEEK -> sendEvent(PlayerEvent.Seek)
                MPVLib.mpvEventId.MPV_EVENT_END_FILE -> {
                    sendEvent(PlayerEvent.EndFile)
                    savePosition()
                }

                MPVLib.mpvEventId.MPV_EVENT_FILE_LOADED -> {
                    sendEvent(PlayerEvent.FileLoaded)
                    sendEvent(PlayerEvent.Paused(player.paused))
                    loadLastPosition().invokeOnCompletion {
                        sendEvent(PlayerEvent.MediaThumbnail(player.thumbnail))
                    }
                }

                MPVLib.mpvEventId.MPV_EVENT_PLAYBACK_RESTART -> {
                    sendEvent(PlayerEvent.PlaybackRestart)
                    sendEvent(PlayerEvent.Paused(player.paused))
                }

                MPVLib.mpvEventId.MPV_EVENT_SHUTDOWN -> {
                    sendEvent(PlayerEvent.Shutdown)
                    stopSelf()
                }
            }
        }

        override fun efEvent(err: String?) {
        }
    }

    override fun onCreate() {
        super.onCreate()

        ContextCompat.registerReceiver(
            this,
            playerNotificationReceiver,
            IntentFilter().apply {
                addAction(PLAY_ACTION)
                addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                addAction(CLOSE_ACTION)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        addObserver(sessionManager)
        MPVLib.addObserver(mpvObserver)
        notificationManager.createNotificationChannel()

        lifecycleScope.launch {
            playerState.collectLatest {
                notificationManager.update()
            }
        }
    }

    override fun onDestroy() {
        savePosition()

        sendEvent(PlayerEvent.ServiceDestroy)
        player.destroy()
        MPVLib.removeObserver(mpvObserver)
        sessionManager.mediaSession.isActive = false
        sessionManager.mediaSession.release()
        notificationManager.cancel()
        removeAllObserver()
        lifecycleScope.cancel()

        unregisterReceiver(playerNotificationReceiver)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(notificationManager.notificationBuilder.build())
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    inner class PlayerServiceBinder : Binder() {
        fun getService(): PlayerService = this@PlayerService
    }

    fun interface Observer {
        fun onCommand(command: PlayerEvent)
    }

    fun addObserver(observer: Observer) = observers.add(observer)
    fun removeObserver(observer: Observer) = observers.remove(observer)
    fun removeAllObserver() = observers.clear()
    private fun sendEvent(command: PlayerEvent) {
        observers.forEach { it.onCommand(command) }
    }

    fun onCommand(command: PlayerCommand) = player.apply {
        when (command) {
            is PlayerCommand.Attach -> command.surfaceHolder.addCallback(this)
            is PlayerCommand.Detach -> command.surface.release()
            is PlayerCommand.SetUri -> {
                if (uri != command.uri) {
                    savePosition()  // Save last media position
                    uri = command.uri
                    command.uri.resolveUri(this@PlayerService)?.let { loadFile(it) }
                    sendEvent(PlayerEvent.Title(""))
                    sendEvent(PlayerEvent.Thumbnail(null))
                }
            }

            is PlayerCommand.SetTitle -> sendEvent(PlayerEvent.Title(command.title))
            is PlayerCommand.SetThumbnail -> sendEvent(PlayerEvent.Thumbnail(command.thumbnail))
            PlayerCommand.Destroy -> stopSelf()
            is PlayerCommand.Paused -> {
                if (!command.paused) {
                    if (keepOpen && eofReached) {
                        seek(0)
                    } else if (isIdling) {
                        command.uri!!.resolveUri(this@PlayerService)?.let { loadFile(it) }
                    }
                }
                paused = command.paused
            }

            PlayerCommand.PlayOrPause -> cyclePause()
            is PlayerCommand.SeekTo -> seek(command.position.coerceIn(0..duration))
            is PlayerCommand.Rotate -> rotate(command.rotate)
            is PlayerCommand.Zoom -> zoom(command.zoom)
            is PlayerCommand.VideoOffset -> offset(
                command.offset.x.toInt(),
                command.offset.y.toInt()
            )

            is PlayerCommand.SetSpeed -> playbackSpeed = command.speed.toDouble()
            is PlayerCommand.SetSubtitleTrack -> sid = command.trackId
            is PlayerCommand.SetAudioTrack -> aid = command.trackId
            is PlayerCommand.Screenshot -> screenshot(onSaveScreenshot = command.onSaveScreenshot)
            is PlayerCommand.AddSubtitle -> addSubtitle(command.filePath)
            is PlayerCommand.AddAudio -> addAudio(command.filePath)
        }
    }

    private fun startForeground(notification: Notification) {
        try {
            ServiceCompat.startForeground(
                this, PlayerNotificationManager.NOTIFICATION_ID, notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                } else {
                    0
                },
            )
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                e is ForegroundServiceStartNotAllowedException
            ) {
                // App not in a valid state to start foreground service (e.g. started from bg)
                e.printStackTrace()
            }
        }
    }

    private fun createMediaSessionCallback() = object : MediaSessionCompat.Callback() {
        override fun onPause() {
            player.paused = true
        }

        override fun onPlay() {
            player.paused = false
        }

        override fun onSeekTo(pos: Long) {
            player.timePos = (pos / 1000).toInt()
        }

        override fun onSkipToNext() = Unit
        override fun onSkipToPrevious() = Unit
        override fun onSetRepeatMode(repeatMode: Int) = Unit

        override fun onSetShuffleMode(shuffleMode: Int) {
            player.changeShuffle(false, shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL)
        }
    }

    private fun loadLastPosition() = uri.resolveUri(this@PlayerService)?.let { path ->
        scope.launch {
            val lastPos = playerRepo.requestLastPlayPosition(path).first()
            if (lastPos > 0 && lastPos.toDouble() / (player.duration * 1000) < 0.9) {
                player.seek((lastPos / 1000).toInt().coerceAtLeast(0))
            }
        }
    } ?: Job().apply { complete() }

    private fun savePosition() = uri.resolveUri(this@PlayerService)?.let { path ->
        val position = sessionManager.playerState.value.position * 1000L
        scope.launch {
            playerRepo.updatePlayHistory(
                MediaPlayHistoryBean(path = path, lastPlayPosition = position)
            ).collect()
        }
    }

    inner class PlayerNotificationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return

            when (intent.action) {
                AudioManager.ACTION_AUDIO_BECOMING_NOISY ->
                    onCommand(PlayerCommand.Paused(true, null))

                PLAY_ACTION -> onCommand(PlayerCommand.PlayOrPause)
                CLOSE_ACTION -> {
                    onCommand(PlayerCommand.Destroy)
                    context?.sendBroadcast(Intent(FINISH_PLAY_ACTIVITY_ACTION))
                }
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(Dispatchers.IO)

        const val PLAY_ACTION = BuildConfig.APPLICATION_ID + ".PlayerPlay"
        const val CLOSE_ACTION = BuildConfig.APPLICATION_ID + ".PlayerClose"
        const val FINISH_PLAY_ACTIVITY_ACTION = BuildConfig.APPLICATION_ID + ".FinishPlayActivity"

        fun createIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(action).apply {
                setPackage(BuildConfig.APPLICATION_ID)
            }
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }
    }
}