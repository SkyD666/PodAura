package com.skyd.podaura.ext

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import kotlin.math.roundToInt


fun Modifier.mirror(): Modifier = scale(scaleX = -1f, scaleY = 1f)

fun Modifier.aspectRatioIn(
    ratio: Float,
    matchHeightConstraintsFirst: Boolean = false,
    minWidth: Dp? = null,
    maxWidth: Dp? = null,
    minHeight: Dp? = null,
    maxHeight: Dp? = null
) = layout { measurable, constraints ->
    val resolvedMinW =
        (minWidth?.toPx() ?: constraints.minWidth.toFloat()).coerceAtLeast(0f)
    val resolvedMaxW =
        (maxWidth?.toPx() ?: constraints.maxWidth.toFloat()).coerceAtLeast(resolvedMinW)
    val resolvedMinH =
        (minHeight?.toPx() ?: constraints.minHeight.toFloat()).coerceAtLeast(0f)
    val resolvedMaxH =
        (maxHeight?.toPx() ?: constraints.maxHeight.toFloat()).coerceAtLeast(resolvedMinH)

    val mergedConstraints = constraints.copy(
        minWidth = resolvedMinW.roundToInt(),
        maxWidth = resolvedMaxW.roundToInt(),
        minHeight = resolvedMinH.roundToInt(),
        maxHeight = resolvedMaxH.roundToInt(),
    )

    val (width, height) = if (matchHeightConstraintsFirst) {
        val height =
            mergedConstraints.constrainHeight((mergedConstraints.maxWidth / ratio).roundToInt())
        val width = (height * ratio).roundToInt()
        val constrainedWidth = mergedConstraints.constrainWidth(width)
        val finalHeight = (constrainedWidth / ratio).roundToInt()
        constrainedWidth to mergedConstraints.constrainHeight(finalHeight)
    } else {
        val width =
            mergedConstraints.constrainWidth((mergedConstraints.maxHeight * ratio).roundToInt())
        val height = (width / ratio).roundToInt()
        val constrainedHeight = mergedConstraints.constrainHeight(height)
        val finalWidth = (constrainedHeight * ratio).roundToInt()
        mergedConstraints.constrainWidth(finalWidth) to constrainedHeight
    }

    val placeable = measurable.measure(Constraints.fixed(width, height))
    layout(width, height) { placeable.place(0, 0) }
}

/** Use [PointerEventPass.Initial] to handle right-clicks before a component's own click handler. */
expect fun Modifier.onRightClickIfSupported(
    interactionSource: MutableInteractionSource? = null,
    enabled: Boolean = true,
    pass: PointerEventPass = PointerEventPass.Main,
    onClick: () -> Unit
): Modifier

internal fun Modifier.onRightClickInPass(
    interactionSource: MutableInteractionSource?,
    enabled: Boolean,
    pass: PointerEventPass,
    matches: (PointerEvent) -> Boolean,
    onClick: () -> Unit,
): Modifier = if (!enabled) this else composed {
    val currentMatches by rememberUpdatedState(matches)
    val currentOnClick by rememberUpdatedState(onClick)
    pointerInput(interactionSource, pass) {
        awaitEachGesture {
            var event: PointerEvent
            do {
                event = awaitPointerEvent(pass)
            } while (event.type != PointerEventType.Press || !currentMatches(event) ||
                event.changes.any { it.isConsumed }
            )
            event.changes.forEach { it.consume() }
            val press = PressInteraction.Press(event.changes.first().position)
            interactionSource?.tryEmit(press)
            var up: PointerInputChange? = null
            try {
                while (true) {
                    event = awaitPointerEvent(pass)
                    if (event.changes.any {
                            it.isConsumed || it.isOutOfBounds(size, extendedTouchPadding)
                        }) break
                    if (event.type == PointerEventType.Release) {
                        if (currentMatches(event)) up = event.changes.first()
                        break
                    }
                    // An additional button press cancels this gesture.
                    if (event.type == PointerEventType.Press) break
                    if (awaitPointerEvent(PointerEventPass.Final).changes.any { it.isConsumed }) break
                }
                up?.consume()
            } finally {
                interactionSource?.tryEmit(
                    if (up == null) PressInteraction.Cancel(press) else PressInteraction.Release(
                        press
                    )
                )
            }
            if (up != null) currentOnClick()
        }
    }
}

expect fun Modifier.hideCursorIfSupported(hide: Boolean): Modifier
