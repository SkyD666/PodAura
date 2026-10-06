package com.skyd.podaura.ext

import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.onClick
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerHoverIcon
import java.awt.Point
import java.awt.Toolkit
import java.awt.image.BufferedImage

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
    PointerIcon(
        Toolkit.getDefaultToolkit().createCustomCursor(
            BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), Point(0, 0), "blankCursor",
        )
    )
}

// The pointerHoverIcon node must stay attached with a swapped icon value: a node attached
// while the mouse is already stationary inside it does not apply its icon until the next
// pointer event, so the cursor would not disappear together with the controller.
actual fun Modifier.hideCursorIfSupported(hide: Boolean): Modifier =
    pointerHoverIcon(if (hide) blankCursor else PointerIcon.Default, overrideDescendants = hide)
