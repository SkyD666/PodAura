package com.skyd.podaura.ui.player.coordinator

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.skyd.fundation.di.inject
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.model.bean.playlist.MediaUrlWithArticleIdBean.Companion.toMediaUrlWithArticleIdBean
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean.Companion.articleId
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.preference.player.PlayerLoopModePreference
import com.skyd.podaura.model.repository.player.IPlayerRepository
import com.skyd.podaura.model.repository.playlist.IAddToPlaylistRepository
import com.skyd.podaura.ui.player.LoopMode
import com.skyd.podaura.ui.player.PlayerCommand
import com.skyd.podaura.ui.player.PlayerEvent
import com.skyd.podaura.ui.player.mpv.EventListener
import com.skyd.podaura.ui.player.mpv.MPVEvent
import com.skyd.podaura.ui.player.mpv.MPVPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow

class PlayerCoordinator : LifecycleOwner {
    override val lifecycle = LifecycleRegistry(this)
    private val playerRepo: IPlayerRepository by inject()
    private val addToPlaylistRepo: IAddToPlaylistRepository by inject()
    val player = MPVPlayer.instance
    val model = PlayerModel()
    val playerState get() = model.playerState
    private var playlistId: String = ""
    private val cachedPlaylistMap = linkedMapOf<String, PlaylistMediaWithArticleBean>()
    private val observers = mutableSetOf<Observer>()

