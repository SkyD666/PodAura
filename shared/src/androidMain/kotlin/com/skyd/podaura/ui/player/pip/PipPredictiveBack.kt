package com.skyd.podaura.ui.player.pip

import android.graphics.Color
import android.os.Build
import androidx.activity.BackEventCompat
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toDrawable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

@Composable
internal fun Modifier.pipPredictiveBack(
    enabled: Boolean,
    inPipMode: Boolean,
    onBack: () -> Unit,
): Modifier {
    // A new state on each PiP transition prevents a shrunken player on restore.
    val gesture = remember(inPipMode) { PipBackGestureState() }
    val progress = animateFloatAsState(
        targetValue = gesture.progress,
        animationSpec = if (gesture.inProgress) snap() else tween(durationMillis = 200),
        label = "PiP back progress",
    )
    val motionScale = rememberCoroutineScope().coroutineContext[MotionDurationScale]
    val activity = LocalActivity.current
    val revealBehind = !inPipMode && (gesture.inProgress || progress.value > 0f)
    DisposableEffect(activity, revealBehind) {
        if (activity == null || !revealBehind || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return@DisposableEffect onDispose { }
        }
        // Scaling Compose content alone leaves the opaque window covering the previous Activity.
        // Keep it visible through cancellation's settle animation, then release its surface again.
        val background = activity.window.decorView.background
        activity.setTranslucent(true)
        activity.window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        onDispose {
            activity.window.setBackgroundDrawable(background)
            activity.setTranslucent(false)
        }
    }
    PredictiveBackHandler(enabled = enabled) { events ->
        gesture.collect(events)
        // Keep the preview until the system takes over; cancellation never enters PiP.
        onBack()
    }
    return this.graphicsLayer {
        val fraction = if (inPipMode || motionScale?.scaleFactor == 0f) 0f
        else PipBackEasing.transform(progress.value)
        scaleX = 1f - 0.1f * fraction
        scaleY = scaleX
        translationX = 8.dp.toPx() * fraction *
                if (gesture.swipeEdge == BackEventCompat.EDGE_LEFT) 1f else -1f
    }
}

internal class PipBackGestureState {
    var progress by mutableFloatStateOf(0f)
        private set
    var inProgress by mutableStateOf(false)
        private set
    var swipeEdge by mutableIntStateOf(BackEventCompat.EDGE_LEFT)
        private set

    suspend fun collect(events: Flow<BackEventCompat>) {
        progress = 0f
        inProgress = true
        try {
            events.collect { event ->
                progress = event.progress
                swipeEdge = event.swipeEdge
            }
        } catch (cancelled: CancellationException) {
            inProgress = false
            progress = 0f
            throw cancelled
        }
    }
}

private val PipBackEasing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)
