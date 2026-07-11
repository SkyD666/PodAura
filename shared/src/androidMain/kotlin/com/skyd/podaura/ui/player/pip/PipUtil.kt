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
import androidx.activity.compose.LocalActivity
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.content.ContextCompat
import androidx.core.graphics.toRect
import androidx.core.util.Consumer
import co.touchlab.kermit.Logger
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.ui.player.component.state.PlayStateCallback

@Composable
/*internal*/ fun PipListenerPreAPI12(shouldEnterPipMode: Boolean) {
    val currentShouldEnterPipMode by rememberUpdatedState(newValue = shouldEnterPipMode)
    // API 31+ enters PiP automatically via setAutoEnterEnabled, so this listener is only needed
    // on O..R. Below O there is no PiP at all -- the old code logged "unsupported" for API 31+
    // too, which was misleading.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S
    ) {
        // Safe cast: LocalActivity is null in previews and non-activity hosts.
        val activity = LocalActivity.current as? ComponentActivity
        DisposableEffect(activity) {
            if (activity == null) return@DisposableEffect onDispose { }
            val onUserLeaveBehavior: () -> Unit = {
                if (currentShouldEnterPipMode) {
                    activity.enterPictureInPictureMode(PictureInPictureParams.Builder().build())
                }
            }
            activity.addOnUserLeaveHintListener(onUserLeaveBehavior)
            onDispose { activity.removeOnUserLeaveHintListener(onUserLeaveBehavior) }
        }
    } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        Logger.i(tag = "PIP_TAG") { "API does not support PiP" }
    }
}

// Android rejects PiP aspect ratios outside (1/2.39, 2.39); stay just inside those bounds.
private const val MIN_PIP_ASPECT_RATIO = 1 / 2.39f
private const val MAX_PIP_ASPECT_RATIO = 2.39f

private class BoundsHolder {
    var value: Rect? = null
}

@Composable
/*internal*/ fun Modifier.pipParams(
    autoEnterPipMode: Boolean,
    isVideo: Boolean,
    playState: PlayState,
): Modifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    pipParamsApi26(
        autoEnterPipMode = autoEnterPipMode,
        isVideo = isVideo,
        playState = playState,
    )
} else this

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun Modifier.pipParamsApi26(
    autoEnterPipMode: Boolean,
    isVideo: Boolean,
    playState: PlayState,
): Modifier {
    val context = LocalContext.current
    val activity = LocalActivity.current

    val builder = remember { PictureInPictureParams.Builder() }
    // Plain holder, not snapshot state: it is written during the layout pass, and writing state
    // there would schedule another recomposition/layout on every frame.
    val lastBounds = remember { BoundsHolder() }
    val currentPlayState by rememberUpdatedState(playState)
    val currentAutoEnterPipMode by rememberUpdatedState(autoEnterPipMode)
    val currentIsVideo by rememberUpdatedState(isVideo)

    val applyBuilder: () -> Unit = remember {
        {
            builder.setActions(
                listOfRemoteActions(playState = currentPlayState, context = context),
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.setAutoEnterEnabled(currentAutoEnterPipMode)
                builder.setSeamlessResizeEnabled(currentIsVideo)
            }
            runCatching { activity?.setPictureInPictureParams(builder.build()) }
                .onFailure { e ->
                    Logger.w(throwable = e, tag = "PIP_TAG") { "setPictureInPictureParams failed" }
                }
        }
    }

    LaunchedEffect(playState.isPlaying, autoEnterPipMode, isVideo) { applyBuilder() }

    return onGloballyPositioned { layoutCoordinates ->
        val rect = layoutCoordinates.boundsInWindow()
        // setPictureInPictureParams is a binder call and onGloballyPositioned fires on every
        // layout pass, so skip it unless the bounds actually moved.
        if (rect == lastBounds.value) return@onGloballyPositioned
        lastBounds.value = rect

        builder.setSourceRectHint(rect.toAndroidRectF().toRect())
        val width = rect.width.toInt()
        val height = rect.height.toInt()
        // Rational(_, 0) throws, and an out-of-range ratio makes setAspectRatio throw too.
        if (width > 0 && height > 0 &&
            width.toFloat() / height in MIN_PIP_ASPECT_RATIO..MAX_PIP_ASPECT_RATIO
        ) {
            builder.setAspectRatio(Rational(width, height))
        }
        applyBuilder()
    }
}

@Composable
/*internal*/ fun rememberIsInPipMode(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
    val activity = LocalActivity.current as? ComponentActivity ?: return false
    var pipMode by remember(activity) { mutableStateOf(activity.isInPictureInPictureMode) }
    DisposableEffect(activity) {
        val observer = Consumer<PictureInPictureModeChangedInfo> { info ->
            pipMode = info.isInPictureInPictureMode
        }
        activity.addOnPictureInPictureModeChangedListener(observer)
        onDispose { activity.removeOnPictureInPictureModeChangedListener(observer) }
    }
    return pipMode
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