    private val mpvObserver = object : EventListener {
        override fun onPropertyChange(name: String) {
            when (name) {
                "aid" -> sendEvent(PlayerEvent.AudioTrackChanged(player.aid))
                "sid" -> sendEvent(PlayerEvent.SubtitleTrackChanged(player.sid))
                "vid" -> sendEvent(PlayerEvent.VideoTrackChanged(player.vid))
                "playlist" -> {
                    val playlistMap = LinkedHashMap<String, PlaylistMediaWithArticleBean>()
                    player.loadPlaylist().forEachIndexed { index, url ->
                        playlistMap[url] =
                            cachedPlaylistMap[url]
                                ?: PlaylistMediaWithArticleBean.fromUrl(
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

                "loop-file", "loop-playlist" -> {
                    val mode = if (player.loopPlaylist) LoopMode.LoopPlaylist
                    else if (player.loopOne) LoopMode.LoopFile
                    else LoopMode.None

                    sendEvent(PlayerEvent.Loop(mode))
                    PlayerLoopModePreference.put(scope, mode)
                }

                "metadata" -> {
                    sendEvent(PlayerEvent.Artist(player.artist))
                    sendEvent(PlayerEvent.Album(player.album))
                }
            }
        }

        override fun onPropertyChange(name: String, value: Long) {
            when (name) {
                "aid" -> sendEvent(PlayerEvent.AudioTrackChanged(value.toInt()))
                "sid" -> sendEvent(PlayerEvent.SubtitleTrackChanged(value.toInt()))
                "time-pos" -> sendEvent(PlayerEvent.Position(value))
                "duration" -> sendEvent(PlayerEvent.Duration(value))
                "video-rotate" -> sendEvent(PlayerEvent.Rotate(value.toFloat()))
                "playlist-pos" -> sendEvent(PlayerEvent.PlaylistPosition(value.toInt()))
            }
        }

        override fun onPropertyChange(name: String, value: Boolean) {
            when (name) {
                "pause" -> sendEvent(PlayerEvent.Paused(value))
                "paused-for-cache",
                "core-idle",
                "demuxer-cache-idle" -> sendEvent(PlayerEvent.Loading(player.loading()))

                "shuffle" -> sendEvent(PlayerEvent.Shuffle(value))
                "idle-active" -> sendEvent(PlayerEvent.Idling(value))
            }
        }

        override fun onPropertyChange(name: String, value: String) {
            when (name) {
                "media-title" -> sendEvent(PlayerEvent.MediaTitle(value))
            }
        }

        override fun onPropertyChange(name: String, value: Double) {
            when (name) {
                "video-zoom" -> sendEvent(PlayerEvent.Zoom(2.0.pow(value).toFloat()))
                "video-pan-x" -> sendEvent(
                    PlayerEvent.VideoOffsetX((value * (player.videoDW ?: 0)).toFloat())
                )

                "video-pan-y" -> sendEvent(
                    PlayerEvent.VideoOffsetY((value * (player.videoDH ?: 0)).toFloat())
                )

                "speed" -> sendEvent(PlayerEvent.Speed(value.toFloat()))
                "audio-delay" -> sendEvent(PlayerEvent.AudioDelay((value * 1000).toLong()))
                "sub-delay" -> sendEvent(PlayerEvent.SubtitleDelay((value * 1000).toLong()))
                "demuxer-cache-duration" -> sendEvent(PlayerEvent.Buffer(value.toInt()))
            }
        }

        private var currentPath: String? = null
        private var currentPathPlayed: Boolean = false
        override fun onEvent(event: Int) {
            when (event) {
                MPVEvent.SEEK -> sendEvent(PlayerEvent.Seek)
                MPVEvent.START_FILE -> {
                    currentPath = player.path
                    sendEvent(PlayerEvent.StartFile(currentPath))
                    sendEvent(PlayerEvent.Loading(true))
                }

                MPVEvent.END_FILE -> {
                    sendEvent(PlayerEvent.EndFile)
                    sendEvent(PlayerEvent.Loading(false))
                    if (currentPathPlayed) {
                        savePosition(currentPath)
                    }
                    currentPath = null
                    currentPathPlayed = false
                }

                MPVEvent.FILE_LOADED -> {
                    currentPathPlayed = true
                    currentPath?.let { currentPath ->
                        scope.launch {
                            playerRepo.insertPlayHistory(
                                path = currentPath,
                                duration = player.duration.toLong(),
                                articleId = cachedPlaylistMap[currentPath]?.articleId
                            ).collect()
                        }
                    }
                    sendEvent(PlayerEvent.Paused(player.paused))
                    sendEvent(PlayerEvent.Loading(player.loading()))
                    loadLastPosition(currentPath).invokeOnCompletion {
                        sendEvent(PlayerEvent.MediaThumbnail(player.thumbnail))
                    }
                }

                MPVEvent.PLAYBACK_RESTART -> {
                    sendEvent(PlayerEvent.PlaybackRestart)
                    sendEvent(PlayerEvent.Paused(player.paused))
                }

                MPVEvent.SHUTDOWN -> {
                    sendEvent(PlayerEvent.Shutdown)
                    destroy()
                    lifecycle.currentState = Lifecycle.State.DESTROYED
                }
            }
        }
    }

    private fun destroy() {
        savePosition(player.path)
        player.destroy()
        MPVPlayer.instance.mpv.removeEventListener(mpvObserver)
        removeAllObserver()
    }

    private fun initPlayer() {
        loop(PlayerLoopModePreference.toLoopMode(dataStore.getOrDefault(PlayerLoopModePreference)))
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
            is PlayerCommand.Attach -> onAttach(command.surfaceHolder)
            is PlayerCommand.Detach -> onDetach(command.surfaceHolder)
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
                    addToPlaylistRepo.removeMediaFromPlaylist(
                        playlistId = playlistId,
                        mediaList = command.playlist.map { it.toMediaUrlWithArticleIdBean() },
                    ).collect()
                }
                removeFromList(command.playlist.map { it.playlistMediaBean.url })
            }

            PlayerCommand.Destroy -> {
                destroy()
                lifecycle.currentState = Lifecycle.State.DESTROYED
            }

            is PlayerCommand.Paused -> {
                if (!command.paused) {
                    if (keepOpen && eofReached) {
                        seek(0)
                    } else if (isIdling && playlistCount > 0) {
                        // playlist-current-pos
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

            is PlayerCommand.AudioDelay -> audioDelay(command.delayMillis)
            is PlayerCommand.SubtitleDelay -> subtitleDelay(command.delayMillis)
            is PlayerCommand.SetSpeed -> playbackSpeed = command.speed.toDouble()
            is PlayerCommand.SetSubtitleTrack -> sid = command.trackId
            is PlayerCommand.SetAudioTrack -> aid = command.trackId
            is PlayerCommand.Screenshot -> screenshot(onSaveScreenshot = command.onSaveScreenshot)
            is PlayerCommand.AddSubtitle -> addSubtitle(command.filePath)
            is PlayerCommand.AddAudio -> addAudio(command.filePath)
            is PlayerCommand.Shuffle -> shuffle(command.shuffle)
            is PlayerCommand.CycleLoop -> {
                val entries = LoopMode.entries
                loop(entries[(playerState.value.loop.ordinal + 1) % entries.size])
            }

            is PlayerCommand.PlayFileInPlaylist -> playFileInPlaylist(command.path)
        }
    }

    private fun loop(mode: LoopMode) {
        when (mode) {
            LoopMode.LoopPlaylist -> player.loopPlaylist()
            LoopMode.LoopFile -> player.loopFile()
            LoopMode.None -> player.loopNo()
        }
    }

    private fun loadLastPosition(path: String?) = if (path != null) {
        scope.launch {
            val lastPos = playerRepo.requestLastPlayPosition(path).first()
            if (lastPos > 0 && abs(player.duration - lastPos / 1000) > 20) {
                player.seek((lastPos / 1000).toInt().coerceAtLeast(0))
            }
        }
    } else Job().apply { complete() }

    private fun savePosition(path: String?) = if (path != null) {
        val position = playerState.value.position * 1000L
        scope.launch {
            if (position > 1000L) {
                playerRepo.updateLastPlayPosition(
                    path = path, lastPlayPosition = position
                ).collect()
            }
        }
    } else Job().apply { complete() }

    init {
        lifecycle.currentState = Lifecycle.State.CREATED
        addObserver(model)
        MPVPlayer.instance.mpv.addEventListener(mpvObserver)
        initPlayer()
        lifecycle.currentState = Lifecycle.State.RESUMED
    }

    companion object {
        private val scope = CoroutineScope(Dispatchers.IO)
    }
}