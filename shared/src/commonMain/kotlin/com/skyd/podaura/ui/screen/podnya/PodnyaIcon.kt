package com.skyd.podaura.ui.screen.podnya

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val PodnyaIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Podnya",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
            // One silhouette with cut-out features keeps the mark clear at entry size.
            moveTo(3.2f, 9.2f)
            lineTo(3.2f, 3.9f)
            curveTo(x1 = 3.2f, y1 = 2.9f, x2 = 3.8f, y2 = 2.6f, x3 = 4.6f, y3 = 3.2f)
            lineTo(8.2f, 5.7f)
            curveTo(x1 = 10.6f, y1 = 5.2f, x2 = 13.4f, y2 = 5.2f, x3 = 15.8f, y3 = 5.7f)
            lineTo(19.4f, 3.2f)
            curveTo(x1 = 20.2f, y1 = 2.6f, x2 = 20.8f, y2 = 2.9f, x3 = 20.8f, y3 = 3.9f)
            lineTo(20.8f, 9.2f)
            curveTo(x1 = 21.6f, y1 = 10.6f, x2 = 22f, y2 = 12.1f, x3 = 22f, y3 = 13.8f)
            curveTo(x1 = 22f, y1 = 18.8f, x2 = 18.8f, y2 = 21f, x3 = 12f, y3 = 21f)
            curveTo(x1 = 5.2f, y1 = 21f, x2 = 2f, y2 = 18.8f, x3 = 2f, y3 = 13.8f)
            curveTo(x1 = 2f, y1 = 12.1f, x2 = 2.4f, y2 = 10.6f, x3 = 3.2f, y3 = 9.2f)
            close()

            moveTo(6.7f, 11.6f)
            curveTo(x1 = 6.7f, y1 = 10.3f, x2 = 8.7f, y2 = 10.3f, x3 = 8.7f, y3 = 11.6f)
            lineTo(8.7f, 13f)
            curveTo(x1 = 8.7f, y1 = 14.3f, x2 = 6.7f, y2 = 14.3f, x3 = 6.7f, y3 = 13f)
            close()
            moveTo(15.3f, 11.6f)
            curveTo(x1 = 15.3f, y1 = 10.3f, x2 = 17.3f, y2 = 10.3f, x3 = 17.3f, y3 = 11.6f)
            lineTo(17.3f, 13f)
            curveTo(x1 = 17.3f, y1 = 14.3f, x2 = 15.3f, y2 = 14.3f, x3 = 15.3f, y3 = 13f)
            close()

            moveTo(9.6f, 16.1f)
            quadTo(x1 = 10f, y1 = 15.7f, x2 = 10.4f, y2 = 16.3f)
            curveTo(x1 = 10.7f, y1 = 17.1f, x2 = 11.4f, y2 = 17.1f, x3 = 11.6f, y3 = 16.2f)
            quadTo(x1 = 12f, y1 = 15.6f, x2 = 12.4f, y2 = 16.2f)
            curveTo(x1 = 12.6f, y1 = 17.1f, x2 = 13.3f, y2 = 17.1f, x3 = 13.6f, y3 = 16.3f)
            quadTo(x1 = 14f, y1 = 15.7f, x2 = 14.4f, y2 = 16.1f)
            curveTo(x1 = 14.3f, y1 = 18f, x2 = 12.7f, y2 = 18.2f, x3 = 12f, y3 = 17.2f)
            curveTo(x1 = 11.3f, y1 = 18.2f, x2 = 9.7f, y2 = 18f, x3 = 9.6f, y3 = 16.1f)
            close()
        }
    }.build()
}
