package io.github.alexzhirkevich.compottie.internal.shapes

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import io.github.alexzhirkevich.compottie.dynamic.DynamicPolystarProvider
import io.github.alexzhirkevich.compottie.dynamic.DynamicShapeLayerProvider
import io.github.alexzhirkevich.compottie.dynamic.DynamicShapeProvider
import io.github.alexzhirkevich.compottie.dynamic.derive
import io.github.alexzhirkevich.compottie.dynamic.layerPath
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.animation.AnimatedNumber
import io.github.alexzhirkevich.compottie.internal.animation.AnimatedVector2
import io.github.alexzhirkevich.compottie.internal.animation.Vec2
import io.github.alexzhirkevich.compottie.internal.animation.defaultPosition
import io.github.alexzhirkevich.compottie.internal.animation.defaultRadius
import io.github.alexzhirkevich.compottie.internal.animation.defaultRotation
import io.github.alexzhirkevich.compottie.internal.animation.defaultRoundness
import io.github.alexzhirkevich.compottie.internal.animation.dynamicOffset
import io.github.alexzhirkevich.compottie.internal.animation.interpolatedNorm
import io.github.alexzhirkevich.compottie.internal.content.Content
import io.github.alexzhirkevich.compottie.internal.content.PathContent
import io.github.alexzhirkevich.compottie.internal.content.nameOrDefault
import io.github.alexzhirkevich.compottie.internal.helpers.CompoundSimultaneousTrimPath
import io.github.alexzhirkevich.compottie.internal.helpers.CompoundTrimPath
import io.github.alexzhirkevich.compottie.internal.utils.degreeToRadians
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.jvm.JvmInline
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

@Serializable
@JvmInline
internal value class StarType(val type: Byte) {
    companion object {
        val Star = StarType(1)
        val Polygon = StarType(2)
    }
}

