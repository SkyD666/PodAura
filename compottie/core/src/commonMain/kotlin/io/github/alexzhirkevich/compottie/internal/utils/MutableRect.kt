package io.github.alexzhirkevich.compottie.internal.utils

import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Rect
import kotlin.math.max
import kotlin.math.min

internal fun MutableRect.intersectOrReset(other: MutableRect) {
    if (overlaps(other)) {
        intersect(other)
    } else {
        set(0f, 0f, 0f, 0f)
    }
}

internal fun MutableRect.intersect(other: MutableRect) =
    intersect(
        left = other.left,
        top = other.top,
        right = other.right,
        bottom = other.bottom
    )

internal fun MutableRect.union(other: MutableRect) {

    if ((other.left >= other.right) || (other.top >= other.bottom)) {
        return
    }

    if ((left < right) && (top < bottom)) {
        left = min(left, other.left)
        top = min(top, other.top)
        right = max(right, other.right)
        bottom = max(bottom, other.bottom)
    } else {
        set(other)
    }
}

internal fun MutableRect.union(other: Rect) {

    if ((other.left >= other.right) || (other.top >= other.bottom)) {
        return
    }

    if ((left < right) && (top < bottom)) {
        left = min(left, other.left)
        top = min(top, other.top)
        right = max(right, other.right)
        bottom = max(bottom, other.bottom)
    } else {
        set(other)
    }
}

internal fun MutableRect.set(other: MutableRect) {
    set(left = other.left, top = other.top, right = other.right, bottom = other.bottom)
}

internal fun MutableRect.set(other: Rect) {
    set(left = other.left, top = other.top, right = other.right, bottom = other.bottom)
}

internal fun MutableRect.extendBy(value: Float) {
    set(
        left = left - value,
        top = top - value,
        right = right + value,
        bottom = bottom + value
    )
}
