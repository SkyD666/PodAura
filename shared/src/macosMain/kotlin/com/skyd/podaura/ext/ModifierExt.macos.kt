package com.skyd.podaura.ext

import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.onClick
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType

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

// The native ComposeWindow's setPointerIcon coerces any non-MacosCursor icon
// to NSCursor.arrowCursor, so hiding the cursor is not supported here.
actual fun Modifier.hideCursorIfSupported(hide: Boolean): Modifier = this
