package io.github.alexzhirkevich.compottie.internal.platform

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath

internal actual fun ExtendedPathMeasure(): ExtendedPathMeasure = AndroidExtendedPathMeasure(
    android.graphics.PathMeasure()
)

internal actual fun PathBuilder(): PathBuilder = AndroidPathBuilder()

private class AndroidPathBuilder : PathBuilder {
    private val androidPath = android.graphics.Path()
    private val androidMatrix = android.graphics.Matrix()

    override fun addPath(
        path: Path,
        matrix: Matrix
    ) {
        androidPath.addPath(
            path.asAndroidPath(),
            androidMatrix.apply {
                reset()
                setFromInternal(matrix)
            }
        )
    }

    override fun setTo(path: Path) {
        path.asAndroidPath().set(androidPath)
        androidPath.rewind()
    }

    override fun close() {
        androidPath.reset()
        androidMatrix.reset()
    }
}

private class AndroidExtendedPathMeasure(
    private val internalPathMeasure: android.graphics.PathMeasure
) : ExtendedPathMeasure {

    override fun nextContour(): Boolean {

        return internalPathMeasure.nextContour()
    }

    override val length: Float
        get() = internalPathMeasure.length

    private val positionArray: FloatArray = FloatArray(2)

    private val tangentArray: FloatArray = FloatArray(2)

    override fun getSegment(
        startDistance: Float,
        stopDistance: Float,
        destination: Path,
        startWithMoveTo: Boolean
    ): Boolean {
        return internalPathMeasure.getSegment(
            startDistance,
            stopDistance,
            destination.asAndroidPath(),
            startWithMoveTo
        )
    }

    override fun setPath(path: Path?, forceClosed: Boolean) {
        internalPathMeasure.setPath(path?.asAndroidPath(), forceClosed)
    }

    override fun getPosition(
        distance: Float
    ): Offset {
        val result = internalPathMeasure.getPosTan(distance, positionArray, tangentArray)
        return if (result) {
            Offset(positionArray[0], positionArray[1])
        } else {
            Offset.Unspecified
        }
    }

    override fun getTangent(
        distance: Float
    ): Offset {
        val result = internalPathMeasure.getPosTan(distance, positionArray, tangentArray)
        return if (result) {
            Offset(tangentArray[0], tangentArray[1])
        } else {
            Offset.Unspecified
        }
    }
}
