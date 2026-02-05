package com.skyd.podaura.ext

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier

actual fun Modifier.onRightClickIfSupported(
    interactionSource: MutableInteractionSource?,
    enabled: Boolean,
    onClick: () -> Unit
): Modifier = this
