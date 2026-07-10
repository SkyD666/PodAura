package io.github.alexzhirkevich.compottie.internal.helpers.text

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class TextRange(

    @SerialName("nm")
    val name: String? = null,

//    @SerialName("s")
//    val selector: TextRangeSelector? = null,

    @SerialName("a")
    val style: TextStyle? = null
) {
    fun copy() = TextRange(
        name = name,
        style = style?.copy()
    )
}