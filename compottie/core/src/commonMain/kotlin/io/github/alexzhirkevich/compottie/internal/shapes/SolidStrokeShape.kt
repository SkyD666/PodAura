package io.github.alexzhirkevich.compottie.internal.shapes

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.DrawScope
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.animation.AnimatedColor
import io.github.alexzhirkevich.compottie.internal.animation.AnimatedNumber
import io.github.alexzhirkevich.compottie.internal.animation.defaultOpacity
import io.github.alexzhirkevich.compottie.internal.helpers.StrokeDash
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("st")
internal class SolidStrokeShape(

    @SerialName("mn")
    override val matchName: String? = null,

    @SerialName("nm")
    override val name: String? = null,

    @SerialName("hd")
    override val hidden: Boolean = false,

    @SerialName("lc")
    override val lineCap: LineCap = LineCap.Round,

    @SerialName("lj")
    override val lineJoin: LineJoin = LineJoin.Round,

    @SerialName("ml")
    override val strokeMiter: Float = 0f,

    @SerialName("o")
    override val opacity: AnimatedNumber = AnimatedNumber.defaultOpacity(),

    @SerialName("w")
    override val strokeWidth: AnimatedNumber,

    @SerialName("d")
    override val strokeDash: List<StrokeDash>? = null,

    @SerialName("c")
    val color: AnimatedColor,
) : BaseStrokeShape(), Shape {

    override fun draw(drawScope: DrawScope, parentMatrix: Matrix, parentAlpha: Float, state: AnimationState) {
        paint.color = Color(color.interpolatedColor(state))

        super.draw(drawScope, parentMatrix, parentAlpha, state)
    }

    override fun deepCopy(): Shape {
        return SolidStrokeShape(
            matchName = matchName,
            name = name,
            hidden = hidden,
            lineCap = lineCap,
            lineJoin = lineJoin,
            strokeMiter = strokeMiter,
            opacity = opacity.copy(),
            strokeWidth = strokeWidth.copy(),
            strokeDash = strokeDash?.map(StrokeDash::copy),
            color = color.copy()
        )
    }
}
