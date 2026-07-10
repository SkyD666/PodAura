package io.github.alexzhirkevich.compottie.internal.shapes

import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.util.fastMap
import io.github.alexzhirkevich.compottie.dynamic.DynamicShapeLayerProvider
import io.github.alexzhirkevich.compottie.dynamic.DynamicShapeProvider
import io.github.alexzhirkevich.compottie.dynamic.DynamicStrokeProvider
import io.github.alexzhirkevich.compottie.dynamic.applyToPaint
import io.github.alexzhirkevich.compottie.dynamic.derive
import io.github.alexzhirkevich.compottie.dynamic.layerPath
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.animation.AnimatedNumber
import io.github.alexzhirkevich.compottie.internal.animation.interpolatedNorm
import io.github.alexzhirkevich.compottie.internal.content.Content
import io.github.alexzhirkevich.compottie.internal.content.DrawingContent
import io.github.alexzhirkevich.compottie.internal.content.PathContent
import io.github.alexzhirkevich.compottie.internal.content.nameOrDefault
import io.github.alexzhirkevich.compottie.internal.effects.LayerEffectsState
import io.github.alexzhirkevich.compottie.internal.helpers.DashType
import io.github.alexzhirkevich.compottie.internal.helpers.StrokeDash
import io.github.alexzhirkevich.compottie.internal.helpers.applyTrimPath
import io.github.alexzhirkevich.compottie.internal.platform.ExtendedPathMeasure
import io.github.alexzhirkevich.compottie.internal.platform.GradientCache
import io.github.alexzhirkevich.compottie.internal.platform.PathBuilder
import io.github.alexzhirkevich.compottie.internal.platform.set
import io.github.alexzhirkevich.compottie.internal.utils.IdentityMatrix
import io.github.alexzhirkevich.compottie.internal.utils.appendPathEffect
import io.github.alexzhirkevich.compottie.internal.utils.extendBy
import io.github.alexzhirkevich.compottie.internal.utils.set
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.math.min

@Serializable
@JvmInline
internal value class LineCap(val type: Byte) {
    companion object {
        val Butt = LineCap(1)
        val Round = LineCap(2)
        val Square = LineCap(3)
    }

    fun asStrokeCap(): StrokeCap {
        return when (this) {
            Butt -> StrokeCap.Butt
            Round -> StrokeCap.Round
            Square -> StrokeCap.Square
            else -> error("Unknown line cap: $this")
        }
    }
}

@Serializable
@JvmInline
internal value class LineJoin(val type: Byte) {
    companion object {
        val Miter = LineJoin(1)
        val Round = LineJoin(2)
        val Bevel = LineJoin(3)
    }

    fun asStrokeJoin(): StrokeJoin {
        return when (this) {
            Miter -> StrokeJoin.Miter
            Round -> StrokeJoin.Round
            Bevel -> StrokeJoin.Bevel
            else -> error("Unknown line join: $this")
        }
    }
}


internal abstract class BaseStrokeShape : Shape, DrawingContent {

    abstract val opacity: AnimatedNumber
    abstract val strokeWidth: AnimatedNumber
    abstract val lineCap: LineCap
    abstract val lineJoin: LineJoin
    abstract val strokeMiter: Float
    abstract val strokeDash: List<StrokeDash>?

    private val pathGroups = mutableListOf<PathGroup>()

    private val trimPathPath = Path()
    private val path = Path()
    private val pathBuilder = PathBuilder()
    private val rawBoundsRect = MutableRect(0f, 0f, 0f, 0f)

    protected val paint by lazy {
        Paint().apply {
            isAntiAlias = true
            strokeMiterLimit = strokeMiter
            strokeCap = lineCap.asStrokeCap()
            strokeJoin = lineJoin.asStrokeJoin()
        }
    }

    private val pm by lazy {
        ExtendedPathMeasure()
    }

