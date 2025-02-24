package com.skyd.anivu.ui.mpv.component.state

import androidx.compose.runtime.Immutable
import com.skyd.anivu.ui.mpv.service.PlayerState

data class PlayState(
    val isPlaying: Boolean,
    val isSeeking: Boolean,
    val state: PlayerState,
) {
    companion object {
        val initial = PlayState(
            isPlaying = false,
            isSeeking = false,
            state = PlayerState(),
        )
    }

    val mediaLoaded = state.mediaStarted
    val audioTrackId = state.audioTrackId
    val subtitleTrackId = state.subtitleTrackId
    val videoTrackId = state.videoTrackId
    val zoom = state.zoom
    val offsetX = state.offsetX
    val offsetY = state.offsetY
    val speed = state.speed
    val videoTracks = state.videoTracks
    val audioTracks = state.audioTracks
    val subtitleTracks = state.subtitleTracks
    val buffer = state.buffer
    val mediaArtist = state.artist
    val album = state.album
    val position = state.position
    val duration = state.duration
    val rotate = state.rotate
    val playlistPosition = state.playlistPosition
    val playlistFirst = state.playlistFirst
    val playlistLast = state.playlistLast
    val paused = state.paused
    val pausedForCache = state.pausedForCache
    val shuffle = state.shuffle
    val loop = state.loop
    val idling = state.idling
    val mediaTitle = state.mediaTitle
    val title = state.customMediaData?.title
    val mediaThumbnail = state.mediaThumbnail
    val thumbnail = state.customMediaData?.thumbnail
    val artist = state.customMediaData?.artist
    val isVideo = state.isVideo
    val playlist = state.playlist
    val path = state.path
}

@Immutable
data class PlayStateCallback(
    val onPlayStateChanged: () -> Unit,
    val onPlayOrPause: () -> Unit,
    val onSeekTo: (position: Long) -> Unit,
    val onSpeedChanged: (Float) -> Unit,
    val onPreviousMedia: () -> Unit,
    val onNextMedia: () -> Unit,
    val onCycleLoop: () -> Unit,
    val onShuffle: (Boolean) -> Unit,
    val onPlayFileInPlaylist: (String) -> Unit,
)