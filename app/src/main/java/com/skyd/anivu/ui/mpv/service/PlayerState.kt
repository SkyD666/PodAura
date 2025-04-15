package com.skyd.anivu.ui.mpv.service

import coil3.Bitmap
import com.skyd.anivu.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.anivu.ui.mpv.LoopMode
import com.skyd.anivu.ui.mpv.MPVPlayer


data class PlayerState(
    val playlistId: String = "",
    val playlist: LinkedHashMap<String, PlaylistMediaWithArticleBean> = linkedMapOf(),
    val mediaStarted: Boolean = false,
    val path: String? = null,
    val audioTrackId: Int = 0,
    val subtitleTrackId: Int = 0,
    val videoTrackId: Int = 0,
    val zoom: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val speed: Float = 1f,
    val audioDelay: Long = 0L,
    val subTitleDelay: Long = 0L,
    val videoTracks: List<MPVPlayer.Track> = listOf(),
    val audioTracks: List<MPVPlayer.Track> = listOf(),
    val subtitleTracks: List<MPVPlayer.Track> = listOf(),
    val buffer: Int = 0,
    val artist: String? = null,
    val album: String? = null,
    val position: Long = 0L,
    val duration: Long = 0L,
    val rotate: Float = 0f,
    val playlistPosition: Int = -1,
    val paused: Boolean = true,
    val pausedForCache: Boolean = false,
    val shuffle: Boolean = false,
    val loop: LoopMode = LoopMode.None,
    val idling: Boolean = true,
    val mediaTitle: String? = null,
    val mediaThumbnail: Bitmap? = null,
) {
    val currentMedia = playlist[path]
    val isVideo = videoTracks.any { it.trackId >= 0 && !it.isAlbumArt }
    val playlistFirst = playlistPosition == 0 && playlist.isNotEmpty() || playlist.size == 1
    val playlistLast = playlistPosition == playlist.size - 1 ||
            playlist.size == 1 || playlistPosition == -1
}