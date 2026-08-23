package com.skyd.podaura.ui.player.media

import com.skyd.fundation.jna.mac.MacArtwork
import com.skyd.fundation.jna.mac.MacMediaPlayer
import com.skyd.fundation.jna.mac.MacMediaPlayerSession
import com.skyd.fundation.jna.mac.MacMediaType
import com.skyd.fundation.jna.mac.MacNowPlayingInfo
import com.skyd.fundation.jna.mac.MacPlaybackState
import com.skyd.fundation.jna.mac.MacRemoteCommand
import com.skyd.fundation.jna.mac.MacRemoteCommandAvailability

internal class MacOSDesktopMediaSessionAdapter : DesktopMediaSessionAdapter {
    @Volatile
    private var commandListener: ((DesktopMediaCommand) -> Unit)? = null

    private val session: MacMediaPlayerSession = MacMediaPlayer.openSession { command ->
        commandListener?.let { listener ->
            listener(command.toDesktopCommand())
            true
        } ?: false
    }

    override fun setCommandListener(listener: (DesktopMediaCommand) -> Unit) {
        commandListener = listener
    }

    override fun update(snapshot: DesktopMediaSnapshot) {
        session.update(
            info = MacNowPlayingInfo(
                title = snapshot.title,
                artist = snapshot.artist,
                album = snapshot.album,
                durationSeconds = snapshot.durationSeconds,
                elapsedSeconds = snapshot.positionSeconds,
                playbackRate = snapshot.playbackRate,
                defaultPlaybackRate = snapshot.defaultPlaybackRate,
                queueIndex = snapshot.queueIndex,
                queueCount = snapshot.queueCount,
                mediaType = if (snapshot.isVideo) MacMediaType.Video else MacMediaType.Audio,
                playbackState = when (snapshot.playbackState) {
                    DesktopPlaybackState.Playing -> MacPlaybackState.Playing
                    DesktopPlaybackState.Paused -> MacPlaybackState.Paused
                    DesktopPlaybackState.Stopped -> MacPlaybackState.Stopped
                },
                artwork = snapshot.artwork?.let { artwork ->
                    MacArtwork(
                        id = artwork.id,
                        pngBytes = artwork.data.pngBytes,
                        width = artwork.data.width,
                        height = artwork.data.height,
                    )
                },
            ),
            commandAvailability = MacRemoteCommandAvailability(
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

private fun MacRemoteCommand.toDesktopCommand(): DesktopMediaCommand = when (this) {
    MacRemoteCommand.Play -> DesktopMediaCommand.Play
    MacRemoteCommand.Pause -> DesktopMediaCommand.Pause
    MacRemoteCommand.TogglePlayPause -> DesktopMediaCommand.TogglePlayPause
    MacRemoteCommand.Previous -> DesktopMediaCommand.Previous
    MacRemoteCommand.Next -> DesktopMediaCommand.Next
    is MacRemoteCommand.ChangePlaybackPosition -> {
        DesktopMediaCommand.ChangePlaybackPosition(positionSeconds)
    }
}
