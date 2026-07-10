package io.github.alexzhirkevich.compottie.internal.helpers

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
internal value class GradientType(val type: Byte) {
    companion object {
        val Linear = GradientType(1)
        val Radial = GradientType(2)
        val Conic = GradientType(3)
    }
}