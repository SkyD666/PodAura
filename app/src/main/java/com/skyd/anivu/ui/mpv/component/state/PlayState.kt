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

    val mediaLoaded = state.mediaLoaded
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
    val artist = state.artist
    val album = state.album
    val position = state.position
    val duration = state.duration
    val rotate = state.rotate
    val playlistPosition = state.playlistPosition
    val playlistCount = state.playlistCount
    val paused = state.paused
    val pausedForCache = state.pausedForCache
    val shuffle = state.shuffle
    val idling = state.idling
    val mediaTitle = state.mediaTitle
    val title = state.customMediaData?.title
    val mediaThumbnail = state.mediaThumbnail
    val thumbnail = state.customMediaData?.thumbnail
    val isVideo = state.isVideo
}

@Immutable
data class PlayStateCallback(
    val onPlayStateChanged: () -> Unit,
    val onPlayOrPause: () -> Unit,
    val onSeekTo: (position: Long) -> Unit,
    val onSpeedChanged: (Float) -> Unit,
)