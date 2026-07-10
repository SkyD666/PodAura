package io.github.alexzhirkevich.compottie.internal.shapes

import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
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
import io.github.alexzhirkevich.compottie.internal.animation.AnimatedColor
import io.github.alexzhirkevich.compottie.internal.animation.AnimatedNumber
import io.github.alexzhirkevich.compottie.internal.animation.defaultOpacity
import io.github.alexzhirkevich.compottie.internal.content.Content
import io.github.alexzhirkevich.compottie.internal.content.DrawingContent
import io.github.alexzhirkevich.compottie.internal.content.PathContent
import io.github.alexzhirkevich.compottie.internal.content.nameOrDefault
import io.github.alexzhirkevich.compottie.internal.effects.LayerEffectsState
import io.github.alexzhirkevich.compottie.internal.helpers.FillRule
import io.github.alexzhirkevich.compottie.internal.helpers.asPathFillType
import io.github.alexzhirkevich.compottie.internal.platform.GradientCache
import io.github.alexzhirkevich.compottie.internal.platform.PathBuilder
import io.github.alexzhirkevich.compottie.internal.utils.extendBy
import io.github.alexzhirkevich.compottie.internal.utils.set
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JsAny
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("fl")
internal class FillShape(

    @SerialName("mn")
    override val matchName: String? = null,

    @SerialName("nm")
    override val name: String? = null,

    @SerialName("hd")
    override val hidden: Boolean = false,

    @SerialName("d")
    val direction: Int = 1,

    @SerialName("o")
    val opacity: AnimatedNumber = AnimatedNumber.defaultOpacity(),

    @SerialName("c")
    val color: AnimatedColor,

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
    private var paths: List<PathContent> = emptyList()

    private val paint = Paint().apply {
        isAntiAlias = true
    }

    @Transient
    private var dynamicFill: DynamicFillProvider? = null

    @Transient
    private var dynamicShape: DynamicShapeProvider? = null


    @Transient
    private var roundShape: RoundShape? = null

    private val effectsState by lazy {
        LayerEffectsState()
    }

    @Transient
    private val gradientCache = GradientCache()

    override fun deepCopy(): Shape {
        return FillShape(
            matchName = matchName,
            name = name,
            hidden = hidden,
            direction = direction,
            opacity = opacity.copy(),
            color = color.copy(),
            fillRule = fillRule
        )
    }

    override fun draw(drawScope: DrawScope, parentMatrix: Matrix, parentAlpha: Float, state: AnimationState) {

        if (dynamicShape?.hidden.derive(hidden, state)) {
            return
        }

        paint.color = Color(color.interpolatedColor(state))
        paint.pathEffect = null

        dynamicFill.applyToPaint(
            paint = paint,
            state = state,
            parentAlpha = parentAlpha,
            parentMatrix = parentMatrix,
            opacity = opacity,
            size = { Rect.Zero },
            gradientCache = gradientCache
        )

        roundShape?.applyTo(paint, state)

        state.thisLayer.effectsApplier.applyTo(paint, state, effectsState)

        path.rewind()
        path.fillType = fillType

        paths.fastForEach {
            pathBuilder.addPath(it.getPath(state), parentMatrix)
        }
        pathBuilder.setTo(path)

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
        paths.fastForEach {
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
        paths = contentsAfter.filterIsInstance<PathContent>()

        roundShape = contentsBefore.find { it is RoundShape } as? RoundShape
    }

    override fun prepareExpressions(state: AnimationState) {
        opacity.prepareExpressions(state)
        color.prepareExpressions(state)
    }

    override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? {
        return when (property?.toString()) {
            "color" -> color
            else -> super.get(property, runtime)
        }
    }
}