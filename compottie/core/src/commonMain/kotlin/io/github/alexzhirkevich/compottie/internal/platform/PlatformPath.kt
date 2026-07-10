package io.github.alexzhirkevich.compottie.internal.platform

import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure

internal interface ExtendedPathMeasure : PathMeasure {
    fun nextContour(): Boolean
}

internal interface PathBuilder : AutoCloseable {

    /**
     * Append a sub-path
     * */
    fun addPath(path: Path, matrix: Matrix)

    /**
     * Map a builder snapshot to the [path] and reset the [PathBuilder]
     * */
    fun setTo(path: Path)
}

internal expect fun ExtendedPathMeasure(): ExtendedPathMeasure

internal expect fun PathBuilder(): PathBuilder

internal fun Path.set(other: Path) {
    reset()
    addPath(other)
}
