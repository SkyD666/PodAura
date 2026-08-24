package com.skyd.podaura.ui.screen.image

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize

internal actual fun PointerEvent.desktopPanDelta(
    bounds: IntSize,
    density: Density,
): Offset = Offset.Zero

internal actual fun PointerEvent.isDesktopPrimaryDoubleClick(): Boolean = false
