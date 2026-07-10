package io.github.alexzhirkevich.compottie.internal.helpers

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
internal value class MatteMode(val mode: Byte) {
    companion object {
        val None = MatteMode(0)
        val Add = MatteMode(1)
        val Invert = MatteMode(2)
        val Luma = MatteMode(3)
        val InvertedLuma = MatteMode(4)
    }
}

internal fun MatteMode.isInvert(): Boolean = this == MatteMode.Invert || this == MatteMode.InvertedLuma
internal fun MatteMode.isLuma(): Boolean = this == MatteMode.Luma || this == MatteMode.InvertedLuma

internal fun MatteMode.isSupported() =
    this == MatteMode.Add
            || this == MatteMode.Invert
//    || this == MatteMode.Luma
//    || this == MatteMode.InvertedLuma