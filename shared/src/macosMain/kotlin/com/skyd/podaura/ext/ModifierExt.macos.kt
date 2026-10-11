@file:Suppress("INVISIBLE_REFERENCE")

package com.skyd.podaura.ext

import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.onClick
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.MacosCursor
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerHoverIcon
import platform.AppKit.NSCursor
import platform.AppKit.NSImage
import platform.Foundation.NSMakePoint
import platform.Foundation.NSMakeSize

actual fun Modifier.onRightClickIfSupported(
    interactionSource: MutableInteractionSource?,
    enabled: Boolean,
    pass: PointerEventPass,
    onClick: () -> Unit
): Modifier {
    val matcher = PointerMatcher.pointer(PointerType.Mouse, button = PointerButton.Secondary)
    return if (pass == PointerEventPass.Main) {
        onClick(
            enabled = enabled,
            interactionSource = interactionSource,
            matcher = matcher,
            onClick = onClick,
        )
    } else {
        onRightClickInPass(interactionSource, enabled, pass, matcher::matches, onClick)
    }
}

private val blankCursor: PointerIcon by lazy {
    MacosCursor(NSCursor(NSImage(size = NSMakeSize(1.0, 1.0)), NSMakePoint(0.0, 0.0)))
}

// Keep the hover node attached so a stationary pointer updates with controller visibility.
actual fun Modifier.hideCursorIfSupported(hide: Boolean): Modifier =
    pointerHoverIcon(if (hide) blankCursor else PointerIcon.Default, overrideDescendants = hide)
