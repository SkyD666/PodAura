package io.github.alexzhirkevich.compottie.internal.platform.effects

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.skiaPaint
import androidx.compose.ui.graphics.toArgb
import io.github.alexzhirkevich.compottie.internal.platform.BlurSigmaScale
import org.jetbrains.skia.ImageFilter

internal actual class PlatformDropShadowEffect(val filter: ImageFilter)

internal actual fun makeNativeDropShadowEffect(
    radius: Float,
    x: Float,
    y: Float,
    color: Color
): PlatformDropShadowEffect {
    val sigma = radius * BlurSigmaScale
    return PlatformDropShadowEffect(
        ImageFilter.makeDropShadow(
            dx = x,
            dy = y,
            sigmaX = sigma,
            sigmaY = sigma,
            color = color.toArgb()
        )
    )
}

internal actual fun Paint.applyNativeDropShadowEffect(
    effect: PlatformDropShadowEffect,
) {
    val fp = skiaPaint

    if (fp.imageFilter == null) {
        fp.imageFilter = effect.filter
    } else {
        fp.imageFilter = ImageFilter.makeCompose(effect.filter, fp.imageFilter)
    }
}
