package io.github.alexzhirkevich.compottie.internal.helpers.text

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
internal value class TextShape(val type: Byte) {
    companion object {
        val Square = TextShape(1)
        val RampUp = TextShape(2)
        val RampDown = TextShape(3)
        val Triangle = TextShape(4)
        val Round = TextShape(5)
        val Smooth = TextShape(6)
    }
}