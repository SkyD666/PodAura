package com.skyd.podaura.ui.player.service

import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.compose.ui.graphics.asAndroidBitmap
import com.skyd.podaura.R
import com.skyd.podaura.ext.getString
import com.skyd.podaura.ext.toUri
import com.skyd.podaura.ui.player.LoopMode
import com.skyd.podaura.ui.player.PlayerEvent
import com.skyd.podaura.ui.player.coordinator.PlayerModel
import com.skyd.podaura.ui.player.createThumbnailFile
import com.skyd.podaura.ui.player.playbackState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.close
import podaura.shared.generated.resources.loop_playlist_mode

class MediaSessionManager(
    private val context: Context,
    private val playerModel: PlayerModel,
    private val callback: MediaSessionCompat.Callback,
) {
    val mediaSession: MediaSessionCompat = initMediaSession()
    private val notificationManager = PlayerNotificationManager(context, mediaSession)
    private val mediaMetadataBuilder = MediaMetadataCompat.Builder()

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    init {
        scope.launch {
            playerModel.newStateByEvent.collect { (newState, event) ->
                event.updateMediaSession(newState)
            }
        }
    }

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
        notificationManager.startForegroundService(service, playerModel.playerState.value)
    }

    fun onDestroy() {
        mediaSession.isActive = false
        mediaSession.release()
        notificationManager.cancel()
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
                            putBitmap(
                                MediaMetadataCompat.METADATA_KEY_ART,
                                newState.mediaThumbnail?.asAndroidBitmap(),
                            )
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