package io.github.alexzhirkevich.compottie.internal.assets

import io.github.alexzhirkevich.compottie.internal.layers.Layer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class PrecompositionAsset(
    override val id: String,
    @SerialName("nm")
    val name: String? = null,
    val layers: List<Layer>
) : LottieAsset {
    override fun copy(): LottieAsset {
        return this
    }
}
