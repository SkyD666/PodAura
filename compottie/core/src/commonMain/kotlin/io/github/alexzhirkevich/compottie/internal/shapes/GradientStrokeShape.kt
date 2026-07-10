package io.github.alexzhirkevich.compottie.internal.shapes

import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.DrawScope
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.animation.AnimatedNumber
import io.github.alexzhirkevich.compottie.internal.animation.AnimatedVector2
import io.github.alexzhirkevich.compottie.internal.animation.defaultOpacity
import io.github.alexzhirkevich.compottie.internal.helpers.GradientColors
import io.github.alexzhirkevich.compottie.internal.helpers.GradientType
import io.github.alexzhirkevich.compottie.internal.helpers.StrokeDash
import io.github.alexzhirkevich.compottie.internal.platform.GradientShader
import io.github.alexzhirkevich.compottie.internal.utils.IdentityMatrix
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("gs")
internal class GradientStrokeShape(

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

    @SerialName("s")
    val startPoint: AnimatedVector2,

    @SerialName("e")
    val endPoint: AnimatedVector2,

    @SerialName("h")
    val highlightLength: AnimatedNumber? = null,

    @SerialName("a")
    val highlightAngle: AnimatedNumber? = null,

    @SerialName("g")
    val colors: GradientColors,

    @SerialName("t")
    val gradientType: GradientType = GradientType.Linear
) : BaseStrokeShape(), Shape {

    override fun draw(drawScope: DrawScope, parentMatrix: Matrix, parentAlpha: Float, state: AnimationState) {

        if (dynamicStroke?.gradient == null) {
            paint.shader = GradientShader(
                type = gradientType,
                startPoint = startPoint,
                endPoint = endPoint,
                colors = colors,
                highlightingAngle = highlightAngle,
                highlightingLength = highlightLength,
                state = state,
                matrix = IdentityMatrix,
                cache = gradientCache
            )
        }

        super.draw(drawScope, parentMatrix, parentAlpha, state)
    }

    override fun prepareExpressions(state: AnimationState) {
        super.prepareExpressions(state)
        startPoint.prepareExpressions(state)
        endPoint.prepareExpressions(state)
        highlightLength?.prepareExpressions(state)
        highlightAngle?.prepareExpressions(state)
        colors.prepareExpressions(state)
    }

    override fun deepCopy(): Shape {
        return GradientStrokeShape(
            matchName = matchName,
            name = name,
            hidden = hidden,
            lineCap = lineCap,
            lineJoin = lineJoin,
            strokeMiter = strokeMiter,
            opacity = opacity.copy(),
            strokeWidth = strokeWidth.copy(),
            strokeDash = strokeDash?.map(StrokeDash::copy),
            startPoint = startPoint.copy(),
            endPoint = endPoint.copy(),
            highlightLength = highlightLength?.copy(),
            highlightAngle = highlightAngle?.copy(),
            colors = colors.copy(),
            gradientType = gradientType
        )
    }
}
