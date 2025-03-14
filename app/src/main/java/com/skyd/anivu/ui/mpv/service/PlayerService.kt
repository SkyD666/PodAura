package com.skyd.anivu.ui.mpv.service

import android.app.Application
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.content.ContextCompat
import com.skyd.anivu.BuildConfig
import com.skyd.anivu.appContext
import com.skyd.anivu.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.anivu.model.bean.playlist.PlaylistMediaWithArticleBean.Companion.articleId
import com.skyd.anivu.model.repository.player.PlayerRepository
import com.skyd.anivu.model.repository.playlist.PlaylistMediaRepository
import com.skyd.anivu.ui.mpv.LoopMode
import com.skyd.anivu.ui.mpv.MPVPlayer
import com.skyd.anivu.ui.mpv.PlayerCommand
import com.skyd.anivu.ui.mpv.PlayerEvent
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

    @Inject
    lateinit var playlistMediaRepo: PlaylistMediaRepository

    private val lifecycleScope = CoroutineScope(Dispatchers.Main)

    private val playerNotificationReceiver = PlayerNotificationReceiver()
    private val binder = PlayerServiceBinder()
    val player = MPVPlayer.getInstance(appContext as Application)
    private val sessionManager = MediaSessionManager(appContext, createMediaSessionCallback())
    private val notificationManager = PlayerNotificationManager(appContext, sessionManager)
    val playerState get() = sessionManager.playerState
    private var playlistId: String = ""
    private val cachedPlaylistMap = linkedMapOf<String, PlaylistMediaWithArticleBean>()

    private val observers = mutableSetOf<Observer>()

    private val mpvObserver = object : MPVLib.EventObserver {
        override fun eventProperty(property: String) {
            when (property) {
                "aid" -> sendEvent(PlayerEvent.AudioTrackChanged(player.aid))
                "sid" -> sendEvent(PlayerEvent.SubtitleTrackChanged(player.sid))
                "vid" -> sendEvent(PlayerEvent.VideoTrackChanged(player.vid))
                "video-zoom" -> sendEvent(PlayerEvent.Zoom(2.0.pow(player.videoZoom).toFloat()))
                "video-pan-x" -> sendEvent(
                    PlayerEvent.VideoOffsetX((player.videoPanX * (player.videoDW ?: 0)).toFloat())
                )

                "video-pan-y" -> sendEvent(
                    PlayerEvent.VideoOffsetY((player.videoPanY * (player.videoDH ?: 0)).toFloat())
                )

                "speed" -> sendEvent(PlayerEvent.Speed(player.playbackSpeed.toFloat()))
                "playlist" -> {
                    val playlistMap = LinkedHashMap<String, PlaylistMediaWithArticleBean>()
                    player.loadPlaylist().forEachIndexed { index, url ->
                        playlistMap[url] =
                            cachedPlaylistMap[url] ?: PlaylistMediaWithArticleBean.fromUrl(
                                playlistId = playlistId,
                                url = url,
                                orderPosition = index.toDouble(),
                            )
                    }
                    sendEvent(PlayerEvent.Playlist(playlistId, playlistMap))
                }

                "track-list" -> {
                    player.loadTracks()
                    sendEvent(PlayerEvent.AllSubtitleTracks(player.subtitleTrack))
                    sendEvent(PlayerEvent.AllVideoTracks(player.videoTrack))
                    sendEvent(PlayerEvent.AllAudioTracks(player.audioTrack))
                }

                "demuxer-cache-duration" -> sendEvent(PlayerEvent.Buffer(player.demuxerCacheDuration.toInt()))
                "loop-file", "loop-playlist" -> sendEvent(
                    PlayerEvent.Loop(
                        if (player.loopPlaylist) LoopMode.LoopPlaylist
                        else if (player.loopOne) LoopMode.LoopFile
                        else LoopMode.None
                    )
                )

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

        private var currentPath: String? = null
        override fun event(eventId: Int) {
            when (eventId) {
                MPVLib.mpvEventId.MPV_EVENT_SEEK -> sendEvent(PlayerEvent.Seek)
                MPVLib.mpvEventId.MPV_EVENT_START_FILE -> {
                    savePosition(currentPath)
                    currentPath = player.path
                    currentPath?.let { currentPath ->
                        scope.launch {
                            playerRepo.insertPlayHistory(
                                path = currentPath,
                                duration = player.duration.toLong(),
                                articleId = cachedPlaylistMap[currentPath]?.articleId
                            ).collect()
                        }
                    }
                    sendEvent(PlayerEvent.StartFile(currentPath))
                }

                MPVLib.mpvEventId.MPV_EVENT_END_FILE -> {
                    sendEvent(PlayerEvent.EndFile)
                    savePosition(currentPath)
                    currentPath = null
                }

                MPVLib.mpvEventId.MPV_EVENT_FILE_LOADED -> {
                    sendEvent(PlayerEvent.Paused(player.paused))
                    loadLastPosition(currentPath).invokeOnCompletion {
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
                addAction(PREVIOUS_ACTION)
                addAction(NEXT_ACTION)
                addAction(LOOP_ACTION)
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
        savePosition(player.path)

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
        notificationManager.startForegroundService(this)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    inner class PlayerServiceBinder : Binder() {
        fun getService(): PlayerService = this@PlayerService
    }

    fun interface Observer {
        fun onEvent(event: PlayerEvent)
    }

    fun addObserver(observer: Observer) = observers.add(observer)
    fun removeObserver(observer: Observer) = observers.remove(observer)
    fun removeAllObserver() = observers.clear()
    private fun sendEvent(event: PlayerEvent) {
        observers.forEach { it.onEvent(event) }
    }

    fun onCommand(command: PlayerCommand) = player.apply {
        when (command) {
            is PlayerCommand.Attach -> command.surfaceHolder.addCallback(this)
            is PlayerCommand.Detach -> command.surface.release()
            is PlayerCommand.LoadList -> {
                playlistId =
                    command.playlist.firstOrNull()?.playlistMediaBean?.playlistId.orEmpty()
                cachedPlaylistMap.clear()
                cachedPlaylistMap.putAll(command.playlist.map { it.playlistMediaBean.url to it })
                loadList(
                    files = command.playlist.map { it.playlistMediaBean.url },
                    startFile = command.startPath
                )
            }

            is PlayerCommand.RemoveMediaFromPlaylist -> {
                if (playlistId != command.playlist.firstOrNull()?.playlistMediaBean?.playlistId) {
                    return@apply
                }
                command.playlist.forEach {
                    cachedPlaylistMap.remove(it.playlistMediaBean.url)
                }
                scope.launch {
                    playlistMediaRepo.deletePlaylistMediaByIdAndUrl(command.playlist).collect()
                }
                removeFromList(command.playlist.map { it.playlistMediaBean.url })
            }

            PlayerCommand.Destroy -> stopSelf()
            is PlayerCommand.Paused -> {
                if (!command.paused) {
                    if (keepOpen && eofReached) {
                        seek(0)
                    } else if (isIdling && playlistCount > 0) {
                        playMediaAtIndex(playlistCount - 1)
                    }
                }
                paused = command.paused
            }

            PlayerCommand.PlayOrPause -> cyclePause()
            PlayerCommand.PreviousMedia -> playlistPrev()
            PlayerCommand.NextMedia -> playlistNext()
            is PlayerCommand.SeekTo -> seek(command.position.coerceIn(0L..duration).toInt())
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
            is PlayerCommand.Shuffle -> shuffle(command.shuffle)
            is PlayerCommand.CycleLoop -> {
                val entries = LoopMode.entries
                when (entries[(playerState.value.loop.ordinal + 1) % entries.size]) {
                    LoopMode.LoopPlaylist -> loopPlaylist()
                    LoopMode.LoopFile -> loopFile()
                    LoopMode.None -> loopNo()
                }
            }

            is PlayerCommand.PlayFileInPlaylist -> playFileInPlaylist(command.path)
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
            player.shuffle(shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL)
        }
    }

    private fun loadLastPosition(path: String?) = if (path != null) {
        scope.launch {
            val lastPos = playerRepo.requestLastPlayPosition(path).first()
            if (lastPos > 0 && lastPos.toDouble() / (player.duration * 1000) < 0.9) {
                player.seek((lastPos / 1000).toInt().coerceAtLeast(0))
            }
        }
    } else Job().apply { complete() }

    private fun savePosition(path: String?) = if (path != null) {
        val position = playerState.value.position * 1000L
        scope.launch {
            playerRepo.updateLastPlayPosition(path = path, lastPlayPosition = position).collect()
        }
    } else Job().apply { complete() }

    inner class PlayerNotificationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return

            when (intent.action) {
                AudioManager.ACTION_AUDIO_BECOMING_NOISY ->
                    onCommand(PlayerCommand.Paused(true))

                PLAY_ACTION -> onCommand(PlayerCommand.PlayOrPause)
                PREVIOUS_ACTION -> onCommand(PlayerCommand.PreviousMedia)
                NEXT_ACTION -> onCommand(PlayerCommand.NextMedia)
                LOOP_ACTION -> onCommand(PlayerCommand.CycleLoop)
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