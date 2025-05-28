package com.skyd.podaura.ui.player.service

import android.annotation.SuppressLint
import android.app.ForegroundServiceStartNotAllowedException
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.app.ServiceCompat
import androidx.core.graphics.get
import androidx.core.graphics.scale
import coil3.Bitmap
import com.skyd.podaura.R
import com.skyd.podaura.ext.getString
import com.skyd.podaura.ext.notify
import com.skyd.podaura.ui.activity.player.PlayActivity
import com.skyd.podaura.ui.player.LoopMode
import com.skyd.podaura.ui.player.PlayerEvent
import com.skyd.podaura.ui.player.createThumbnail
import com.skyd.podaura.ui.player.isPlaying
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.close
import podaura.shared.generated.resources.loop_playlist_mode
import podaura.shared.generated.resources.pause
import podaura.shared.generated.resources.play
import podaura.shared.generated.resources.player_notification_channel_description
import podaura.shared.generated.resources.player_notification_channel_name
import podaura.shared.generated.resources.skip_next
import podaura.shared.generated.resources.skip_previous

class PlayerNotificationManager(
    private val context: Context,
    mediaSession: MediaSessionCompat,
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var notificationCreated = false

    init {
        createNotificationChannel()
    }

    private val style = androidx.media.app.NotificationCompat.MediaStyle()
        .setMediaSession(mediaSession.sessionToken)

    private val baseNotificationBuilder = run {
        val openActivityPendingIntent = PendingIntentCompat.getActivity(
            context,
            0,
            Intent(context, PlayActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT,
            false
        )
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_icon_24)
            .setStyle(style)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openActivityPendingIntent)
            .setOngoing(true)
    }

    // https://developer.android.com/develop/ui/views/notifications/notification-permission#exemptions-media-sessions
    @SuppressLint("MissingPermission")
    private fun android.app.Notification.myNotify() {
        notify(context, NOTIFICATION_ID)
    }

    private var fetchThumbnailJob: Job? = null
    private val updateThumbnail = { state: PlayerState ->
        fetchThumbnailJob?.cancel()
        fetchThumbnailJob = scope.launch(Dispatchers.IO) {
            getThumbnail(state)?.let { thumbnail ->
                baseNotificationBuilder.setThumbnail(thumbnail).build().myNotify()
            }
        }
    }

    private fun fullUpdatedNotificationWithoutThumbnail(playerState: PlayerState) =
        baseNotificationBuilder
            .setContentTitle(playerState)
            .setContentText(playerState)
            .addAllAction(style, playerState)
            .build()

    private fun fullUpdateNotification(playerState: PlayerState) {
        fullUpdatedNotificationWithoutThumbnail(playerState).myNotify()
        fetchThumbnailJob?.cancel()
        fetchThumbnailJob = scope.launch { updateThumbnail(playerState) }
    }

    private fun partialUpdateNotification(event: PlayerEvent, state: PlayerState) {
        when (event) {
            is PlayerEvent.MediaTitle -> baseNotificationBuilder.setContentTitle(state)
            is PlayerEvent.StartFile -> {
                updateThumbnail(state)
                baseNotificationBuilder.setContentTitle(state)
            }

            is PlayerEvent.Album,
            is PlayerEvent.Artist -> baseNotificationBuilder.setContentText(state)

            is PlayerEvent.Idling,
            is PlayerEvent.Paused,
            is PlayerEvent.Loading,
            is PlayerEvent.Duration,
            is PlayerEvent.Position,
            is PlayerEvent.Loop,
            is PlayerEvent.Playlist,
            is PlayerEvent.PlaylistPosition -> baseNotificationBuilder.addAllAction(style, state)

            is PlayerEvent.MediaThumbnail -> {
                updateThumbnail(state)
                return
            }

            else -> return
        }
        baseNotificationBuilder.build().myNotify()
    }

    private fun buildNotificationAction(
        @DrawableRes icon: Int,
        title: CharSequence,
        intentAction: String,
    ): NotificationCompat.Action {
        val intent = PlayerService.createIntent(context, intentAction)
        val builder = NotificationCompat.Action.Builder(icon, title, intent)
        with(builder) {
            setContextual(false)
            setShowsUserInterface(false)
            return build()
        }
    }

    fun notifyNotification() = baseNotificationBuilder.build().myNotify()

    fun update(event: PlayerEvent, state: PlayerState) {
        if (notificationCreated) {
            partialUpdateNotification(event, state)
        }
    }

    internal fun startForegroundService(service: PlayerService, playerState: PlayerState) =
        scope.launch {
            try {
                ServiceCompat.startForeground(
                    service,
                    NOTIFICATION_ID,
                    fullUpdatedNotificationWithoutThumbnail(playerState),
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    } else {
                        0
                    },
                )
                notificationCreated = true
                fullUpdateNotification(playerState)
            } catch (e: Exception) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    e is ForegroundServiceStartNotAllowedException
                ) {
                    // App not in a valid state to start foreground service (e.g. started from bg)
                    e.printStackTrace()
                }
            }
        }

    private fun NotificationCompat.Builder.addAllAction(
        style: androidx.media.app.NotificationCompat.MediaStyle,
        playerState: PlayerState,
    ) = apply {
        clearActions()
        var actionCount = 0
        val actionsInCompactView = mutableListOf<Int>()
        val myAddAction: (NotificationCompat.Action, Boolean) -> Unit = { action, inCompact ->
            addAction(action)
            if (inCompact) actionsInCompactView += actionCount
            actionCount++
        }

        val previousPendingIntent = buildNotificationAction(
            icon = R.drawable.ic_skip_previous_24,
            title = context.getString(Res.string.skip_previous),
            intentAction = PlayerService.PREVIOUS_ACTION
        )
        val nextPendingIntent = buildNotificationAction(
            icon = R.drawable.ic_skip_next_24,
            title = context.getString(Res.string.skip_next),
            intentAction = PlayerService.NEXT_ACTION
        )
        val playPendingIntent = if (playerState.isPlaying) {
            buildNotificationAction(
                icon = R.drawable.ic_play_arrow_24,
                title = context.getString(Res.string.play),
                intentAction = PlayerService.PLAY_ACTION
            )
        } else {
            buildNotificationAction(
                icon = R.drawable.ic_pause_24,
                title = context.getString(Res.string.pause),
                intentAction = PlayerService.PLAY_ACTION
            )
        }
        val loopPendingIntent = buildNotificationAction(
            icon = when (playerState.loop) {
                LoopMode.LoopPlaylist -> R.drawable.ic_repeat_on_24
                LoopMode.LoopFile -> R.drawable.ic_repeat_one_on_24
                LoopMode.None -> R.drawable.ic_repeat_24
            },
            title = context.getString(Res.string.loop_playlist_mode),
            intentAction = PlayerService.LOOP_ACTION
        )
        val closePendingIntent = buildNotificationAction(
            icon = R.drawable.ic_close_24,
            title = context.getString(Res.string.close),
            intentAction = PlayerService.CLOSE_ACTION
        )
        if (!playerState.playlistFirst) {
            myAddAction(previousPendingIntent, true)
        }
        myAddAction(playPendingIntent, true)
        if (!playerState.playlistLast) {
            myAddAction(nextPendingIntent, true)
        }
        myAddAction(loopPendingIntent, false)
        myAddAction(closePendingIntent, true)

        style.setShowActionsInCompactView(
            *actionsInCompactView.subList(
                0, minOf(3, actionsInCompactView.size)
            ).toIntArray()
        )
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            if (notificationManager.getNotificationChannel(CHANNEL_ID) != null) {
                return
            }
            val name = context.getString(Res.string.player_notification_channel_name)
            val descriptionText =
                context.getString(Res.string.player_notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun cancel() {
        notificationCreated = false
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun NotificationCompat.Builder.setContentTitle(playerState: PlayerState) = apply {
        setContentTitle(playerState.run {
            currentMedia?.title.orEmpty().ifBlank { mediaTitle }
        })
    }

    private fun NotificationCompat.Builder.setContentText(playerState: PlayerState) = apply {
        with(playerState) {
            val artistEmpty = artist.isNullOrEmpty()
            val albumEmpty = album.isNullOrEmpty()
            setContentText(
                when {
                    !artistEmpty && !albumEmpty -> "$artist / $album"
                    !artistEmpty -> album
                    !albumEmpty -> artist
                    else -> null
                }
            )
        }
    }

    private var lastThumbnail: Any? = null
    private suspend fun getThumbnail(playerState: PlayerState): Bitmap? = playerState.run {
        val thumbnailAny = currentMedia?.thumbnailAny
        if (thumbnailAny is String) {
            lastThumbnail = thumbnailAny
            withContext(Dispatchers.IO) { createThumbnail(thumbnailAny) }
                ?: mediaThumbnail?.asAndroidBitmap()
        } else {
            lastThumbnail = thumbnailAny
            mediaThumbnail?.asAndroidBitmap()
        }
    }

    private fun NotificationCompat.Builder.setThumbnail(thumbnail: Bitmap) = apply {
        setLargeIcon(thumbnail)
        setColorized(true)
        // scale thumbnail to a single color in two steps
        val b1 = thumbnail.scale(16, 16)
        val b2 = b1.scale(1, 1)
        setColor(b2[0, 0])
        b2.recycle()
        b1.recycle()
    }

    companion object {
        const val CHANNEL_ID = "PlayerChannel"
        const val NOTIFICATION_ID = 0x0d000721
    }
}