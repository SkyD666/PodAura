package com.skyd.podaura.ui.player.pip

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.content.ContextCompat
import androidx.core.graphics.toRect
import androidx.core.util.Consumer
import co.touchlab.kermit.Logger
import com.skyd.podaura.ext.activity
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.ui.player.component.state.PlayStateCallback

@Composable
/*internal*/ fun PipListenerPreAPI12(shouldEnterPipMode: Boolean) {
    val currentShouldEnterPipMode by rememberUpdatedState(newValue = shouldEnterPipMode)
    if (Build.VERSION.SDK_INT in Build.VERSION_CODES.O..<Build.VERSION_CODES.S) {
        val context = LocalContext.current
        DisposableEffect(context) {
            val activity = context.activity as ComponentActivity
            val onUserLeaveBehavior: () -> Unit = {
                if (currentShouldEnterPipMode) {
                    val builder = PictureInPictureParams.Builder()
                    activity.enterPictureInPictureMode(builder.build())
                }
            }
            activity.addOnUserLeaveHintListener(onUserLeaveBehavior)
            onDispose { activity.removeOnUserLeaveHintListener(onUserLeaveBehavior) }
        }
    } else {
        Logger.i("PIP_TAG") { "API does not support PiP" }
    }
}

@Composable
/*internal*/ fun Modifier.pipParams(
    context: Context,
    autoEnterPipMode: Boolean,
    isVideo: Boolean,
    playState: PlayState,
): Modifier = run {
    var builder by remember { mutableStateOf<PictureInPictureParams.Builder?>(null) }
    val currentPlayState by rememberUpdatedState(playState)
    val currentAutoEnterPipMode by rememberUpdatedState(autoEnterPipMode)
    val setActionsAndApplyBuilder: (PictureInPictureParams.Builder) -> Unit = remember {
        { builder ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setActions(
                    listOfRemoteActions(
                        playState = currentPlayState,
                        context = context,
                    ),
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    builder.setAutoEnterEnabled(currentAutoEnterPipMode)
                    if (!isVideo) {
                        builder.setSeamlessResizeEnabled(false)
                    }
                }
                context.activity.setPictureInPictureParams(builder.build())
            }
        }
    }

    LaunchedEffect(playState.isPlaying) {
        builder?.let { setActionsAndApplyBuilder(it) }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        onGloballyPositioned { layoutCoordinates ->
            (builder ?: PictureInPictureParams.Builder()).let { b ->
                builder = b
                val rect = layoutCoordinates.boundsInWindow()
                b.setSourceRectHint(rect.toAndroidRectF().toRect())
                if (!rect.isEmpty && rect.width / rect.height in 0.42..<2.4) {
                    b.setAspectRatio(Rational(rect.width.toInt(), rect.height.toInt()))
                }
                setActionsAndApplyBuilder(b)
            }
        }
    } else this
}

@Composable
/*internal*/ fun rememberIsInPipMode(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val activity = LocalContext.current.activity as ComponentActivity
        var pipMode by remember { mutableStateOf(activity.isInPictureInPictureMode) }
        DisposableEffect(activity) {
            val observer = Consumer<PictureInPictureModeChangedInfo> { info ->
                pipMode = info.isInPictureInPictureMode
            }
            activity.addOnPictureInPictureModeChangedListener(observer)
            onDispose { activity.removeOnPictureInPictureModeChangedListener(observer) }
        }
        return pipMode
    } else {
        return false
    }
}

@Composable
fun PipBroadcastReceiver(playStateCallback: PlayStateCallback) {
    if (rememberIsInPipMode()) {
        val context = LocalContext.current
        DisposableEffect(context) {
            val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if ((intent == null) || (intent.action != ACTION_BROADCAST_CONTROL)) {
                        return
                    }

                    when (intent.getIntExtra(EXTRA_CONTROL_TYPE, 0)) {
                        EXTRA_CONTROL_PAUSE, EXTRA_CONTROL_PLAY ->
                            playStateCallback.onPlayStateChanged()
                    }
                }
            }
            ContextCompat.registerReceiver(
                context,
                broadcastReceiver,
                IntentFilter(ACTION_BROADCAST_CONTROL),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            onDispose {
                context.unregisterReceiver(broadcastReceiver)
            }
        }
    }
}

internal fun Activity.manualEnterPictureInPictureMode() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        enterPictureInPictureMode(PictureInPictureParams.Builder().build())
    }
}