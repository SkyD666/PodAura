package com.skyd.podaura.ext

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.roundToInt

inline fun Modifier.thenIf(condition: Boolean, block: Modifier.() -> Modifier): Modifier {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (condition) block() else this
}

inline fun <T> Modifier.thenIfNotNull(obj: T?, block: Modifier.(T) -> Modifier): Modifier {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (obj != null) block(obj) else this
}

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