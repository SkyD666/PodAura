package com.skyd.podaura.ui.player.port

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor

@Composable
internal fun PlayerSkeleton(
    modifier: Modifier,
    shape: Shape = RectangleShape,
    animated: Boolean = true,
) {
    Box(modifier = modifier.background(playerSkeletonBrush(animated), shape))
}

@Composable
private fun playerSkeletonBrush(animated: Boolean): Brush {
    val base = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    val motionDurationScale = rememberCoroutineScope().coroutineContext[MotionDurationScale]
    if (!animated || motionDurationScale?.scaleFactor == 0f) {
        return SolidColor(base)
    }

    val transition = rememberInfiniteTransition(label = "playerSkeletonShimmer")
    val progress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_200, easing = LinearEasing),
        ),
        label = "playerSkeletonShimmerProgress",
    ).value
    val startX = -1_000f + progress * 2_000f
    return Brush.linearGradient(
        colorStops = arrayOf(
            0f to base,
            0.45f to base,
            0.5f to MaterialTheme.colorScheme.outline.copy(alpha = 0.34f),
            0.55f to base,
            1f to base,
        ),
        start = Offset(startX, 0f),
        end = Offset(startX + 600f, 600f),
    )
}
