package com.skyd.podaura.ui.player

import android.view.Surface
import android.view.SurfaceHolder
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import java.io.File

sealed interface PlayerCommand {
    data class Attach(val surfaceHolder: SurfaceHolder) : PlayerCommand
    data class Detach(val surface: Surface) : PlayerCommand

    data class LoadList(
        val playlist: List<PlaylistMediaWithArticleBean>,
        val startPath: String? = null,
    ) : PlayerCommand

    data class RemoveMediaFromPlaylist(
        val playlist: List<PlaylistMediaWithArticleBean>,
    ) : PlayerCommand

    data object Destroy : PlayerCommand
    data class Paused(val paused: Boolean) : PlayerCommand
    data object PlayOrPause : PlayerCommand
    data object PreviousMedia : PlayerCommand
    data object NextMedia : PlayerCommand
    data class SeekTo(val position: Long) : PlayerCommand
    data class Rotate(val rotate: Int) : PlayerCommand
    data class Zoom(val zoom: Float) : PlayerCommand
    data class VideoOffset(val offset: Offset) : PlayerCommand
    data class AudioDelay(val delayMillis: Long) : PlayerCommand
    data class SubtitleDelay(val delayMillis: Long) : PlayerCommand
    data class SetSpeed(val speed: Float) : PlayerCommand
    data class SetSubtitleTrack(val trackId: Int) : PlayerCommand
    data class SetAudioTrack(val trackId: Int) : PlayerCommand
    data class Screenshot(val onSaveScreenshot: (File) -> Unit) : PlayerCommand
    data class AddSubtitle(val filePath: String) : PlayerCommand
    data class AddAudio(val filePath: String) : PlayerCommand
    data class Shuffle(val shuffle: Boolean) : PlayerCommand
    data class PlayFileInPlaylist(val path: String) : PlayerCommand
    data object CycleLoop : PlayerCommand
}

sealed interface PlayerEvent {
    data object Shutdown : PlayerEvent
    data class Idling(val value: Boolean) : PlayerEvent
    data class Position(val value: Long) : PlayerEvent
    data class Duration(val value: Long) : PlayerEvent
    data class MediaTitle(val value: String) : PlayerEvent
    data class Playlist(
        val playlistId: String,
        val newPlaylist: LinkedHashMap<String, PlaylistMediaWithArticleBean>
    ) : PlayerEvent

    data class Paused(val value: Boolean) : PlayerEvent
    data class Loading(val value: Boolean) : PlayerEvent
    data object Seek : PlayerEvent
    data object EndFile : PlayerEvent
    data class StartFile(val path: String?) : PlayerEvent
    data object PlaybackRestart : PlayerEvent
    data class Zoom(val value: Float) : PlayerEvent
    data class VideoOffsetX(val value: Float) : PlayerEvent
    data class VideoOffsetY(val value: Float) : PlayerEvent
    data class Rotate(val value: Float) : PlayerEvent
    data class Speed(val value: Float) : PlayerEvent
    data class AudioDelay(val value: Long) : PlayerEvent
    data class SubtitleDelay(val value: Long) : PlayerEvent
    data class AllSubtitleTracks(val tracks: List<Track>) : PlayerEvent
    data class SubtitleTrackChanged(val trackId: Int) : PlayerEvent
    data class AllVideoTracks(val tracks: List<Track>) : PlayerEvent
    data class VideoTrackChanged(val trackId: Int) : PlayerEvent
    data class AllAudioTracks(val tracks: List<Track>) : PlayerEvent
    data class AudioTrackChanged(val trackId: Int) : PlayerEvent
    data class Buffer(val bufferDuration: Int) : PlayerEvent
    data class Shuffle(val shuffle: Boolean) : PlayerEvent
    data class Loop(val mode: LoopMode) : PlayerEvent
    data class PlaylistPosition(val value: Int) : PlayerEvent
    data class Artist(val value: String) : PlayerEvent
    data class Album(val value: String) : PlayerEvent
    data class MediaThumbnail(val value: ImageBitmap?) : PlayerEvent
}