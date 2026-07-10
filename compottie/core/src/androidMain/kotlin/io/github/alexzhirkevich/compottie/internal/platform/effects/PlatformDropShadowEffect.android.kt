package io.github.alexzhirkevich.compottie.internal.platform.effects

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.nativePaint
import androidx.compose.ui.graphics.toArgb
import io.github.alexzhirkevich.compottie.internal.platform.BlurSigmaScale

internal actual class PlatformDropShadowEffect(
    val radius: Float,
    val x: Float,
    val y: Float,
    val color: Color
)

internal actual fun makeNativeDropShadowEffect(
    radius: Float,
    x: Float,
    y: Float,
    color: Color
): PlatformDropShadowEffect = PlatformDropShadowEffect(radius * BlurSigmaScale, x, y, color)

internal actual fun Paint.applyNativeDropShadowEffect(
    effect: PlatformDropShadowEffect,
) {
    nativePaint.setShadowLayer(effect.radius, effect.x, effect.y, effect.color.toArgb())
}
