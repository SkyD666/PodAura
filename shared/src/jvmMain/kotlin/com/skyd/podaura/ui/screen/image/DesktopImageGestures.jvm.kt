package com.skyd.podaura.ui.screen.image

import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.skyd.fundation.util.Platform
import com.skyd.fundation.util.platform
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import kotlin.math.sqrt

internal actual fun PointerEvent.desktopPanDelta(
    bounds: IntSize,
    density: Density,
): Offset {
    val delta = changes.fold(Offset.Zero) { total, change -> total + change.scrollDelta }
    val scrollAmount = (awtEventOrNull as? MouseWheelEvent)?.scrollAmount?.toFloat() ?: 1f
    val fallback = with(density) { 40.dp.toPx() }

    return when (platform) {
        Platform.macOS_Jvm -> delta * with(density) { -10.dp.toPx() * scrollAmount }
        Platform.Windows -> Offset(
            x = -delta.x * (bounds.width / 20f).takeIf { it > 0f }
                .orFallback(fallback) * scrollAmount,
            y = -delta.y * (bounds.height / 20f).takeIf { it > 0f }
                .orFallback(fallback) * scrollAmount,
        )

        Platform.Linux -> Offset(
            x = -delta.x * sqrt(bounds.width.toFloat()).takeIf { it > 0f }
                .orFallback(fallback) * scrollAmount,
            y = -delta.y * sqrt(bounds.height.toFloat()).takeIf { it > 0f }
                .orFallback(fallback) * scrollAmount,
        )

        Platform.Android, Platform.iOS, Platform.macOS_Native -> Offset.Zero
    }
}

internal actual fun PointerEvent.isDesktopPrimaryDoubleClick(): Boolean {
    val event = awtEventOrNull ?: return false
    return event.isPrimaryDoubleClick()
}

internal fun MouseEvent.isPrimaryDoubleClick(): Boolean =
    id == MouseEvent.MOUSE_RELEASED &&
            button == MouseEvent.BUTTON1 &&
            clickCount == 2

private fun Float?.orFallback(fallback: Float): Float = this ?: fallback
