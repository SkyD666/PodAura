package io.github.alexzhirkevich.compottie.internal.shapes

import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.util.fastForEach
import io.github.alexzhirkevich.compottie.dynamic.DynamicFillProvider
import io.github.alexzhirkevich.compottie.dynamic.DynamicShapeLayerProvider
import io.github.alexzhirkevich.compottie.dynamic.DynamicShapeProvider
import io.github.alexzhirkevich.compottie.dynamic.applyToPaint
import io.github.alexzhirkevich.compottie.dynamic.derive
import io.github.alexzhirkevich.compottie.dynamic.layerPath
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.animation.AnimatedNumber
import io.github.alexzhirkevich.compottie.internal.animation.AnimatedVector2
import io.github.alexzhirkevich.compottie.internal.animation.defaultOpacity
import io.github.alexzhirkevich.compottie.internal.content.Content
import io.github.alexzhirkevich.compottie.internal.content.DrawingContent
import io.github.alexzhirkevich.compottie.internal.content.PathContent
import io.github.alexzhirkevich.compottie.internal.content.nameOrDefault
import io.github.alexzhirkevich.compottie.internal.effects.LayerEffectsState
import io.github.alexzhirkevich.compottie.internal.helpers.FillRule
import io.github.alexzhirkevich.compottie.internal.helpers.GradientColors
import io.github.alexzhirkevich.compottie.internal.helpers.GradientType
import io.github.alexzhirkevich.compottie.internal.helpers.asPathFillType
import io.github.alexzhirkevich.compottie.internal.platform.GradientCache
import io.github.alexzhirkevich.compottie.internal.platform.GradientShader
import io.github.alexzhirkevich.compottie.internal.platform.PathBuilder
import io.github.alexzhirkevich.compottie.internal.utils.IdentityMatrix
import io.github.alexzhirkevich.compottie.internal.utils.extendBy
import io.github.alexzhirkevich.compottie.internal.utils.firstInstanceOf
import io.github.alexzhirkevich.compottie.internal.utils.set
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("gf")
internal class GradientFillShape(

    @SerialName("mn")
    override val matchName: String? = null,

    @SerialName("nm")
    override val name: String? = null,

    @SerialName("hd")
    override val hidden: Boolean = false,

    @SerialName("o")
    val opacity: AnimatedNumber = AnimatedNumber.defaultOpacity(),

    @SerialName("s")
    val startPoint: AnimatedVector2,

    @SerialName("e")
    val endPoint: AnimatedVector2,

    @SerialName("t")
    val gradientType: GradientType,

    @SerialName("h")
    val highlightLength: AnimatedNumber? = null,

    @SerialName("a")
    val highlightAngle: AnimatedNumber? = null,

    @SerialName("g")
    val colors: GradientColors,

    @SerialName("r")
    val fillRule: FillRule? = null,
) : Shape, DrawingContent {

    @Transient
    private val path = Path()

    @Transient
    private val pathBuilder = PathBuilder()

    @Transient
    private val fillType = fillRule?.asPathFillType() ?: path.fillType

    @Transient
    private val boundsRect = MutableRect(0f, 0f, 0f, 0f)

    @Transient
    private var pathContents: List<PathContent> = emptyList()

    private val paint = Paint().apply {
        isAntiAlias = true
    }

    @Transient
    private var dynamicFill: DynamicFillProvider? = null

    @Transient
    private var dynamicShape: DynamicShapeProvider? = null

    @Transient
    private val gradientCache = GradientCache()

    @Transient
    private var roundShape: RoundShape? = null

    private val effectsState by lazy {
        LayerEffectsState()
    }

    override fun draw(drawScope: DrawScope, parentMatrix: Matrix, parentAlpha: Float, state: AnimationState) {

        paint.pathEffect = null

        if (dynamicShape?.hidden.derive(hidden, state)) {
            return
        }

        if (dynamicFill?.gradient == null) {
            paint.shader = GradientShader(
                type = gradientType,
                startPoint = startPoint,
                endPoint = endPoint,
                colors = colors,
                highlightingAngle = highlightAngle,
                highlightingLength = highlightLength,
                state = state,
                matrix = parentMatrix,
                cache = gradientCache
            )
        }
        getBounds(drawScope, IdentityMatrix, false, state, boundsRect)

        dynamicFill.applyToPaint(
            paint = paint,
            state = state,
            parentAlpha = parentAlpha,
            opacity = opacity,
            parentMatrix = parentMatrix,
            size = boundsRect::toRect,
            gradientCache = gradientCache
        )

        state.thisLayer.effectsApplier.applyTo(paint, state, effectsState)

        path.rewind()
        path.fillType = fillType

        pathContents.fastForEach {
            pathBuilder.addPath(it.getPath(state), parentMatrix)
        }
        pathBuilder.setTo(path)

        roundShape?.applyTo(paint, state)

        drawScope.drawContext.canvas.drawPath(path, paint)
    }

    override fun getBounds(
        drawScope: DrawScope,
        parentMatrix: Matrix,
        applyParents: Boolean,
        state: AnimationState,
        outBounds: MutableRect
    ) {
        path.rewind()
        pathContents.fastForEach {
            pathBuilder.addPath(it.getPath(state), parentMatrix)
        }
        pathBuilder.setTo(path)
        outBounds.set(path.getBounds())
        outBounds.extendBy(1f)
    }

    override fun setDynamicProperties(basePath: String?, properties: DynamicShapeLayerProvider?) {
        super.setDynamicProperties(basePath, properties)

        val path = layerPath(basePath, nameOrDefault)
        dynamicFill = properties?.get(path)
        dynamicShape = properties?.get(path)
    }


    override fun setContents(contentsBefore: List<Content>, contentsAfter: List<Content>) {
        pathContents = contentsAfter.filterIsInstance<PathContent>()
        roundShape = contentsBefore.firstInstanceOf()
    }

    override fun prepareExpressions(state: AnimationState) {
        opacity.prepareExpressions(state)
        startPoint.prepareExpressions(state)
        endPoint.prepareExpressions(state)
        colors.prepareExpressions(state)
    }

    override fun deepCopy(): Shape {
        return GradientFillShape(
            matchName = matchName,
            name = name,
            hidden = hidden,
            opacity = opacity.copy(),
            startPoint = startPoint.copy(),
            endPoint = endPoint.copy(),
            gradientType = gradientType,
//            highlightLength = highlightLength?.copy(),
//            highlightAngle = highlightAngle?.copy(),
            colors = colors.copy(),
            fillRule = fillRule
        )
    }
}

