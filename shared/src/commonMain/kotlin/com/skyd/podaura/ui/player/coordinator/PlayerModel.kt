package com.skyd.podaura.ui.player.coordinator

import com.skyd.podaura.ui.player.PlayerEvent
import com.skyd.podaura.ui.player.service.PlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn

class PlayerModel : PlayerCoordinator.Observer {
    private val eventFlow = Channel<PlayerEvent>(Channel.UNLIMITED)
    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private val initialPlayerState = PlayerState()
    val playerState = eventFlow
        .consumeAsFlow()
        .scan(initialPlayerState) { old, event ->
            val newState = event.reduce(old)
            _newStateByEvent.emit(newState to event)
            return@scan newState
        }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, initialPlayerState)

    private val _newStateByEvent = MutableSharedFlow<Pair<PlayerState, PlayerEvent>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val newStateByEvent = _newStateByEvent.asSharedFlow()

    private fun PlayerEvent.reduce(old: PlayerState): PlayerState = when (this) {
        is PlayerEvent.Album -> old.copy(album = value)
        is PlayerEvent.AllAudioTracks -> old.copy(audioTracks = tracks)
        is PlayerEvent.AllSubtitleTracks -> old.copy(subtitleTracks = tracks)
        is PlayerEvent.AllVideoTracks -> old.copy(videoTracks = tracks)
        is PlayerEvent.Artist -> old.copy(artist = value)
        is PlayerEvent.AudioTrackChanged -> old.copy(audioTrackId = trackId)
        is PlayerEvent.Buffer -> old.copy(buffer = bufferDuration)
        is PlayerEvent.Duration -> old.copy(duration = value)
        is PlayerEvent.Idling -> old.copy(idling = value)
        is PlayerEvent.Paused -> old.copy(paused = value)
        is PlayerEvent.Loading -> old.copy(loading = value)
        is PlayerEvent.PlaylistPosition -> old.copy(playlistPosition = value)
        is PlayerEvent.Position -> old.copy(position = value)
        is PlayerEvent.Rotate -> old.copy(rotate = value)
        is PlayerEvent.Shuffle -> old.copy(shuffle = shuffle)
        is PlayerEvent.Loop -> old.copy(loop = mode)
        is PlayerEvent.Speed -> old.copy(speed = value)
        is PlayerEvent.SubtitleTrackChanged -> old.copy(subtitleTrackId = trackId)
        is PlayerEvent.VideoTrackChanged -> old.copy(videoTrackId = trackId)
        is PlayerEvent.MediaThumbnail -> old.copy(mediaThumbnail = value)
        is PlayerEvent.MediaTitle -> old.copy(mediaTitle = value)
        is PlayerEvent.VideoOffsetX -> old.copy(offsetX = value)
        is PlayerEvent.VideoOffsetY -> old.copy(offsetY = value)
        is PlayerEvent.Zoom -> old.copy(zoom = value)
        is PlayerEvent.AudioDelay -> old.copy(audioDelay = value)
        is PlayerEvent.SubtitleDelay -> old.copy(subTitleDelay = value)
        is PlayerEvent.PlaybackRestart -> old.copy(mediaStarted = true)
        is PlayerEvent.StartFile -> old.copy(mediaStarted = true, path = path)
        is PlayerEvent.EndFile -> old.copy(paused = true, mediaStarted = false)
        is PlayerEvent.Playlist -> old.copy(playlistId = playlistId, playlist = newPlaylist)

        else -> old
    }

    override fun onEvent(event: PlayerEvent) {
        eventFlow.trySend(event)
    }
}