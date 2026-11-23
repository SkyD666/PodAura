package com.skyd.podaura.ui.screen.nano

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val NanoIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Nano",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        group(
            rotate = 8.5f,
            pivotX = 12f,
            pivotY = 12f,
            translationX = 0.85f,
            translationY = -0.45f,
        ) {
            // Clip the key just outside the body's contour so the gap shows the background.
            val exposedKey = PathData {
                moveTo(8.9f, 3f)
                curveTo(x1 = 14.4f, y1 = 2.94f, x2 = 18.28f, y2 = 7.9f, x3 = 19.15f, y3 = 12.65f)
                curveTo(x1 = 20.35f, y1 = 18.2f, x2 = 16.85f, y2 = 20.7f, x3 = 10.72f, y3 = 21.59f)
                verticalLineTo(24f)
                horizontalLineTo(24f)
                verticalLineTo(0f)
                horizontalLineTo(8.9f)
                close()
            }
            group(clipPathData = exposedKey) {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(12.85f, 13.73f)
                    lineTo(18.02f, 9.96f)
                    lineTo(18.97f, 11.26f)
                    lineTo(13.79f, 15.02f)
                    close()
                }
                path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                    moveTo(16.71f, 11.04f)
                    arcTo(
                        horizontalEllipseRadius = 2.85f,
                        verticalEllipseRadius = 2.85f,
                        theta = 0f,
                        isMoreThanHalf = true,
                        isPositiveArc = true,
                        x1 = 19.46f,
                        y1 = 9.05f,
                    )
                    curveTo(
                        x1 = 19.24f,
                        y1 = 9.72f,
                        x2 = 19.57f,
                        y2 = 10.17f,
                        x3 = 20.28f,
                        y3 = 10.18f
                    )
                    arcTo(
                        horizontalEllipseRadius = 2.85f,
                        verticalEllipseRadius = 2.85f,
                        theta = 0f,
                        isMoreThanHalf = true,
                        isPositiveArc = true,
                        x1 = 17.53f,
                        y1 = 12.18f,
                    )
                    curveTo(
                        x1 = 17.75f,
                        y1 = 11.5f,
                        x2 = 17.42f,
                        y2 = 11.05f,
                        x3 = 16.71f,
                        y3 = 11.04f
                    )
                    close()

                    roundHole(x = 16.74f, y = 8.19f)
                    roundHole(x = 20.25f, y = 13.03f)
                }
            }
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                moveTo(0.7f, 15.2f)
                curveTo(x1 = 0.45f, y1 = 12.8f, x2 = 1.75f, y2 = 11.25f, x3 = 2.35f, y3 = 9.4f)
                curveTo(x1 = 3.4f, y1 = 6.05f, x2 = 5.55f, y2 = 4.15f, x3 = 8.9f, y3 = 4.1f)
                curveTo(x1 = 13.7f, y1 = 4.04f, x2 = 17.2f, y2 = 8.24f, x3 = 18.05f, y3 = 12.85f)
                curveTo(x1 = 19.05f, y1 = 17.55f, x2 = 16.15f, y2 = 19.65f, x3 = 10.55f, y3 = 20.5f)
                curveTo(x1 = 4.65f, y1 = 21.4f, x2 = 1.12f, y2 = 19.35f, x3 = 0.7f, y3 = 15.2f)
                close()

                for ((x, y) in listOf(5.09f to 12.86f, 10.76f to 11.88f)) {
                    moveTo(x, y)
                    semicircle(radius = 0.87f, dx = 1.72f, dy = -0.3f)
                    lineToRelative(0.3f, 1.68f)
                    semicircle(radius = 0.87f, dx = -1.72f, dy = 0.3f)
                    close()
                }
            }
        }
    }.build()
}

private fun PathBuilder.roundHole(x: Float, y: Float) {
    val radius = 1.43f
    moveTo(x - radius, y)
    semicircle(radius = radius, dx = 2 * radius, dy = 0f)
    semicircle(radius = radius, dx = -2 * radius, dy = 0f)
    close()
}

private fun PathBuilder.semicircle(radius: Float, dx: Float, dy: Float) {
    arcToRelative(
        a = radius,
        b = radius,
        theta = 0f,
        isMoreThanHalf = false,
        isPositiveArc = true,
        dx1 = dx,
        dy1 = dy,
    )
}