    private val dashPattern by lazy {
        strokeDash
            ?.fastFilter { it.dashType != DashType.Offset }
            ?.fastMap { it.value }
            ?.let {
                if (it.size % 2 == 1)
                    it + it
                else it
            }
    }

    private val dashOffset by lazy {
        strokeDash?.fastFirstOrNull { it.dashType == DashType.Offset }?.value
    }

    private val dashPatternValues by lazy {
        FloatArray(dashPattern?.size?.coerceAtLeast(2) ?: 0)
    }

    private var roundShape: RoundShape? = null

    private val effectsState by lazy {
        LayerEffectsState()
    }

    protected var dynamicStroke: DynamicStrokeProvider? = null

    private var dynamicShape: DynamicShapeProvider? = null

    protected var gradientCache = GradientCache()

    override fun draw(
        drawScope: DrawScope,
        parentMatrix: Matrix,
        parentAlpha: Float,
        state: AnimationState,
    ) {
        if (dynamicShape?.hidden.derive(hidden, state)) {
            return
        }

        paint.pathEffect = null
        paint.style = PaintingStyle.Stroke

        getBounds(drawScope, IdentityMatrix, false, state, rawBoundsRect)

        dynamicStroke.applyToPaint(
            paint = paint,
            state = state,
            parentAlpha = parentAlpha,
            parentMatrix = IdentityMatrix,
            opacity = opacity,
            strokeWidth = strokeWidth,
            size = rawBoundsRect::toRect,
            gradientCache = gradientCache
        )

        if (paint.strokeWidth <= 0) {
            return
        }

        applyDashPatternIfNeeded(state)

        state.thisLayer.effectsApplier.applyTo(paint, state, effectsState)

        roundShape?.applyTo(paint, state)

        drawScope.drawIntoCanvas { canvas ->
            canvas.save()
            canvas.concat(parentMatrix)
            pathGroups.fastForEach { pathGroup ->

                if (pathGroup.trimPath != null) {
                    applyTrimPath(canvas, state, pathGroup)
                } else {
                    path.rewind()
                    pathGroup.paths.fastForEachReversed {
                        path.addPath(it.getPath(state))
                    }
                    canvas.drawPath(path, paint)
                }
            }
            canvas.restore()
        }
    }

    override fun setDynamicProperties(basePath: String?, properties: DynamicShapeLayerProvider?) {
        super.setDynamicProperties(basePath, properties)

        val path = layerPath(basePath, nameOrDefault)
        dynamicStroke = properties?.get(path)
        dynamicShape = properties?.get(path)
    }

    override fun setContents(contentsBefore: List<Content>, contentsAfter: List<Content>) {

        val trimPathContentBefore: TrimPathShape? = contentsBefore
            .fastFirstOrNull(Content::isIndividualTrimPath) as TrimPathShape?

        var currentPathGroup: PathGroup? = null

        contentsAfter.fastForEachReversed { content ->
            if (content.isIndividualTrimPath()) {

                currentPathGroup?.let(pathGroups::add)

                currentPathGroup = PathGroup(content)

            } else if (content is PathContent) {
                if (currentPathGroup == null) {
                    currentPathGroup = PathGroup(trimPathContentBefore)
                }
                currentPathGroup.paths.add(content)
            } else if (content is RoundShape) {
                roundShape = content
            }
        }

        currentPathGroup?.let(pathGroups::add)
    }

    override fun getBounds(
        drawScope: DrawScope,
        parentMatrix: Matrix,
        applyParents: Boolean,
        state: AnimationState,
        outBounds: MutableRect,
    ) {
        path.rewind()
        pathGroups.fastForEach { pathGroup ->
            pathGroup.paths.fastForEach {
                pathBuilder.addPath(it.getPath(state), parentMatrix)
            }
        }
        pathBuilder.setTo(path)

        outBounds.set(path.getBounds())
        outBounds.extendBy(strokeWidth.interpolatedFloat(state) + 1)
    }

