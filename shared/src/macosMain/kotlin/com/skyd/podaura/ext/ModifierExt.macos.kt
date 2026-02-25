package com.skyd.podaura.ext

import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.onClick
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerType

actual fun Modifier.onRightClickIfSupported(
    interactionSource: MutableInteractionSource?,
    enabled: Boolean,
    onClick: () -> Unit
): Modifier = if (interactionSource == null) {
    onClick(
        enabled = enabled,
        matcher = PointerMatcher.pointer(PointerType.Mouse, button = PointerButton.Secondary),
        onClick = onClick,
    )
} else {
    onClick(
        enabled = enabled,
        interactionSource = interactionSource,
        matcher = PointerMatcher.pointer(PointerType.Mouse, button = PointerButton.Secondary),
        onClick = onClick,
    )
}
