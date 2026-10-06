package com.skyd.podaura.ext

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass

actual fun Modifier.onRightClickIfSupported(
    interactionSource: MutableInteractionSource?,
    enabled: Boolean,
    pass: PointerEventPass,
    onClick: () -> Unit
): Modifier = this

actual fun Modifier.hideCursorIfSupported(hide: Boolean): Modifier = this