    override fun prepareExpressions(state: AnimationState) {
        opacity.prepareExpressions(state)
        strokeWidth.prepareExpressions(state)
        strokeDash?.fastForEach { it.prepareExpressions(state) }
    }

    private fun applyTrimPath(
        canvas: Canvas,
        state: AnimationState,
        pathGroup: PathGroup,
    ) {
        if (pathGroup.trimPath == null) {
            return
        }

        path.rewind()

        pathGroup.paths.fastForEachReversed {
            path.addPath(it.getPath(state))
        }
        val animStartValue: Float = pathGroup.trimPath.start.interpolatedNorm(state)
        val animEndValue: Float = pathGroup.trimPath.end.interpolatedNorm(state)
        val animOffsetValue: Float = pathGroup.trimPath.offset.interpolated(state) / 360f

        // If the start-end is ~100, consider it to be the full path.
        if (animStartValue < 0.01f && animEndValue > 0.99f) {
            canvas.drawPath(path, paint)
            return
        }

        pm.setPath(path, false)

        var totalLength: Float = pm.length

        while (pm.nextContour()) {
            totalLength += pm.length
        }
        val offsetLength = totalLength * animOffsetValue
        val startLength = totalLength * animStartValue + offsetLength
        val endLength = min(
            (totalLength * animEndValue + offsetLength).toDouble(),
            (startLength + totalLength - 1f).toDouble()
        ).toFloat()

        var currentLength = 0f

        pathGroup.paths.fastForEachReversed {
            trimPathPath.set(it.getPath(state))
//            trimPathPath.transform(parentMatrix)
            pm.setPath(trimPathPath, false)
            val length: Float = pm.length
            if (endLength > totalLength && endLength - totalLength < currentLength + length && currentLength < endLength - totalLength) {
                // Draw the segment when the end is greater than the length which wraps around to the
                // beginning.
                val startValue = if (startLength > totalLength) {
                    (startLength - totalLength) / length
                } else {
                    0f
                }
                val endValue = min(((endLength - totalLength) / length), 1f)
                trimPathPath.applyTrimPath(startValue, endValue, 0f)
                canvas.drawPath(trimPathPath, paint)
            } else
                if (currentLength + length < startLength || currentLength > endLength) {
                    // Do nothing
                } else if (currentLength + length <= endLength && startLength < currentLength) {
                    canvas.drawPath(trimPathPath, paint)
                } else {
                    val startValue = if (startLength < currentLength) {
                        0f
                    } else {
                        (startLength - currentLength) / length
                    }
                    val endValue = if (endLength > currentLength + length) {
                        1f
                    } else {
                        (endLength - currentLength) / length
                    }
                    trimPathPath.applyTrimPath(startValue, endValue, 0f)
                    canvas.drawPath(trimPathPath, paint)
                }
            currentLength += length
        }
    }


    private fun applyDashPatternIfNeeded(state: AnimationState) {


        val dp = dashPattern

        if (dp.isNullOrEmpty()) {
            return
        }


        val o = dashOffset?.interpolatedFloat(state) ?: 0f

        dp.fastForEachIndexed { i, strokeDash ->

            dashPatternValues[i] = strokeDash.interpolatedFloat(state)

            // If the value of the dash pattern or gap is too small, the number of individual sections
            // approaches infinity as the value approaches 0.
            // To mitigate this, we essentially put a minimum value on the dash pattern size of 1px
            // and a minimum gap size of 0.01.
            when {
                i % 2 == 0 -> dashPatternValues[i] = dashPatternValues[i].coerceAtLeast(1f)
                i % 2 == 1 -> dashPatternValues[i] = dashPatternValues[i].coerceAtLeast(.01f)
            }

            dashPatternValues[i] = dashPatternValues[i]
        }

        paint.appendPathEffect(PathEffect.dashPathEffect(dashPatternValues, o))
    }
}

private class PathGroup(
    val trimPath: TrimPathShape?,
) {

    val paths: MutableList<PathContent> = mutableListOf()
}