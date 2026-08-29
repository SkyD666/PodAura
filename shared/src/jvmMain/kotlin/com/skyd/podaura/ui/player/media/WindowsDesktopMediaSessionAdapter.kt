package com.skyd.podaura.ui.player.media

import com.skyd.fundation.jna.windows.WindowsArtwork
import com.skyd.fundation.jna.windows.WindowsMediaPlayer
import com.skyd.fundation.jna.windows.WindowsMediaPlayerSession
import com.skyd.fundation.jna.windows.WindowsMediaType
import com.skyd.fundation.jna.windows.WindowsMediaWindowRegistration
import com.skyd.fundation.jna.windows.WindowsNowPlayingInfo
import com.skyd.fundation.jna.windows.WindowsPlaybackState
import com.skyd.fundation.jna.windows.WindowsRemoteCommand
import com.skyd.fundation.jna.windows.WindowsRemoteCommandAvailability
import com.skyd.fundation.jna.windows.WindowsTaskbarTooltips

internal class WindowsDesktopMediaSessionAdapter :
    DesktopMediaSessionAdapter,
    DesktopMediaWindowHost {
    @Volatile
    private var commandListener: ((DesktopMediaCommand) -> Unit)? = null

    private val session: WindowsMediaPlayerSession = WindowsMediaPlayer.openSession { command ->
        commandListener?.let { listener ->
            listener(command.toDesktopCommand())
            true
        } ?: false
    }

    override fun setCommandListener(listener: (DesktopMediaCommand) -> Unit) {
        commandListener = listener
    }

    override fun attachWindow(
        windowHandle: Long,
        isMainWindow: Boolean,
        tooltips: DesktopMediaWindowTooltips,
    ): DesktopMediaWindowRegistration = session.attachWindow(
        windowHandle = windowHandle,
        isMainWindow = isMainWindow,
        tooltips = tooltips.toWindowsTooltips(),
    ).toDesktopRegistration()

    override fun update(snapshot: DesktopMediaSnapshot) {
        session.update(
            info = WindowsNowPlayingInfo(
                title = snapshot.title,
                artist = snapshot.artist,
                album = snapshot.album,
                durationSeconds = snapshot.durationSeconds,
                elapsedSeconds = snapshot.positionSeconds,
                playbackRate = snapshot.playbackRate,
                defaultPlaybackRate = snapshot.defaultPlaybackRate,
                queueIndex = snapshot.queueIndex,
                queueCount = snapshot.queueCount,
                mediaType = if (snapshot.isVideo) WindowsMediaType.Video else WindowsMediaType.Audio,
                playbackState = when (snapshot.playbackState) {
                    DesktopPlaybackState.Playing -> WindowsPlaybackState.Playing
                    DesktopPlaybackState.Paused -> WindowsPlaybackState.Paused
                    DesktopPlaybackState.Stopped -> WindowsPlaybackState.Stopped
                },
                artwork = snapshot.artwork?.let { artwork ->
                    WindowsArtwork(
                        id = artwork.id,
                        pngBytes = artwork.data.pngBytes,
                        width = artwork.data.width,
                        height = artwork.data.height,
                    )
                },
            ),
            commandAvailability = WindowsRemoteCommandAvailability(
                canPlay = snapshot.canPlay,
                canPause = snapshot.canPause,
                canTogglePlayPause = snapshot.canTogglePlayPause,
                canGoPrevious = snapshot.canGoPrevious,
                canGoNext = snapshot.canGoNext,
                canChangePlaybackPosition = snapshot.canChangePlaybackPosition,
            ),
        )
    }

    override fun clear() = session.clear()

    override fun close() {
        commandListener = null
        session.close()
    }
}

private fun WindowsMediaWindowRegistration.toDesktopRegistration() =
    object : DesktopMediaWindowRegistration {
        override fun updateTooltips(tooltips: DesktopMediaWindowTooltips) {
            this@toDesktopRegistration.updateTooltips(tooltips.toWindowsTooltips())
        }

        override fun close() = this@toDesktopRegistration.close()
    }

private fun DesktopMediaWindowTooltips.toWindowsTooltips() = WindowsTaskbarTooltips(
    previous = previous,
    play = play,
    pause = pause,
    next = next,
)

private fun WindowsRemoteCommand.toDesktopCommand(): DesktopMediaCommand = when (this) {
    WindowsRemoteCommand.Play -> DesktopMediaCommand.Play
    WindowsRemoteCommand.Pause -> DesktopMediaCommand.Pause
    WindowsRemoteCommand.TogglePlayPause -> DesktopMediaCommand.TogglePlayPause
    WindowsRemoteCommand.Previous -> DesktopMediaCommand.Previous
    WindowsRemoteCommand.Next -> DesktopMediaCommand.Next
    is WindowsRemoteCommand.ChangePlaybackPosition ->
        DesktopMediaCommand.ChangePlaybackPosition(positionSeconds)
}
