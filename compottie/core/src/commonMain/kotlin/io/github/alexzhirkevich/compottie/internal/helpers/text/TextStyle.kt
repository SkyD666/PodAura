package io.github.alexzhirkevich.compottie.internal.helpers.text

import io.github.alexzhirkevich.compottie.internal.animation.AnimatedColor
import io.github.alexzhirkevich.compottie.internal.animation.AnimatedNumber
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class TextStyle(

    @SerialName("sw")
    val strokeWidth: AnimatedNumber? = null,

    @SerialName("sc")
    val strokeColor: AnimatedColor? = null,

    @SerialName("sh")
    val strokeHue: AnimatedNumber? = null,

    @SerialName("ss")
    val strokeSaturation: AnimatedNumber? = null,

    @SerialName("sb")
    val strokeBrightness: AnimatedNumber? = null,

    @SerialName("o")
    val strokeOpacity: AnimatedNumber? = null,

    @SerialName("fc")
    val fillColor: AnimatedColor? = null,

    @SerialName("fh")
    val fillHue: AnimatedNumber? = null,

    @SerialName("fs")
    val fillSaturation: AnimatedNumber? = null,

    @SerialName("fb")
    val fillBrightness: AnimatedNumber? = null,

    @SerialName("fo")
    val fillOpacity: AnimatedNumber? = null,

    @SerialName("t")
    val letterSpacing: AnimatedNumber? = null,

    @SerialName("ls")
    val lineSpacing: AnimatedNumber? = null,

    @SerialName("bl")
    val blur: AnimatedNumber? = null,
) {
    fun copy() = TextStyle(
        strokeWidth = strokeWidth?.copy(),
        strokeColor = strokeColor?.copy(),
        strokeHue = strokeHue?.copy(),
        strokeSaturation = strokeSaturation?.copy(),
        strokeBrightness = strokeBrightness?.copy(),
        strokeOpacity = strokeOpacity?.copy(),
        fillColor = fillColor?.copy(),
        fillHue = fillHue?.copy(),
        fillSaturation = fillSaturation?.copy(),
        fillBrightness = fillBrightness?.copy(),
        fillOpacity = fillOpacity?.copy(),
        letterSpacing = letterSpacing?.copy(),
        lineSpacing = lineSpacing?.copy(),
        blur = blur?.copy()
    )
}