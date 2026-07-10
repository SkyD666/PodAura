package io.github.alexzhirkevich.compottie.internal.helpers

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
internal value class TrimPathType(
    val type: Byte
) {

    companion object {
        val Simultaneously = TrimPathType(1)

        val Individually = TrimPathType(2)
    }
}