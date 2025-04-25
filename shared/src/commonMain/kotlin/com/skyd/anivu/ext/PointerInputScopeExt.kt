package com.skyd.anivu.ext

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlin.math.PI
import kotlin.math.abs


suspend fun PointerInputScope.detectDoubleFingerTransformGestures(
    onVerticalDragStart: (Offset) -> Unit = { },
    onVerticalDragEnd: () -> Unit = { },
    onVerticalDragCancel: () -> Unit = { },
    onVerticalDrag: (change: PointerInputChange, dragAmount: Float) -> Unit,
    onHorizontalDragStart: (Offset) -> Unit = { },
    onHorizontalDragEnd: () -> Unit = { },
    onHorizontalDragCancel: () -> Unit = { },
    onHorizontalDrag: (change: PointerInputChange, dragAmount: Float) -> Unit,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit,
) = awaitEachGesture {
    var rotation = 0f
    var zoom = 1f
    var pan = Offset.Zero
    var singlePan = Offset.Zero
    var pastTouchSlop = false
    val touchSlop = viewConfiguration.touchSlop
    var lockedToPanZoom = false

    var horizontalDrag = false
    var verticalDrag = false
    var transformDrag = false

    val firstDown = awaitFirstDown(requireUnconsumed = false)
    var canceled: Boolean

    do {
        val event = awaitPointerEvent()
        canceled = event.changes.fastAny { it.isConsumed }
        if (canceled) continue
        val count: Int = if (event.changes.size > 2) {
            event.changes.takeIf { it.last().id != it.first().id }?.size ?: 1
        } else event.changes.size

        val zoomChange = event.calculateZoom()
        val panChange = event.calculatePan()
        val rotationChange = event.calculateRotation()
        if (!pastTouchSlop) {
            if (count == 1) {
                singlePan += panChange
                val singlePanMotion = singlePan.getDistance()
                if (singlePanMotion > touchSlop) {
                    pastTouchSlop = true
                    if (abs(singlePan.x) > abs(singlePan.y)) {
                        horizontalDrag = true
                        onHorizontalDragStart(firstDown.position)
                    } else {
                        verticalDrag = true
                        onVerticalDragStart(firstDown.position)
                    }
                }
            } else if (count > 1) {
                zoom *= zoomChange
                rotation += rotationChange
                pan += panChange

                val centroidSize = event.calculateCentroidSize(useCurrent = false)
                val zoomMotion = abs(1 - zoom) * centroidSize
                val rotationMotion = abs(rotation * PI.toFloat() * centroidSize / 180f)
                val panMotion = pan.getDistance()

                if (zoomMotion > touchSlop || rotationMotion > touchSlop || panMotion > touchSlop) {
                    transformDrag = true
                    pastTouchSlop = true
                    lockedToPanZoom = rotationMotion < touchSlop
                }
            }
        }
        if (pastTouchSlop) {
            if (horizontalDrag) {
                onHorizontalDrag(event.changes.first(), panChange.x)
            } else if (verticalDrag) {
                onVerticalDrag(event.changes.first(), panChange.y)
            } else if (transformDrag) {
                val centroid = event.calculateCentroid(useCurrent = false)
                val effectiveRotation = if (lockedToPanZoom) 0f else rotationChange
                if (effectiveRotation != 0f ||
                    zoomChange != 1f ||
                    panChange != Offset.Zero
                ) {
                    onGesture(centroid, panChange, zoomChange, effectiveRotation)
                }
            }
            event.changes.fastForEach {
                if (it.positionChanged()) {
                    it.consume()
                }
            }
        }
    } while (!canceled && event.changes.fastAny { it.pressed })
    if (horizontalDrag) {
        if (canceled) onHorizontalDragCancel() else onHorizontalDragEnd()
    } else if (verticalDrag) {
        if (canceled) onVerticalDragCancel() else onVerticalDragEnd()
    }
}