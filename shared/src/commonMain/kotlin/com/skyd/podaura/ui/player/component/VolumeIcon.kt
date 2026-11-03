package com.skyd.podaura.ui.player.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeMute
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.ui.graphics.vector.ImageVector

internal fun volumeIcon(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    muted: Boolean = false,
): ImageVector = when {
    muted -> Icons.AutoMirrored.Rounded.VolumeOff
    value <= range.start -> Icons.AutoMirrored.Rounded.VolumeMute
    value <= range.start + (range.endInclusive - range.start) / 2 -> Icons.AutoMirrored.Rounded.VolumeDown
    else -> Icons.AutoMirrored.Rounded.VolumeUp
}
