package io.github.alexzhirkevich.compottie.internal.helpers.text

import io.github.alexzhirkevich.compottie.internal.animation.AnimatedNumber
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class TextRangeSelector(

//    @SerialName("t")
//    val expressible : BooleanInt,
//
//    @SerialName("xe")
//    val maxEase : AnimatedNumber? = null,
//
//    @SerialName("ne")
//    val minEase : AnimatedNumber? = null,
//
//    @SerialName("a")
//    val maxAmount : AnimatedNumber? = null,
//
//    @SerialName("b")
//    val basedOn : TextBased,
//
//    @SerialName("rn")
//    val randomize : BooleanInt = BooleanInt.No,

    @SerialName("sm")
    val smoothness: AnimatedNumber? = null,

    @SerialName("sh")
    val shape: TextShape,

    @SerialName("o")
    val offset: AnimatedNumber? = null,

    @SerialName("r")
    val rangeUnits: TextRangeUnits? = null,

    @SerialName("s")
    val start: AnimatedNumber? = null,

    @SerialName("e")
    val end: AnimatedNumber? = null,
)