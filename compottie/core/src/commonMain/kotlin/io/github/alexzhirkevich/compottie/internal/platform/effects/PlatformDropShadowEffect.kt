package io.github.alexzhirkevich.compottie.internal.platform.effects

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint

internal expect class PlatformDropShadowEffect

internal expect fun makeNativeDropShadowEffect(
    radius: Float,
    x: Float,
    y: Float,
    color: Color
): PlatformDropShadowEffect


internal expect fun Paint.applyNativeDropShadowEffect(
    effect: PlatformDropShadowEffect,
)