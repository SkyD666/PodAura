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

/** Measures the controls first, then gives the media the remaining viewport height. */
@Composable
internal fun PlaybackPane(
    modifier: Modifier = Modifier,
    media: @Composable () -> Unit,
    controls: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier) {
        val viewportHeight = constraints.maxHeight
        Layout(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            content = {
                Box(contentAlignment = Alignment.Center) { media() }
                Box { controls() }
            },
        ) { measurables, constraints ->
            val controlsPlaceable = measurables[1].measure(
                constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity),
            )
            // Keep a 96 dp cover plus MediaArea's vertical padding; scroll this pane if needed.
            val mediaHeight = (viewportHeight - controlsPlaceable.height).coerceAtLeast(108.dp.roundToPx())
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
