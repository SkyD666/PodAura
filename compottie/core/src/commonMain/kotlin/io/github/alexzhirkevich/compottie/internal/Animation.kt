package io.github.alexzhirkevich.compottie.internal

import androidx.compose.ui.util.fastForEach
import io.github.alexzhirkevich.compottie.internal.animation.ExpressionHolder
import io.github.alexzhirkevich.compottie.internal.assets.CharacterData
import io.github.alexzhirkevich.compottie.internal.assets.FontList
import io.github.alexzhirkevich.compottie.internal.assets.LottieAsset
import io.github.alexzhirkevich.compottie.internal.assets.PrecompositionAsset
import io.github.alexzhirkevich.compottie.internal.helpers.Marker
import io.github.alexzhirkevich.compottie.internal.layers.Layer
import io.github.alexzhirkevich.compottie.internal.layers.TextLayer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject


@Serializable
internal class Animation(
    @SerialName("fr")
    val frameRate: Float,

    @SerialName("w")
    val width: Float,

    @SerialName("h")
    val height: Float,

    @SerialName("ip")
    val inPoint: Float,

    @SerialName("op")
    val outPoint: Float,

    @SerialName("nm")
    val name: String? = null,

    val layers: List<Layer> = emptyList(),

    val assets: List<LottieAsset> = emptyList(),

    val fonts: FontList? = null,

    val chars: List<CharacterData> = emptyList(),

    val markers: List<Marker> = emptyList(),

    @SerialName("slots")
    private val slotsMap: Map<String, JsonElement>? = null,
) : ExpressionHolder {

    @Transient
    val layersMap = layers.associateBy { it.name }

    @Transient
    val slots = AnimationSlots(
        slots = slotsMap?.mapValues {
            checkNotNull(it.value.jsonObject["p"]) {
                "Invalid slottable property: ${it.value}"
            }
        }.orEmpty()
    )

    override fun prepareExpressions(state: AnimationState) {
        layers.fastForEach { it.prepareExpressions(state) }
    }

    fun deepCopy(): Animation {
        return Animation(
            frameRate = frameRate,
            width = width,
            height = height,
            inPoint = inPoint,
            outPoint = outPoint,
            name = name,
            layers = layers.map(Layer::deepCopy),
            assets = assets.map(LottieAsset::copy),
            fonts = fonts?.deepCopy(),
            chars = chars.map(CharacterData::deepCopy),
            markers = markers,
            slotsMap = slotsMap
        )
    }
}

private fun List<Layer>.hasTextLayers(): Boolean {
    return any { it is TextLayer }
}

internal fun Animation.hasTextLayers(): Boolean {
    return layers.hasTextLayers() || assets.any { it is PrecompositionAsset && it.layers.hasTextLayers() }
}
