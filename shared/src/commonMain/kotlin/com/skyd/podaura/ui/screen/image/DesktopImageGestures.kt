package com.skyd.podaura.ui.screen.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.isAltGraphPressed
import androidx.compose.ui.input.pointer.isAltPressed
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isFunctionPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.isSymPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach
import com.github.panpf.zoomimage.compose.zoom.ZoomableState
import com.skyd.fundation.util.Platform
import com.skyd.fundation.util.isPhone
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal val LocalDesktopMagnificationFactors = staticCompositionLocalOf<Flow<Float>> {
    emptyFlow()
}

internal enum class DesktopScrollAction {
    Pan,
    HorizontalPan,
    Zoom,
    Ignore,
}

internal fun resolveDesktopScrollAction(
    platform: Platform,
    ctrlPressed: Boolean = false,
    metaPressed: Boolean = false,
    shiftPressed: Boolean = false,
    hasOtherModifier: Boolean = false,
): DesktopScrollAction {
    if (platform.isPhone) return DesktopScrollAction.Ignore

    val usesMetaForZoom = platform == Platform.macOS_Jvm || platform == Platform.macOS_Native
    val zoomModifierPressed = if (usesMetaForZoom) metaPressed else ctrlPressed
    val wrongZoomModifierPressed = if (usesMetaForZoom) ctrlPressed else metaPressed

    return when {
        hasOtherModifier || wrongZoomModifierPressed || zoomModifierPressed && shiftPressed ->
            DesktopScrollAction.Ignore

        zoomModifierPressed -> DesktopScrollAction.Zoom
        shiftPressed -> DesktopScrollAction.HorizontalPan
        else -> DesktopScrollAction.Pan
    }
}

internal fun horizontalPanDelta(delta: Offset): Offset {
    val horizontal = if (delta.x != 0f) delta.x else delta.y
    return Offset(horizontal, 0f)
}

internal class DesktopImageGestureState(
    private val zoomable: ZoomableState,
) {
    private var pointerPosition: Offset? = null

    fun updatePointerPosition(position: Offset) {
        pointerPosition = position
    }

    suspend fun panBy(delta: Offset) {
        if (delta != Offset.Zero) zoomable.offsetBy(delta)
    }

    suspend fun zoomByWheel(scrollDelta: Float, centroid: Offset? = null) {
        if (!scrollDelta.isFinite() || scrollDelta == 0f) return
        val adjustedDelta = if (zoomable.reverseMouseWheelScale) -scrollDelta else scrollDelta
        val targetScale = zoomable.mouseWheelScaleCalculator.calculateScale(
            currentScale = zoomable.transform.scaleX,
            scrollDelta = adjustedDelta,
        )
        if (!targetScale.isFinite()) return
        zoomable.scale(
            targetScale = targetScale,
            centroidContentPointF = contentPointAt(centroid),
        )
    }

    suspend fun zoomByFactor(scaleFactor: Float, centroid: Offset? = null) {
        if (!scaleFactor.isFinite() || scaleFactor <= 0f || scaleFactor == 1f) return
        zoomable.scaleBy(
            addScale = scaleFactor,
            centroidContentPointF = contentPointAt(centroid),
        )
    }

    suspend fun switchScale(centroid: Offset) {
        zoomable.switchScale(
            centroidContentPointF = zoomable.touchPointToContentPointF(centroid),
            animated = true,
        )
    }

    private fun contentPointAt(centroid: Offset?) =
        zoomable.touchPointToContentPointF(centroid ?: currentPointerOrCenter())

    private fun currentPointerOrCenter(): Offset = pointerPosition ?: Offset(
        x = zoomable.containerSize.width / 2f,
        y = zoomable.containerSize.height / 2f,
    )
}

