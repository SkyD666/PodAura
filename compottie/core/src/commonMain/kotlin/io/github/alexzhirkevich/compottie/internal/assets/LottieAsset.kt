package io.github.alexzhirkevich.compottie.internal.assets

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

@Serializable(with = AssetSerializer::class)
public sealed interface LottieAsset {

    public val id: String

    public fun copy(): LottieAsset

    @Serializable
    public class Unsupported() : LottieAsset {
        override val id: String get() = ""

        override fun copy(): Unsupported = Unsupported()
    }
}


internal class AssetSerializer : JsonContentPolymorphicSerializer<LottieAsset>(LottieAsset::class) {

    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<LottieAsset> {
        return when {
            "layers" in element.jsonObject.keys -> PrecompositionAsset.serializer()
            "p" in element.jsonObject.keys -> ImageAsset.serializer()
            else -> LottieAsset.Unsupported.serializer()
        }
    }
}

