package com.skyd.anivu.ui.mpv

import android.graphics.Bitmap
import android.view.Surface
import android.view.SurfaceHolder
import androidx.compose.ui.geometry.Offset
import com.skyd.anivu.ui.mpv.MPVPlayer.Track
import com.skyd.anivu.ui.mpv.service.CustomMediaData
import java.io.File

sealed interface PlayerCommand {
    data class Attach(val surfaceHolder: SurfaceHolder) : PlayerCommand
    data class Detach(val surface: Surface) : PlayerCommand
    data class SetPath(
        val path: String,
        val articleId: String? = null,
        val title: String? = null,
        val thumbnail: Bitmap? = null,
    ) : PlayerCommand

    data object Destroy : PlayerCommand
    data class Paused(val paused: Boolean) : PlayerCommand
    data object PlayOrPause : PlayerCommand
    data class SeekTo(val position: Long) : PlayerCommand
    data class Rotate(val rotate: Int) : PlayerCommand
    data class Zoom(val zoom: Float) : PlayerCommand
    data class VideoOffset(val offset: Offset) : PlayerCommand
    data class SetSpeed(val speed: Float) : PlayerCommand
    data class SetSubtitleTrack(val trackId: Int) : PlayerCommand
    data class SetAudioTrack(val trackId: Int) : PlayerCommand
    data class Screenshot(val onSaveScreenshot: (File) -> Unit) : PlayerCommand
    data class AddSubtitle(val filePath: String) : PlayerCommand
    data class AddAudio(val filePath: String) : PlayerCommand
}

sealed interface PlayerEvent {
    data object ServiceDestroy : PlayerEvent
    data object Shutdown : PlayerEvent
    data class Idling(val value: Boolean) : PlayerEvent
    data class Position(val value: Long) : PlayerEvent
    data class Duration(val value: Long) : PlayerEvent
    data class MediaTitle(val value: String) : PlayerEvent
    data class CustomData(val path: String, val value: CustomMediaData) : PlayerEvent
    data class Paused(val value: Boolean) : PlayerEvent
    data class PausedForCache(val value: Boolean) : PlayerEvent
    data object Seek : PlayerEvent
    data object EndFile : PlayerEvent
    data class FileLoaded(val path: String?) : PlayerEvent
    data object PlaybackRestart : PlayerEvent
    data class Zoom(val value: Float) : PlayerEvent
    data class VideoOffsetX(val value: Float) : PlayerEvent
    data class VideoOffsetY(val value: Float) : PlayerEvent
    data class Rotate(val value: Float) : PlayerEvent
    data class Speed(val value: Float) : PlayerEvent
    data class AllSubtitleTracks(val tracks: List<Track>) : PlayerEvent
    data class SubtitleTrackChanged(val trackId: Int) : PlayerEvent
    data class AllVideoTracks(val tracks: List<Track>) : PlayerEvent
    data class VideoTrackChanged(val trackId: Int) : PlayerEvent
    data class AllAudioTracks(val tracks: List<Track>) : PlayerEvent
    data class AudioTrackChanged(val trackId: Int) : PlayerEvent
    data class Buffer(val bufferDuration: Int) : PlayerEvent
    data class Shuffle(val value: Boolean) : PlayerEvent
    data class Loop(val value: Int) : PlayerEvent
    data class PlaylistPosition(val value: Int) : PlayerEvent
    data class PlaylistCount(val value: Int) : PlayerEvent
    data class Artist(val value: String) : PlayerEvent
    data class Album(val value: String) : PlayerEvent
    data class MediaThumbnail(val value: Bitmap?) : PlayerEvent
}