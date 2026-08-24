package com.skyd.podaura.ui.screen.image

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import platform.AppKit.NSEvent

internal actual fun PointerEvent.desktopPanDelta(
    bounds: IntSize,
    density: Density,
): Offset {
    val nativeEvent = nativeEvent as? NSEvent
        ?: return -changes.fold(Offset.Zero) { total, change -> total + change.scrollDelta }
    val multiplier = if (nativeEvent.hasPreciseScrollingDeltas) {
        density.density
    } else {
        with(density) { 10.dp.toPx() }
    }
    return Offset(
        x = -nativeEvent.scrollingDeltaX.toFloat() * multiplier,
        y = -nativeEvent.scrollingDeltaY.toFloat() * multiplier,
    )
}

internal actual fun PointerEvent.isDesktopPrimaryDoubleClick(): Boolean {
    val event = nativeEvent as? NSEvent ?: return false
    return event.buttonNumber.toInt() == 0 && event.clickCount.toInt() == 2
}
