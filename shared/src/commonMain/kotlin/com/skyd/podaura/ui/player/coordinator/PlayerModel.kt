package com.skyd.podaura.ui.player.coordinator

import com.skyd.podaura.ui.player.PlaybackFailure
import com.skyd.podaura.ui.player.PlayerEvent
import com.skyd.podaura.ui.player.service.PlayerState
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class PlayerModel : PlayerCoordinator.Observer {
    private val initialPlayerState = PlayerState()
    private val _playerState = MutableStateFlow(initialPlayerState)
    val playerState = _playerState.asStateFlow()

    private val _newStateByEvent = MutableSharedFlow<Pair<PlayerState, PlayerEvent>>(
        extraBufferCapacity = 64,
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
        is PlayerEvent.Seekable -> old.copy(seekable = value)
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
        is PlayerEvent.PlaybackRestart -> old.clearPlaybackEnd().copy(
            mediaStarted = true,
        )

        is PlayerEvent.StartFile -> old.clearPlaybackEnd().copy(
            mediaStarted = true,
            seekable = false,
            path = path,
            audioTrackId = 0,
            subtitleTrackId = 0,
            videoTrackId = 0,
            videoTracks = emptyList(),
            audioTracks = emptyList(),
            subtitleTracks = emptyList(),
            buffer = 0,
            artist = null,
            album = null,
            position = 0L,
            duration = 0L,
            loading = true,
            idling = false,
            mediaTitle = null,
            mediaThumbnail = null,
        )

        is PlayerEvent.FileLoaded -> old.copy(
            videoTracks = videoTracks,
            audioTracks = audioTracks,
            subtitleTracks = subtitleTracks,
            videoTrackId = videoTrackId,
            audioTrackId = audioTrackId,
            subtitleTrackId = subtitleTrackId,
        )

        is PlayerEvent.EndFile -> old.clearPlaybackEnd().copy(
            paused = true,
            mediaStarted = false,
            lastPlaybackEnd = end,
        )

        is PlayerEvent.PlaybackFailed -> old.enqueueFailure(failure)

        PlayerEvent.ClearPlaybackEnd -> old.clearPlaybackEnd()
        is PlayerEvent.Playlist -> old.copy(playlistId = playlistId, playlist = newPlaylist)
        is PlayerEvent.Shutdown -> initialPlayerState
        else -> old
    }

    private fun PlayerState.clearPlaybackEnd() = copy(
        lastPlaybackEnd = null,
        pendingPlaybackFailures = pendingPlaybackFailures.filter { it.retryEnd == null },
    )

    private fun PlayerState.enqueueFailure(failure: PlaybackFailure) = copy(
        // Retain recent notifications while no player UI is collecting them.
        pendingPlaybackFailures = (pendingPlaybackFailures + failure).takeLast(MAX_PENDING_FAILURES),
    )

    // Called by the coordinator actor, just like onEvent, without broadcasting a player event.
    internal fun consumePlaybackFailure(id: String): PlaybackFailure? {
        val state = _playerState.value
        val failure = state.pendingPlaybackFailures.firstOrNull { it.id == id } ?: return null
        _playerState.value = state.copy(
            pendingPlaybackFailures = state.pendingPlaybackFailures.filterNot { it.id == id },
        )
        return failure
    }

    override fun onEvent(event: PlayerEvent) {
        val oldState = _playerState.value
        val newState = event.reduce(oldState)
        if (newState != oldState) _playerState.value = newState
        _newStateByEvent.tryEmit(newState to event)
    }

    internal companion object {
        const val MAX_PENDING_FAILURES = 32
    }
}
