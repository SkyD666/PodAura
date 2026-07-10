package io.github.alexzhirkevich.compottie.internal.helpers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import io.github.alexzhirkevich.compottie.internal.utils.toOffset
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.math.min


@Serializable
public class Bezier internal constructor(

    @SerialName("c")
    internal var isClosed: Boolean = false,

    @SerialName("i")
    internal var inTangents: List<FloatArray> = emptyList(),

    @SerialName("o")
    internal var outTangents: List<FloatArray> = emptyList(),

    @SerialName("v")
    internal val vertices: List<FloatArray> = emptyList(),
) {

    @Transient
    internal var curves: MutableList<CubicCurveData> = ArrayList(vertices.size)
        private set

    @Transient
    internal var initialPoint: Offset = Offset.Zero
        private set

    init {

        if (vertices.isNotEmpty()) {
            initialPoint = vertices.first().toOffset()

            for (i in 1 .. vertices.lastIndex) {

                val prevVertex = vertices[i - 1]
                val cp1 = outTangents.getOrNull(i - 1) ?: EmptyTangent
                val cp2 = inTangents.getOrNull(i) ?: EmptyTangent
                val vertex = vertices[i]

                val shapeCp1 = Offset(prevVertex[0] + cp1[0], prevVertex[1] + cp1[1])
                val shapeCp2 = Offset(vertex[0] + cp2[0], vertex[1] + cp2[1])
                curves.add(
                    CubicCurveData(
                        shapeCp1,
                        shapeCp2,
                        vertex.toOffset()
                    )
                )
            }

            if (isClosed) {
                closeShape()
            }
        }
    }

    internal fun setIsClosed(closed: Boolean) {
        if (isClosed == closed) {
            return
        }

        this.isClosed = closed
        if (closed) {
            closeShape()
        } else {
            curves.removeAt(curves.lastIndex)
        }
    }

    internal fun interpolateBetween(
        a: Bezier,
        b: Bezier,
        percentage: Float,
    ) {

        isClosed = a.isClosed || b.isClosed

        val points = min(a.curves.size, b.curves.size)

        if (curves.size < points) {
            repeat(points - curves.size) {
                curves.add(CubicCurveData())
            }
        }
        if (curves.size > points) {
            repeat(curves.size - points) {
                curves.removeAt(curves.lastIndex)
            }
        }

        initialPoint = lerp(a.initialPoint, b.initialPoint, percentage)

        curves.fastForEachIndexed { i, curve ->
            val curve1 = a.curves[i]
            val curve2 = b.curves[i]

            curve.controlPoint1 = lerp(curve1.controlPoint1, curve2.controlPoint1, percentage)
            curve.controlPoint2 = lerp(curve1.controlPoint2, curve2.controlPoint2, percentage)
            curve.vertex = lerp(curve1.vertex, curve2.vertex, percentage)
        }
    }

    internal fun mapPath(outPath: Path) {
        outPath.rewind()
        outPath.moveTo(initialPoint.x, initialPoint.y)

        var pathFromDataCurrentPoint = initialPoint

        curves.fastForEach { curve ->
            if (curve.controlPoint1 == pathFromDataCurrentPoint && curve.controlPoint2 == curve.vertex) {
                // On some phones like Samsung phones, zero valued control points can cause artifacting.
                // https://github.com/airbnb/lottie-android/issues/275
                //
                // This does its best to add a tiny value to the vertex without affecting the final
                // animation as much as possible.
                //            outPath.relativeMoveTo(0.01f, 0.01f);
                outPath.lineTo(curve.vertex.x, curve.vertex.y)
            } else {
                outPath.cubicTo(
                    curve.controlPoint1.x,
                    curve.controlPoint1.y,
                    curve.controlPoint2.x,
                    curve.controlPoint2.y,
                    curve.vertex.x,
                    curve.vertex.y
                )
            }
            pathFromDataCurrentPoint = curve.vertex
        }
        if (isClosed) {
            outPath.close()
        }
    }

    private fun closeShape() {
        val vertex = vertices[0]
        val prevVertex = vertices.last()
        val cp1 = outTangents.getOrNull(vertices.lastIndex) ?: EmptyTangent
        val cp2 = inTangents.getOrNull(0) ?: EmptyTangent

        val shapeCp1 = Offset(prevVertex[0] + cp1[0], prevVertex[1] + cp1[1])
        val shapeCp2 = Offset(vertex[0] + cp2[0], vertex[1] + cp2[1])

        curves.add(
            CubicCurveData(
                shapeCp1,
                shapeCp2,
                vertex.toOffset()
            )
        )
    }
}

internal class CubicCurveData(
    var controlPoint1: Offset = Offset.Zero,
    var controlPoint2: Offset = Offset.Zero,
    var vertex: Offset = Offset.Zero
)

private val EmptyTangent = floatArrayOf(0f, 0f)