@Serializable
@SerialName("sr")
internal class PolystarShape(

    @SerialName("mn")
    override val matchName: String? = null,

    @SerialName("nm")
    override val name: String? = null,

    @SerialName("hd")
    override val hidden: Boolean = false,

    @SerialName("p")
    val position: AnimatedVector2 = AnimatedVector2.defaultPosition(),

    @SerialName("d")
    val direction: Int = 1,

    @SerialName("is")
    val innerRoundness: AnimatedNumber = AnimatedNumber.defaultRoundness(),

    @SerialName("ir")
    val innerRadius: AnimatedNumber = AnimatedNumber.defaultRadius(),

    @SerialName("or")
    val outerRadius: AnimatedNumber = AnimatedNumber.defaultRadius(),

    @SerialName("os")
    val outerRoundness: AnimatedNumber = AnimatedNumber.defaultRoundness(),

    @SerialName("r")
    val rotation: AnimatedNumber = AnimatedNumber.defaultRotation(),

    @SerialName("pt")
    val points: AnimatedNumber,

    @SerialName("sy")
    val starType: StarType,
) : Shape, PathContent {

    @Transient
    private val path = Path()

    @Transient
    private val lastSegmentPath = Path()

    @Transient
    private val lastSegmentPathMeasure = PathMeasure()

    @Transient
    private var trimPaths: CompoundTrimPath? = null

    @Transient
    private var dynamicShape: DynamicShapeProvider? = null


    override fun getPath(state: AnimationState): Path {

        path.rewind()

        if (dynamicShape?.hidden.derive(hidden, state)) {
            return path
        }

        when (starType) {
            StarType.Star -> createStarPath(state)
            StarType.Polygon -> createPolygonPath(state)
        }

        trimPaths?.apply(path, state)

        return path
    }

    override fun setContents(contentsBefore: List<Content>, contentsAfter: List<Content>) {
        trimPaths = CompoundSimultaneousTrimPath(contentsBefore)
    }

    override fun prepareExpressions(state: AnimationState) {
        position.prepareExpressions(state)
        innerRoundness.prepareExpressions(state)
        outerRoundness.prepareExpressions(state)
        innerRadius.prepareExpressions(state)
        outerRadius.prepareExpressions(state)
        rotation.prepareExpressions(state)
        points.prepareExpressions(state)
    }

    override fun setDynamicProperties(basePath: String?, properties: DynamicShapeLayerProvider?) {
        super.setDynamicProperties(basePath, properties)

        dynamicShape = properties?.get(layerPath(basePath, nameOrDefault))
        val dynamicPolystar = dynamicShape as? DynamicPolystarProvider?

        position.dynamicOffset(dynamicPolystar?.position)
        points.dynamic = dynamicPolystar?.points
        rotation.dynamic = dynamicPolystar?.rotation
        innerRadius.dynamic = dynamicPolystar?.innerRadius
        innerRoundness.dynamic = dynamicPolystar?.innerRoundness
        outerRadius.dynamic = dynamicPolystar?.outerRadius
        outerRoundness.dynamic = dynamicPolystar?.outerRoundness

    }

    override fun deepCopy(): Shape {
        return PolystarShape(
            matchName = matchName,
            name = name,
            hidden = hidden,
            position = position.copy(),
            direction = direction,
            innerRoundness = innerRoundness.copy(),
            innerRadius = innerRadius.copy(),
            outerRadius = outerRadius.copy(),
            outerRoundness = outerRoundness.copy(),
            rotation = rotation.copy(),
            points = points.copy(),
            starType = starType
        )
    }


    private fun createStarPath(state: AnimationState) {
        val points = points.interpolated(state = state)
        var currentAngle = degreeToRadians(rotation.interpolatedFloat(state) - 90f)

        // adjust current angle for partial points
        var anglePerPoint: Float = (TwoPI / points).toFloat()
        if (direction == DIRECTION_REVERSED) {
            anglePerPoint *= -1f
        }
        val halfAnglePerPoint = anglePerPoint / 2.0f
        val partialPointAmount = points - points.toInt()
        if (partialPointAmount != 0f) {
            currentAngle += (halfAnglePerPoint * (1f - partialPointAmount))
        }

        val outerRadius = outerRadius.interpolatedFloat(state)
        val innerRadius = innerRadius.interpolatedFloat(state)

        val innerRoundedness = innerRoundness.interpolatedNorm(state)
        val outerRoundedness = outerRoundness.interpolatedNorm(state)

        var x: Float
        var y: Float
        var previousX: Float
        var previousY: Float
        var partialPointRadius = 0f

        if (partialPointAmount != 0f) {
            partialPointRadius = innerRadius + partialPointAmount * (outerRadius - innerRadius)
            x = partialPointRadius * cos(currentAngle)
            y = partialPointRadius * sin(currentAngle)
            path.moveTo(x, y)
            currentAngle += (anglePerPoint * partialPointAmount / 2f)
        } else {
            x = outerRadius * cos(currentAngle)
            y = outerRadius * sin(currentAngle)
            path.moveTo(x, y)
            currentAngle += halfAnglePerPoint
        }

        val startX = x
        val startY = y

        // True means the line will go to outer radius. False means inner radius.
        var longSegment = false
        val numPoints = (ceil(points) * 2).toInt()
        repeat(numPoints) { i ->
            var radius = if (longSegment) outerRadius else innerRadius
            var dTheta = halfAnglePerPoint
            if (partialPointRadius != 0f && i == numPoints - 2) {
                dTheta = anglePerPoint * partialPointAmount / 2f
            }
            if (partialPointRadius != 0f && i == numPoints - 1) {
                radius = partialPointRadius
            }
            previousX = x
            previousY = y
            x = radius * cos(currentAngle)
            y = radius * sin(currentAngle)

            if (innerRoundedness == 0f && outerRoundedness == 0f) {
                if (i == numPoints - 1) {
                    path.lineTo(startX, startY)
                } else {
                    path.lineTo(x, y)
                }
            } else {
                val cp1Theta = (atan2(previousY, previousX) - HalfPI)
                val cp1Dx = cos(cp1Theta).toFloat()
                val cp1Dy = sin(cp1Theta).toFloat()

                val cp2Theta = (atan2(y, x) - HalfPI)
                val cp2Dx = cos(cp2Theta).toFloat()
                val cp2Dy = sin(cp2Theta).toFloat()

                val cp1Roundedness = if (longSegment) innerRoundedness else outerRoundedness
                val cp2Roundedness = if (longSegment) outerRoundedness else innerRoundedness
                val cp1Radius = if (longSegment) innerRadius else outerRadius
                val cp2Radius = if (longSegment) outerRadius else innerRadius

                var cp1x: Float =
                    cp1Radius * cp1Roundedness * POLYSTAR_MAGIC_NUMBER * cp1Dx
                var cp1y: Float =
                    cp1Radius * cp1Roundedness * POLYSTAR_MAGIC_NUMBER * cp1Dy
                var cp2x: Float =
                    cp2Radius * cp2Roundedness * POLYSTAR_MAGIC_NUMBER * cp2Dx
                var cp2y: Float =
                    cp2Radius * cp2Roundedness * POLYSTAR_MAGIC_NUMBER * cp2Dy

                if (partialPointAmount != 0f) {
                    if (i == 0) {
                        cp1x *= partialPointAmount
                        cp1y *= partialPointAmount
                    } else if (i == numPoints - 1) {
                        cp2x *= partialPointAmount
                        cp2y *= partialPointAmount
                    }
                }

                path.cubicTo(previousX - cp1x, previousY - cp1y, x + cp2x, y + cp2y, x, y)
            }

            currentAngle += dTheta
            longSegment = !longSegment
        }

        val pos = Vec2(position.interpolatedVec(state))
        if (pos != Vec2.Zero) {
            path.translate(pos)
        }

        path.close()
    }

    private fun createPolygonPath(state: AnimationState) {
        val points = floor(points.interpolatedFloat(state)).toInt()
        var currentAngle = degreeToRadians((rotation.interpolatedFloat(state)) - 90f)

        // adjust current angle for partial points
        val anglePerPoint = (TwoPI / points).toFloat()

        val roundedness = outerRoundness.interpolatedNorm(state)
        val radius = outerRadius.interpolatedFloat(state)
        var x: Float
        var y: Float
        var previousX: Float
        var previousY: Float
        x = radius * cos(currentAngle)
        y = radius * sin(currentAngle)
        path.moveTo(x, y)
        currentAngle += anglePerPoint


        repeat(points) { i ->

            previousX = x
            previousY = y
            x = radius * cos(currentAngle)
            y = radius * sin(currentAngle)

            if (roundedness != 0f) {
                val cp1Theta = (atan2(previousY, previousX) - HalfPI)
                val cp1Dx = cos(cp1Theta)
                val cp1Dy = sin(cp1Theta)

                val cp2Theta = atan2(y, x) - HalfPI
                val cp2Dx = cos(cp2Theta).toFloat()
                val cp2Dy = sin(cp2Theta).toFloat()

                val cp1x = radius * roundedness * POLYGON_MAGIC_NUMBER * cp1Dx
                val cp1y = radius * roundedness * POLYGON_MAGIC_NUMBER * cp1Dy
                val cp2x = radius * roundedness * POLYGON_MAGIC_NUMBER * cp2Dx
                val cp2y = radius * roundedness * POLYGON_MAGIC_NUMBER * cp2Dy


                val vX = (previousX - cp1x).toFloat()
                val vY = (previousY - cp1y).toFloat()
                val cpX = x + cp2x
                val cpY = y + cp2y

                if (i == points - 1) {
                    // When there is a huge stroke, it will flash if the path ends where it starts.
                    // We want the final bezier curve to end *slightly* before the start.
                    // The close() call at the end will complete the polystar.
                    // https://github.com/airbnb/lottie-android/issues/2329
                    lastSegmentPath.reset()
                    lastSegmentPath.moveTo(previousX, previousY)
                    lastSegmentPath.cubicTo(vX, vY, cpX, cpY, x, y)
                    lastSegmentPathMeasure.setPath(lastSegmentPath, false)

                    val lastSegmentPosition =
                        lastSegmentPathMeasure.getPosition(lastSegmentPathMeasure.length * 0.9999f)
                    path.cubicTo(
                        vX,
                        vY,
                        cpX,
                        cpY,
                        lastSegmentPosition.x,
                        lastSegmentPosition.y
                    )
                } else {
                    path.cubicTo(vX, vY, x + cp2x, y + cp2y, x, y)
                }
            } else {
                if (i != points - 1) {
                    // When there is a huge stroke, it will flash if the path ends where it starts.
                    // The close() call should make the path effectively equivalent.
                    // https://github.com/airbnb/lottie-android/issues/2329
                    path.lineTo(x, y)
                }
            }

            currentAngle += anglePerPoint
        }

        val pos = Vec2(position.interpolatedVec(state))
        if (pos != Vec2.Zero) {
            path.translate(pos)
        }
        path.close()
    }
}

private const val HalfPI = PI / 2f
private const val TwoPI = PI * 2f
private const val POLYSTAR_MAGIC_NUMBER = .47829f
private const val POLYGON_MAGIC_NUMBER = .25f
