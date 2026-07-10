package io.github.alexzhirkevich.compottie.internal

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Vertices
import androidx.compose.ui.graphics.drawscope.DrawContext
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

private object EmptyCanvas : Canvas {
    override fun save() {
    }

    override fun restore() {
    }

    override fun saveLayer(
        bounds: Rect,
        paint: Paint
    ) {
    }

    override fun translate(dx: Float, dy: Float) {
    }

    override fun scale(sx: Float, sy: Float) {
    }

    override fun rotate(degrees: Float) {
    }

    override fun skew(sx: Float, sy: Float) {
    }

    override fun concat(matrix: Matrix) {
    }

    override fun clipRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        clipOp: ClipOp
    ) {
    }

    override fun clipPath(
        path: Path,
        clipOp: ClipOp
    ) {
    }

    override fun drawLine(
        p1: Offset,
        p2: Offset,
        paint: Paint
    ) {
    }

    override fun drawRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        paint: Paint
    ) {
    }

    override fun drawRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radiusX: Float,
        radiusY: Float,
        paint: Paint
    ) {
    }

    override fun drawOval(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        paint: Paint
    ) {
    }

    override fun drawCircle(
        center: Offset,
        radius: Float,
        paint: Paint
    ) {
    }

    override fun drawArc(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        paint: Paint
    ) {
    }

    override fun drawPath(
        path: Path,
        paint: Paint
    ) {
    }

    override fun drawImage(
        image: ImageBitmap,
        topLeftOffset: Offset,
        paint: Paint
    ) {
    }

    override fun drawImageRect(
        image: ImageBitmap,
        srcOffset: IntOffset,
        srcSize: IntSize,
        dstOffset: IntOffset,
        dstSize: IntSize,
        paint: Paint
    ) {
    }

    override fun drawPoints(
        pointMode: PointMode,
        points: List<Offset>,
        paint: Paint
    ) {
    }

    override fun drawRawPoints(
        pointMode: PointMode,
        points: FloatArray,
        paint: Paint
    ) {
    }

    override fun drawVertices(
        vertices: Vertices,
        blendMode: BlendMode,
        paint: Paint
    ) {
    }

    override fun enableZ() {
    }

    override fun disableZ() {
    }
}


internal class EmptyDrawScope(
    val d: Density,
    override val layoutDirection: LayoutDirection,
) : DrawScope, Density by d {

    override val drawContext: DrawContext = object : DrawContext {

        override var canvas: Canvas
            get() = EmptyCanvas
            set(value) {}

        override var size: Size = Size(1000f, 1000f)
            set(value) {}

        override val transform: DrawTransform
            get() = object : DrawTransform {
                override val size: Size
                    get() = this@EmptyDrawScope.size

                override fun inset(
                    left: Float,
                    top: Float,
                    right: Float,
                    bottom: Float
                ) {
                }

                override fun clipRect(
                    left: Float,
                    top: Float,
                    right: Float,
                    bottom: Float,
                    clipOp: ClipOp
                ) {
                }

                override fun clipPath(
                    path: Path,
                    clipOp: ClipOp
                ) {
                }

                override fun translate(left: Float, top: Float) {
                }

                override fun rotate(
                    degrees: Float,
                    pivot: Offset
                ) {
                }

                override fun scale(
                    scaleX: Float,
                    scaleY: Float,
                    pivot: Offset
                ) {
                }

                override fun transform(matrix: Matrix) {
                }
            }
    }

    override fun drawLine(
        brush: Brush,
        start: Offset,
        end: Offset,
        strokeWidth: Float,
        cap: StrokeCap,
        pathEffect: PathEffect?,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
    }

    override fun drawLine(
        color: Color,
        start: Offset,
        end: Offset,
        strokeWidth: Float,
        cap: StrokeCap,
        pathEffect: PathEffect?,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
    }

    override fun drawRect(
        brush: Brush,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
    }

    override fun drawRect(
        color: Color,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
    }

    override fun drawImage(
        image: ImageBitmap,
        topLeft: Offset,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
    }

    @Deprecated(
        "Prefer usage of drawImage that consumes an optional FilterQuality parameter",
        replaceWith = ReplaceWith(
            "drawImage(image, srcOffset, srcSize, dstOffset, dstSize, alpha, style, colorFilter, blendMode, FilterQuality.Low)",
            "androidx.compose.ui.graphics.drawscope",
            "androidx.compose.ui.graphics.FilterQuality"
        ),
        level = DeprecationLevel.HIDDEN
    )
    override fun drawImage(
        image: ImageBitmap,
        srcOffset: IntOffset,
        srcSize: IntSize,
        dstOffset: IntOffset,
        dstSize: IntSize,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
    }

    override fun drawRoundRect(
        brush: Brush,
        topLeft: Offset,
        size: Size,
        cornerRadius: CornerRadius,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
    }

    override fun drawRoundRect(
        color: Color,
        topLeft: Offset,
        size: Size,
        cornerRadius: CornerRadius,
        style: DrawStyle,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
    }

    override fun drawCircle(
        brush: Brush,
        radius: Float,
        center: Offset,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
    }

    override fun drawCircle(
        color: Color,
        radius: Float,
        center: Offset,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
    }

    override fun drawOval(
        brush: Brush,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
    }

    override fun drawOval(
        color: Color,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
    }

    override fun drawArc(
        brush: Brush,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
    }

    override fun drawArc(
        color: Color,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
    }

    override fun drawPath(
        path: Path,
        color: Color,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
    }

    override fun drawPath(
        path: Path,
        brush: Brush,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
    }

    override fun drawPoints(
        points: List<Offset>,
        pointMode: PointMode,
        color: Color,
        strokeWidth: Float,
        cap: StrokeCap,
        pathEffect: PathEffect?,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
    }

    override fun drawPoints(
        points: List<Offset>,
        pointMode: PointMode,
        brush: Brush,
        strokeWidth: Float,
        cap: StrokeCap,
        pathEffect: PathEffect?,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
    }
}
