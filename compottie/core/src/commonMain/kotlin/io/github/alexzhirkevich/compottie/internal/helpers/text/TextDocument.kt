package io.github.alexzhirkevich.compottie.internal.helpers.text

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class TextDocument(

    @SerialName("f")
    var fontFamily: String? = null,

    @SerialName("fc")
    var fillColor: FloatArray? = null,

    @SerialName("sc")
    var strokeColor: FloatArray? = null,

    @SerialName("sw")
    var strokeWidth: Float = 0f,

    @SerialName("of")
    var strokeOverFill: Boolean = false,

    @SerialName("s")
    var fontSize: Float = 10f,

    @SerialName("lh")
    var lineHeight: Float = fontSize,

    @SerialName("sz")
    var wrapSize: FloatArray? = null,

    @SerialName("ps")
    var wrapPosition: FloatArray? = null,

    @SerialName("t")
    var text: String? = null,

    @SerialName("j")
    var textJustify: TextJustify = TextJustify.Left,

    @SerialName("ca")
    var textCaps: TextCaps = TextCaps.Regular,

    @SerialName("tr")
    var textTracking: Float? = null,

    @SerialName("ls")
    var baselineShift: Float? = null,
)

internal val TextDocument.fontScale get() = fontSize / 100f
