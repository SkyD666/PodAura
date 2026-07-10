package io.github.alexzhirkevich.compottie.internal.helpers

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class Marker(
    @SerialName("cm")
    val name: String? = null,

    @SerialName("tm")
    val startFrame: Float,

    @SerialName("dr")
    val durationFrames: Float
)