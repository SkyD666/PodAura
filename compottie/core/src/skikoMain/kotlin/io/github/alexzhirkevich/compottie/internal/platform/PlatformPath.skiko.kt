package io.github.alexzhirkevich.compottie.internal.platform

import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.asSkiaPathMeasure
import androidx.compose.ui.graphics.materializeSkiaPath
import org.jetbrains.skia.PathBuilder as SkPathBuilder

internal actual fun ExtendedPathMeasure(): ExtendedPathMeasure = SkikoExtendedPathMeasure()

internal actual fun PathBuilder(): PathBuilder = SkikoPathBuilder()

private class SkikoExtendedPathMeasure(
    private val delegate: PathMeasure = PathMeasure()
) : ExtendedPathMeasure, PathMeasure by delegate {

    override fun nextContour(): Boolean {
        return delegate.asSkiaPathMeasure().nextContour()
    }
}

private class SkikoPathBuilder : PathBuilder {

    private val skikoPathBuilder = SkPathBuilder()

    override fun addPath(
        path: Path,
        matrix: Matrix
    ) {
        skikoPathBuilder.addPath(path.materializeSkiaPath(), matrix = matrix.asSkia33())
    }

    override fun setTo(path: Path) {
        val snapshot = skikoPathBuilder.snapshot()
        path.materializeSkiaPath().swap(snapshot)
        snapshot.close()
        skikoPathBuilder.reset()
    }

    override fun close() {
        skikoPathBuilder.close()
    }
}
