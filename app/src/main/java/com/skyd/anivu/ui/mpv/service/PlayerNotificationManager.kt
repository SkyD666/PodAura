package com.skyd.anivu.ui.mpv.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.DrawableRes
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import com.skyd.anivu.R
import com.skyd.anivu.ui.activity.player.PlayActivity

class PlayerNotificationManager(
    private val context: Context,
    private val sessionManager: MediaSessionManager,
) {
    private val playerState get() = sessionManager.playerState

    val notificationBuilder: NotificationCompat.Builder
        get() {
            val openActivityPendingIntent = PendingIntentCompat.getActivity(
                context,
                0,
                Intent(context, PlayActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT,
                false
            )
            val style = androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(sessionManager.mediaSession.sessionToken)
            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_icon_24)
                .setStyle(style)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(openActivityPendingIntent)
                .setOngoing(true)
                .setContentTitle()
                .setContentText()
                .setThumbnail()
                .addAllAction(style)
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

    fun update() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun NotificationCompat.Builder.addAllAction(
        style: androidx.media.app.NotificationCompat.MediaStyle,
    ) = apply {
        val playPendingIntent =
            if (sessionManager.state != PlaybackStateCompat.STATE_PLAYING) buildNotificationAction(
                icon = R.drawable.ic_play_arrow_24,
                title = context.getString(R.string.play),
                intentAction = PlayerService.PLAY_ACTION
            ) else buildNotificationAction(
                icon = R.drawable.ic_pause_24,
                title = context.getString(R.string.pause),
                intentAction = PlayerService.PLAY_ACTION
            )
        val closePendingIntent = buildNotificationAction(
            icon = R.drawable.ic_close_24,
            title = context.getString(R.string.close),
            intentAction = PlayerService.CLOSE_ACTION
        )
        addAction(playPendingIntent)
        addAction(closePendingIntent)

        style.setShowActionsInCompactView(0, 1)
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            if (notificationManager.getNotificationChannel(CHANNEL_ID) != null) {
                return
            }
            val name = context.getString(R.string.player_notification_channel_name)
            val descriptionText =
                context.getString(R.string.player_notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun cancel() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun NotificationCompat.Builder.setContentTitle() = apply {
        setContentTitle(playerState.value.run {
            customMediaData?.title.orEmpty().ifBlank { mediaTitle }
        })
    }

    private fun NotificationCompat.Builder.setContentText() = apply {
        with(playerState.value) {
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

    private fun NotificationCompat.Builder.setThumbnail() = apply {
        playerState.value.run { customMediaData?.thumbnail ?: mediaThumbnail }?.also {
            setLargeIcon(it)
            setColorized(true)
            // scale thumbnail to a single color in two steps
            val b1 = Bitmap.createScaledBitmap(it, 16, 16, true)
            val b2 = Bitmap.createScaledBitmap(b1, 1, 1, true)
            setColor(b2.getPixel(0, 0))
            b2.recycle()
            b1.recycle()
        }
    }

    companion object {
        const val CHANNEL_ID = "PlayerChannel"
        const val NOTIFICATION_ID = 0x0d000721
    }
}