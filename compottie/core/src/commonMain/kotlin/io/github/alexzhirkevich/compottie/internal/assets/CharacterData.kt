package io.github.alexzhirkevich.compottie.internal.assets

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class CharacterData(

    @SerialName("ch")
    val character: String,

    @SerialName("fFamily")
    val fontFamily: String,

    @SerialName("size")
    val fontSize: Float = 10f,

    @SerialName("style")
    val fontStyle: String? = null,

    @SerialName("w")
    val width: Float? = null,

    @SerialName("data")
    val data: CharacterPath? = null
) {
    fun deepCopy(): CharacterData {
        return CharacterData(
            character = character,
            fontFamily = fontFamily,
            fontSize = fontSize,
            fontStyle = fontStyle,
            width = width,
            data = data?.copy()
        )
    }
}