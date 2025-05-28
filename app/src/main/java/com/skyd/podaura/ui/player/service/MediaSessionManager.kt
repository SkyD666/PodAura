package com.skyd.podaura.ui.player.service

import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.compose.ui.graphics.asAndroidBitmap
import coil3.toBitmap
import com.skyd.podaura.R
import com.skyd.podaura.ext.getString
import com.skyd.podaura.ext.toUri
import com.skyd.podaura.ui.player.PlayerEvent
import com.skyd.podaura.ui.player.createThumbnailFile
import com.skyd.podaura.ui.player.playbackState
import com.skyd.podaura.ui.player.LoopMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.close
import podaura.shared.generated.resources.loop_playlist_mode

class MediaSessionManager(
    private val context: Context,
    private val callback: MediaSessionCompat.Callback,
) : PlayerService.Observer {
    val mediaSession: MediaSessionCompat = initMediaSession()
    private val notificationManager = PlayerNotificationManager(context, mediaSession)
    private val mediaMetadataBuilder = MediaMetadataCompat.Builder()

    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private val eventFlow = Channel<PlayerEvent>(Channel.UNLIMITED)

    private val initialPlayerState = PlayerState()
    val playerState = eventFlow
        .consumeAsFlow()
        .scan(initialPlayerState) { old, event ->
            val newState = event.reduce(old)
            event.updateMediaSession(newState)
            return@scan newState
        }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, initialPlayerState)

    private fun initMediaSession(): MediaSessionCompat {
        /*
            https://developer.android.com/guide/topics/media-apps/working-with-a-media-session
            https://developer.android.com/guide/topics/media-apps/audio-app/mediasession-callbacks
            https://developer.android.com/reference/android/support/v4/media/session/MediaSessionCompat
         */
        val session = MediaSessionCompat(context, TAG)
        session.setFlags(0)
        session.setCallback(callback)
        return session
    }

    fun startForegroundService(service: PlayerService) {
        notificationManager.startForegroundService(service, playerState.value)
    }

    fun onDestroy() {
        mediaSession.isActive = false
        mediaSession.release()
        notificationManager.cancel()
    }

    override fun onEvent(event: PlayerEvent) {
        eventFlow.trySend(event)
    }

    private inline fun MediaMetadataCompat.Builder.updateMetadata(
        block: MediaMetadataCompat.Builder.() -> Unit,
    ) {
        block()
        return mediaSession.setMetadata(build())
    }

    private fun PlayerState.buildPlaybackState(): PlaybackStateCompat {
        var state: Int = playbackState()
        var actions = PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_SET_REPEAT_MODE
        if (duration > 0) actions = actions or PlaybackStateCompat.ACTION_SEEK_TO
        if (!playlistFirst) {
            actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        }
        if (!playlistLast) {
            actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        }
        if (playlist.isNotEmpty()) {
            actions = actions or PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE
        }
        mediaSession.isActive = state != PlaybackStateCompat.STATE_NONE
        return with(PlaybackStateCompat.Builder()) {
            setState(state, position * 1000, speed)
            setActions(actions)
            addCustomAction(
                PlaybackStateCompat.CustomAction.Builder(
                    PlayerService.LOOP_ACTION,
                    context.getString(Res.string.loop_playlist_mode),
                    when (loop) {
                        LoopMode.LoopPlaylist -> R.drawable.ic_repeat_on_24
                        LoopMode.LoopFile -> R.drawable.ic_repeat_one_on_24
                        LoopMode.None -> R.drawable.ic_repeat_24
                    },
                ).build()
            )
            addCustomAction(
                PlaybackStateCompat.CustomAction.Builder(
                    PlayerService.CLOSE_ACTION,
                    context.getString(Res.string.close),
                    R.drawable.ic_close_24,
                ).build()
            )
            //setActiveQueueItemId(0) TODO
            build()
        }
    }

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

    private suspend fun PlayerEvent.updateMediaSession(newState: PlayerState) {
        when (this) {
            is PlayerEvent.Shuffle -> mediaSession.setShuffleMode(
                if (shuffle) PlaybackStateCompat.SHUFFLE_MODE_ALL
                else PlaybackStateCompat.SHUFFLE_MODE_NONE
            )

            is PlayerEvent.Loop -> with(mediaSession) {
                setRepeatMode(
                    when (mode) {
                        LoopMode.LoopPlaylist -> PlaybackStateCompat.REPEAT_MODE_ALL
                        LoopMode.LoopFile -> PlaybackStateCompat.REPEAT_MODE_ONE
                        LoopMode.None -> PlaybackStateCompat.REPEAT_MODE_NONE
                    }
                )
                setPlaybackState(newState.buildPlaybackState())
            }

            is PlayerEvent.Paused,
            is PlayerEvent.EndFile,
            is PlayerEvent.Speed,
            is PlayerEvent.Position -> mediaSession.setPlaybackState(newState.buildPlaybackState())

            is PlayerEvent.Duration -> {
                mediaMetadataBuilder.updateMetadata {
                    putLong(
                        MediaMetadataCompat.METADATA_KEY_DURATION,
                        (value * 1000).takeIf { it > 0 } ?: -1,
                    )
                }
                mediaSession.setPlaybackState(newState.buildPlaybackState())
            }

            is PlayerEvent.Loading -> {
                mediaSession.setPlaybackState(newState.buildPlaybackState())
            }

            is PlayerEvent.MediaTitle -> mediaMetadataBuilder.updateMetadata {
                putText(
                    MediaMetadataCompat.METADATA_KEY_TITLE,
                    newState.currentMedia?.title.orEmpty().ifBlank { value })
            }

            is PlayerEvent.Artist -> mediaMetadataBuilder.updateMetadata {
                putText(MediaMetadataCompat.METADATA_KEY_ARTIST, value)
            }

            is PlayerEvent.Album -> mediaMetadataBuilder.updateMetadata {
                putText(MediaMetadataCompat.METADATA_KEY_ALBUM, value)
            }

            is PlayerEvent.MediaThumbnail -> coroutineScope {
                launch {
                    mediaMetadataBuilder.updateMetadata {
                        val thumbnail = newState.currentMedia?.thumbnail?.let { thumbnailUrl ->
                            withContext(Dispatchers.IO) { createThumbnailFile(thumbnailUrl) }
                        }?.toUri(context)
                        if (thumbnail != null) {
                            putString(
                                MediaMetadataCompat.METADATA_KEY_ART_URI,
                                thumbnail.toString()
                            )
                        } else if (newState.mediaThumbnail != null) {
                            putBitmap(MediaMetadataCompat.METADATA_KEY_ART, newState.mediaThumbnail?.toBitmap())
                        }
                    }
                    notificationManager.notifyNotification()
                }
            }

            is PlayerEvent.PlaylistPosition -> Unit
            else -> Unit
        }
        notificationManager.update(this, newState)
    }

    companion object {
        private const val TAG = "MediaSessionManager"
    }
}