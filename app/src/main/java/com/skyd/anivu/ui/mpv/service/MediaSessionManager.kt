package com.skyd.anivu.ui.mpv.service

import android.content.Context
import android.graphics.Bitmap
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.collection.LruCache
import com.skyd.anivu.ui.mpv.LoopMode
import com.skyd.anivu.ui.mpv.PlayerEvent
import com.skyd.anivu.ui.mpv.createThumbnail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaSessionManager(
    private val context: Context,
    private val callback: MediaSessionCompat.Callback,
) : PlayerService.Observer {
    val mediaSession: MediaSessionCompat = initMediaSession()
    private val mediaMetadataBuilder = MediaMetadataCompat.Builder()
    private val playbackStateBuilder = PlaybackStateCompat.Builder()

    private val scope = CoroutineScope(Dispatchers.Main)
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

    var state: Int = PlaybackStateCompat.STATE_NONE
        private set

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

    override fun onEvent(event: PlayerEvent) {
        eventFlow.trySend(event)
    }

    private var thumbnailCache = LruCache<String, Bitmap>(maxSize = 2)
    private suspend fun PlayerState.buildMediaMetadata(): MediaMetadataCompat {
        // TODO could provide: genre, num_tracks, track_number, year
        return with(mediaMetadataBuilder) {
            putText(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
            val thumbnail = currentMedia?.thumbnail
            var customThumbnail: Bitmap? = null
            if (thumbnail != null) {
                customThumbnail = thumbnailCache[thumbnail]
                if (customThumbnail == null) {
                    val newThumb = withContext(Dispatchers.IO) { createThumbnail(thumbnail) }
                    if (newThumb != null) {
                        thumbnailCache.put(thumbnail, newThumb)
                        customThumbnail = newThumb
                    }
                }
            }
            // put even if it's null to reset any previous art
            putBitmap(MediaMetadataCompat.METADATA_KEY_ART, customThumbnail ?: mediaThumbnail)
            putText(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
            putLong(
                MediaMetadataCompat.METADATA_KEY_DURATION,
                (duration * 1000).takeIf { it > 0 } ?: -1)
            putText(
                MediaMetadataCompat.METADATA_KEY_TITLE,
                currentMedia?.title.orEmpty().ifBlank { mediaTitle })
            build()
        }
    }

    private fun PlayerState.buildPlaybackState(): PlaybackStateCompat {
        state = when {
            idling || position < 0 || duration <= 0 || playlist.isEmpty() -> {
                PlaybackStateCompat.STATE_NONE
            }

            pausedForCache -> PlaybackStateCompat.STATE_BUFFERING
            paused -> PlaybackStateCompat.STATE_PAUSED
            else -> PlaybackStateCompat.STATE_PLAYING
        }
        var actions = PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_SET_REPEAT_MODE
        if (duration > 0) actions = actions or PlaybackStateCompat.ACTION_SEEK_TO
        if (playlist.isNotEmpty()) {
            // we could be very pedantic here but it's probably better to either show both or none
            actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE
        }
        mediaSession.isActive = state != PlaybackStateCompat.STATE_NONE
        return with(playbackStateBuilder) {
            setState(state, position * 1000, speed)
            setActions(actions)
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
        is PlayerEvent.PausedForCache -> old.copy(pausedForCache = value)
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
        is PlayerEvent.PlaybackRestart -> old.copy(mediaStarted = true)
        is PlayerEvent.StartFile -> old.copy(mediaStarted = true, path = path)
        is PlayerEvent.EndFile -> old.copy(paused = true, mediaStarted = false)
        is PlayerEvent.Playlist -> old.copy(playlistId = playlistId, playlist = newPlaylist)

        else -> old
    }

    private fun PlayerEvent.updateMediaSession(newState: PlayerState) {
        when (this) {
            is PlayerEvent.Shuffle -> mediaSession.setShuffleMode(
                if (shuffle) PlaybackStateCompat.SHUFFLE_MODE_ALL
                else PlaybackStateCompat.SHUFFLE_MODE_NONE
            )

            is PlayerEvent.Loop -> mediaSession.setRepeatMode(
                when (mode) {
                    LoopMode.LoopPlaylist -> PlaybackStateCompat.REPEAT_MODE_ALL
                    LoopMode.LoopFile -> PlaybackStateCompat.REPEAT_MODE_ONE
                    LoopMode.None -> PlaybackStateCompat.REPEAT_MODE_NONE
                }
            )

            is PlayerEvent.Paused,
            is PlayerEvent.EndFile,
            is PlayerEvent.Speed,
            is PlayerEvent.Position,
            is PlayerEvent.Duration,
            is PlayerEvent.PausedForCache -> {
                mediaSession.setPlaybackState(newState.buildPlaybackState())
                scope.launch { mediaSession.setMetadata(newState.buildMediaMetadata()) }
            }

            is PlayerEvent.Idling,
            is PlayerEvent.MediaTitle,
            is PlayerEvent.Artist,
            is PlayerEvent.Album,
            is PlayerEvent.MediaThumbnail,
            is PlayerEvent.StartFile -> {
                scope.launch { mediaSession.setMetadata(newState.buildMediaMetadata()) }
            }

            is PlayerEvent.PlaylistPosition -> Unit
            else -> Unit
        }
    }

    companion object {
        private const val TAG = "MediaSessionManager"
    }
}