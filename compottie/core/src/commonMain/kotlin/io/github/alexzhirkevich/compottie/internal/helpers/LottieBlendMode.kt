package io.github.alexzhirkevich.compottie.internal.helpers

import androidx.compose.ui.graphics.BlendMode
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
internal value class LottieBlendMode(val type: Byte) {
    companion object {
        val Normal = LottieBlendMode(0)
        val Multiply = LottieBlendMode(1)
        val Screen = LottieBlendMode(2)
        val Overlay = LottieBlendMode(3)
        val Darken = LottieBlendMode(4)
        val Lighten = LottieBlendMode(5)
        val ColorDodge = LottieBlendMode(6)
        val ColorBurn = LottieBlendMode(7)
        val HardLight = LottieBlendMode(8)
        val SoftLight = LottieBlendMode(9)
        val Difference = LottieBlendMode(10)
        val Exclusion = LottieBlendMode(11)
        val Hue = LottieBlendMode(12)
        val Saturation = LottieBlendMode(13)
        val Color = LottieBlendMode(14)
        val Luminosity = LottieBlendMode(15)

        val Add = LottieBlendMode(16)
        val Mix = LottieBlendMode(17)
    }
}

internal fun LottieBlendMode.asComposeBlendMode(): BlendMode {
    return when (this) {
        LottieBlendMode.Normal -> BlendMode.SrcOver
        LottieBlendMode.Multiply -> BlendMode.Multiply
        LottieBlendMode.Screen -> BlendMode.Screen
        LottieBlendMode.Overlay -> BlendMode.Overlay
        LottieBlendMode.Darken -> BlendMode.Darken
        LottieBlendMode.Lighten -> BlendMode.Lighten
        LottieBlendMode.ColorDodge -> BlendMode.ColorDodge
        LottieBlendMode.ColorBurn -> BlendMode.ColorBurn
        LottieBlendMode.HardLight -> BlendMode.Hardlight
        LottieBlendMode.SoftLight -> BlendMode.Softlight
        LottieBlendMode.Difference -> BlendMode.Difference
        LottieBlendMode.Exclusion -> BlendMode.Exclusion
        LottieBlendMode.Hue -> BlendMode.Hue
        LottieBlendMode.Saturation -> BlendMode.Saturation
        LottieBlendMode.Color -> BlendMode.Color
        LottieBlendMode.Luminosity -> BlendMode.Luminosity
        else -> BlendMode.SrcOver
    }
}
