package com.skyd.podaura.ui.player.media

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.AppKit.NSImage
import platform.Foundation.NSData
import platform.Foundation.NSMakeSize
import platform.Foundation.NSNumber
import platform.Foundation.create
import platform.MediaPlayer.MPChangePlaybackPositionCommandEvent
import platform.MediaPlayer.MPMediaItemArtwork
import platform.MediaPlayer.MPMediaItemPropertyAlbumTitle
import platform.MediaPlayer.MPMediaItemPropertyArtist
import platform.MediaPlayer.MPMediaItemPropertyArtwork
import platform.MediaPlayer.MPMediaItemPropertyPlaybackDuration
import platform.MediaPlayer.MPMediaItemPropertyPodcastTitle
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPNowPlayingInfoMediaTypeAudio
import platform.MediaPlayer.MPNowPlayingInfoMediaTypeVideo
import platform.MediaPlayer.MPNowPlayingInfoPropertyDefaultPlaybackRate
import platform.MediaPlayer.MPNowPlayingInfoPropertyElapsedPlaybackTime
import platform.MediaPlayer.MPNowPlayingInfoPropertyMediaType
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackQueueCount
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackQueueIndex
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackRate
import platform.MediaPlayer.MPNowPlayingPlaybackStatePaused
import platform.MediaPlayer.MPNowPlayingPlaybackStatePlaying
import platform.MediaPlayer.MPNowPlayingPlaybackStateStopped
import platform.MediaPlayer.MPRemoteCommand
import platform.MediaPlayer.MPRemoteCommandCenter
import platform.MediaPlayer.MPRemoteCommandEvent
import platform.MediaPlayer.MPRemoteCommandHandlerStatusCommandFailed
import platform.MediaPlayer.MPRemoteCommandHandlerStatusNoActionableNowPlayingItem
import platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess

internal class MacosMediaSessionAdapter : DesktopMediaSessionAdapter {
    private val remote = MPRemoteCommandCenter.sharedCommandCenter()
    private val nowPlaying = MPNowPlayingInfoCenter.defaultCenter()
    private val targets = mutableListOf<Pair<MPRemoteCommand, Any>>()
    private var listener: ((DesktopMediaCommand) -> Unit)? = null
    private var artworkId: String? = null
    private var artwork: MPMediaItemArtwork? = null
    private var closed = false

    init {
        command(remote.playCommand) { DesktopMediaCommand.Play }
        command(remote.pauseCommand) { DesktopMediaCommand.Pause }
        command(remote.togglePlayPauseCommand) { DesktopMediaCommand.TogglePlayPause }
        command(remote.previousTrackCommand) { DesktopMediaCommand.Previous }
        command(remote.nextTrackCommand) { DesktopMediaCommand.Next }
        command(remote.changePlaybackPositionCommand) { event ->
            (event as? MPChangePlaybackPositionCommandEvent)?.positionTime
                ?.takeIf(Double::isFinite)
                ?.let(DesktopMediaCommand::ChangePlaybackPosition)
        }
    }

    override fun setCommandListener(listener: (DesktopMediaCommand) -> Unit) {
        this.listener = listener
    }

    private fun command(
        command: MPRemoteCommand,
        map: (MPRemoteCommandEvent) -> DesktopMediaCommand?,
    ) {
        command.enabled = false
        val token = command.addTargetWithHandler { event ->
            val callback = listener
            if (closed || callback == null || !command.enabled) {
                MPRemoteCommandHandlerStatusNoActionableNowPlayingItem
            } else {
                val mapped = event?.let(map)
                if (mapped == null) MPRemoteCommandHandlerStatusCommandFailed else {
                    // The shared controller dispatches player commands on its main-thread scope.
                    callback(mapped)
                    MPRemoteCommandHandlerStatusSuccess
                }
            }
        }
        targets += command to token
    }

