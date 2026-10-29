package com.skyd.podaura.ui.player.port

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

/** Keeps the media mounted while adapting between stacked and side-by-side playback. */
@Composable
internal fun PlaybackPane(
    modifier: Modifier = Modifier,
    sideBySide: Boolean = false,
    media: @Composable () -> Unit,
    controls: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier) {
        val viewportHeight = constraints.maxHeight
        val paneScrollState = rememberScrollState()
        val controlsScrollState = rememberScrollState()
        Layout(
            modifier = if (sideBySide) Modifier else Modifier.verticalScroll(paneScrollState),
            content = {
                Box(contentAlignment = Alignment.Center) { media() }
                Box(
                    modifier = if (sideBySide) Modifier.verticalScroll(controlsScrollState) else Modifier,
                    contentAlignment = Alignment.BottomStart,
                ) { controls() }
            },
        ) { measurables, constraints ->
            if (sideBySide) {
                // Reserve enough width for the playback buttons at the 600 dp breakpoint.
                val controlsWidth = maxOf((constraints.maxWidth * 0.6f).toInt(), 380.dp.roundToPx())
                val mediaWidth = constraints.maxWidth - controlsWidth
                val mediaPlaceable =
                    measurables[0].measure(Constraints.fixed(mediaWidth, viewportHeight))
                val controlsPlaceable =
                    measurables[1].measure(Constraints.fixed(controlsWidth, viewportHeight))
                return@Layout layout(constraints.maxWidth, viewportHeight) {
                    mediaPlaceable.placeRelative(0, 0)
                    controlsPlaceable.placeRelative(mediaWidth, 0)
                }
            }
            val controlsPlaceable = measurables[1].measure(
                constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity),
            )
            // Keep a 96 dp cover plus MediaArea's vertical padding; scroll this pane if needed.
            val mediaHeight =
                (viewportHeight - controlsPlaceable.height).coerceAtLeast(108.dp.roundToPx())
            val mediaPlaceable = measurables[0].measure(
                Constraints.fixed(constraints.maxWidth, mediaHeight),
            )
            layout(constraints.maxWidth, mediaHeight + controlsPlaceable.height) {
                mediaPlaceable.placeRelative(0, 0)
                controlsPlaceable.placeRelative(0, mediaHeight)
            }
        }
    }
}