internal fun Modifier.desktopImageGestures(
    state: DesktopImageGestureState,
    platform: Platform,
): Modifier {
    if (platform.isPhone) return this

    return pointerInput(state, platform) {
        val eventDensity: Density = this
        kotlinx.coroutines.coroutineScope {
            val gestureMutex = Mutex()

            fun handleGesture(transform: suspend () -> Unit): Boolean {
                launch(start = CoroutineStart.UNDISPATCHED) {
                    gestureMutex.withLock { transform() }
                }
                return true
            }

            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull() ?: continue
                    state.updatePointerPosition(change.position)

                    val handled = when (event.type) {
                        PointerEventType.Scroll -> {
                            val action = event.keyboardModifiers.desktopScrollAction(platform)
                            when (action) {
                                DesktopScrollAction.Pan,
                                DesktopScrollAction.HorizontalPan -> {
                                    val panDelta = event.desktopPanDelta(size, eventDensity)
                                    val delta = if (action == DesktopScrollAction.HorizontalPan) {
                                        horizontalPanDelta(panDelta)
                                    } else {
                                        panDelta
                                    }
                                    handleGesture { state.panBy(delta) }
                                }

                                DesktopScrollAction.Zoom -> {
                                    val scrollDelta = event.desktopZoomDelta()
                                    val centroid = change.position
                                    handleGesture {
                                        state.zoomByWheel(
                                            scrollDelta = scrollDelta,
                                            centroid = centroid,
                                        )
                                    }
                                }

                                DesktopScrollAction.Ignore -> false
                            }
                        }

                        PointerEventType.PanMove -> {
                            val delta = event.trackpadPanDelta()
                            handleGesture { state.panBy(delta) }
                        }

                        PointerEventType.ScaleChange -> {
                            val delta = event.trackpadPanDelta()
                            val scaleFactor = event.trackpadScaleFactor()
                            val centroid = change.position
                            handleGesture {
                                state.panBy(delta)
                                state.zoomByFactor(
                                    scaleFactor = scaleFactor,
                                    centroid = centroid,
                                )
                            }
                        }

                        PointerEventType.PanStart,
                        PointerEventType.ScaleStart,
                        PointerEventType.PanEnd,
                        PointerEventType.ScaleEnd -> true

                        PointerEventType.Release -> {
                            if (event.isDesktopPrimaryDoubleClick()) {
                                handleGesture { state.switchScale(change.position) }
                            } else {
                                false
                            }
                        }

                        else -> false
                    }

                    if (handled) {
                        event.changes.fastForEach { it.consume() }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DesktopMagnificationEffect(state: DesktopImageGestureState) {
    val scaleFactors = LocalDesktopMagnificationFactors.current
    LaunchedEffect(state, scaleFactors) {
        scaleFactors.collect { scaleFactor ->
            state.zoomByFactor(scaleFactor)
        }
    }
}

private fun PointerKeyboardModifiers.desktopScrollAction(platform: Platform) =
    resolveDesktopScrollAction(
        platform = platform,
        ctrlPressed = isCtrlPressed,
        metaPressed = isMetaPressed,
        shiftPressed = isShiftPressed,
        hasOtherModifier = isAltPressed || isAltGraphPressed ||
                isFunctionPressed || isSymPressed,
)

private fun PointerEvent.trackpadPanDelta(): Offset = -changes.fold(Offset.Zero) { total, change ->
    val historical = change.historical.fold(Offset.Zero) { subtotal, item ->
        subtotal + item.panOffset
    }
    total + historical + change.panOffset
}

private fun PointerEvent.trackpadScaleFactor(): Float = changes.fold(1f) { total, change ->
    val historical = change.historical.fold(1f) { subtotal, item ->
        subtotal * item.scaleFactor
    }
    total * historical * change.scaleFactor
}

private fun PointerEvent.desktopZoomDelta(): Float =
    changes.fold(0f) { total, change -> total + change.scrollDelta.y }

internal expect fun PointerEvent.desktopPanDelta(
    bounds: IntSize,
    density: Density,
): Offset

internal expect fun PointerEvent.isDesktopPrimaryDoubleClick(): Boolean