    override fun update(snapshot: DesktopMediaSnapshot) {
        if (closed) return
        val info = mutableMapOf<Any?, Any?>()
        snapshot.title?.let { info[MPMediaItemPropertyTitle] = it }
        snapshot.artist?.let { info[MPMediaItemPropertyArtist] = it }
        snapshot.album?.let {
            info[MPMediaItemPropertyAlbumTitle] = it
            info[MPMediaItemPropertyPodcastTitle] = it
        }
        snapshot.durationSeconds?.let {
            info[MPMediaItemPropertyPlaybackDuration] = NSNumber(double = it)
        }
        snapshot.positionSeconds?.let {
            info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = NSNumber(double = it)
        }
        info[MPNowPlayingInfoPropertyPlaybackRate] = NSNumber(double = snapshot.playbackRate)
        info[MPNowPlayingInfoPropertyDefaultPlaybackRate] =
            NSNumber(double = snapshot.defaultPlaybackRate)
        snapshot.queueIndex?.let {
            info[MPNowPlayingInfoPropertyPlaybackQueueIndex] = NSNumber(int = it)
        }
        info[MPNowPlayingInfoPropertyPlaybackQueueCount] = NSNumber(int = snapshot.queueCount)
        info[MPNowPlayingInfoPropertyMediaType] = NSNumber(
            unsignedInteger = if (snapshot.isVideo) MPNowPlayingInfoMediaTypeVideo else MPNowPlayingInfoMediaTypeAudio
        )
        artworkFor(snapshot.artwork)?.let { info[MPMediaItemPropertyArtwork] = it }
        nowPlaying.nowPlayingInfo = info
        nowPlaying.playbackState = when (snapshot.playbackState) {
            DesktopPlaybackState.Playing -> MPNowPlayingPlaybackStatePlaying
            DesktopPlaybackState.Paused -> MPNowPlayingPlaybackStatePaused
            DesktopPlaybackState.Stopped -> MPNowPlayingPlaybackStateStopped
        }
        remote.playCommand.enabled = snapshot.canPlay
        remote.pauseCommand.enabled = snapshot.canPause
        remote.togglePlayPauseCommand.enabled = snapshot.canTogglePlayPause
        remote.previousTrackCommand.enabled = snapshot.canGoPrevious
        remote.nextTrackCommand.enabled = snapshot.canGoNext
        remote.changePlaybackPositionCommand.enabled = snapshot.canChangePlaybackPosition
    }

    private fun artworkFor(value: DesktopArtwork?): MPMediaItemArtwork? {
        if (value?.id == artworkId) return artwork
        artworkId = value?.id
        artwork = null
        if (value == null) return null
        val data = value.data
        if (data.pngBytes.isEmpty() || data.width <= 0 || data.height <= 0) return null
        val image = runCatching {
            data.pngBytes.usePinned {
                NSImage(data = NSData.create(it.addressOf(0), data.pngBytes.size.toULong()))
            }
        }.getOrNull() ?: return null
        artwork = MPMediaItemArtwork(
            NSMakeSize(
                data.width.toDouble(),
                data.height.toDouble()
            )
        ) { requested ->
            val size = requested.useContents {
                NSMakeSize(
                    width.takeIf { it.isFinite() && it > 0.0 }?.coerceAtMost(data.width.toDouble())
                        ?: data.width.toDouble(),
                    height.takeIf { it.isFinite() && it > 0.0 }
                        ?.coerceAtMost(data.height.toDouble())
                        ?: data.height.toDouble(),
                )
            }
            // MediaPlayer's bindings expose NSImage as an Objective-C forward declaration.
            (image.copy() as NSImage).apply { setSize(size) } as objcnames.classes.NSImage
        }
        return artwork
    }

    override fun clear() {
        if (closed) return
        targets.forEach { (command, _) -> command.enabled = false }
        nowPlaying.playbackState = MPNowPlayingPlaybackStateStopped
        nowPlaying.nowPlayingInfo = null
        artworkId = null
        artwork = null
    }

    override fun close() {
        if (closed) return
        clear()
        closed = true
        listener = null
        targets.forEach { (command, token) -> command.removeTarget(token) }
        targets.clear()
    }
}
