package com.skyd.podaura.ui.player.pip

import android.app.PendingIntent
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import com.skyd.podaura.ext.getString
import com.skyd.podaura.shared.R
import com.skyd.podaura.ui.player.component.state.PlayState
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.pause
import podaura.shared.generated.resources.play


// Constant for broadcast receiver
const val ACTION_BROADCAST_CONTROL = "broadcast_control"

// Intent extras for broadcast controls from Picture-in-Picture mode.
const val EXTRA_CONTROL_TYPE = "control_type"
const val EXTRA_CONTROL_PLAY = 1
const val EXTRA_CONTROL_PAUSE = 2
const val REQUEST_PLAY = 5
const val REQUEST_PAUSE = 6

@RequiresApi(Build.VERSION_CODES.O)
fun listOfRemoteActions(
    playState: PlayState,
    context: Context,
): List<RemoteAction> {
    return listOf(
        if (playState.isPlaying) {
            buildRemoteAction(
                R.drawable.ic_pause_24,
                context.getString(Res.string.pause),
                REQUEST_PAUSE,
                EXTRA_CONTROL_PAUSE,
                context = context,
            )
        } else {
            buildRemoteAction(
                R.drawable.ic_play_arrow_24,
                context.getString(Res.string.play),
                REQUEST_PLAY,
                EXTRA_CONTROL_PLAY,
                context = context,
            )
        },
    )
}

@RequiresApi(Build.VERSION_CODES.O)
private fun buildRemoteAction(
    @DrawableRes iconResId: Int,
    title: String,
    requestCode: Int,
    controlType: Int,
    context: Context,
): RemoteAction {
    return RemoteAction(
        Icon.createWithResource(context, iconResId),
        title,
        title,
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(ACTION_BROADCAST_CONTROL)
                .setPackage(context.packageName)
                .putExtra(EXTRA_CONTROL_TYPE, controlType),
            PendingIntent.FLAG_IMMUTABLE,
        ),
    )
}
