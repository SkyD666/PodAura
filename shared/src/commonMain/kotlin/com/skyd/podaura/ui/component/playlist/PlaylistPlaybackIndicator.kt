package com.skyd.podaura.ui.component.playlist

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.skyd.podaura.ui.component.rememberSystemAnimationsEnabled
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.player_current_media
import podaura.shared.generated.resources.playing

private val restingHeights = listOf(0.4f, 0.85f, 0.6f)

@Composable
internal fun PlaylistPlaybackIndicator(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
) {
    val heights = remember { restingHeights.map { Animatable(it) } }
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val visible = animate && lifecycleState.isAtLeast(Lifecycle.State.STARTED)
    val motionEnabled = rememberSystemAnimationsEnabled(observe = visible && isPlaying)
    val canAnimate = visible && motionEnabled

    LaunchedEffect(isPlaying, canAnimate) {
        heights.forEachIndexed { index, height ->
            launch {
                when {
                    !canAnimate -> height.snapTo(restingHeights[index])
                    !isPlaying -> height.animateTo(restingHeights[index], tween(150))
                    else -> {
                        // Different starting directions and periods keep the bars out of sync.
                        var rising = index != 1
                        while (isActive) {
                            height.animateTo(if (rising) 0.95f else 0.25f, tween(540 + index * 84))
                            rising = !rising
                        }
                    }
                }
            }
        }
    }

    val description = stringResource(
        if (isPlaying) Res.string.playing else Res.string.player_current_media,
    )
    Canvas(modifier.size(32.dp).semantics { contentDescription = description }) {
        val barWidth = size.width / 8f
        val bottom = size.height * 0.8f
        heights.forEachIndexed { index, height ->
            val barHeight = size.height * 0.6f * height.value
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(size.width * (0.1875f + index * 0.25f), bottom - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}
