package com.skyd.podaura.ui.component.shape

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection


enum class ForwardRippleDirect {
    Forward, Backward
}

class ForwardRippleShape(private val direct: ForwardRippleDirect) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val rect = size.toRect().run {
            copy(
                left = if (direct == ForwardRippleDirect.Forward) left else left - width,
                right = if (direct == ForwardRippleDirect.Forward) right + width else right,
                top = top - width / 2,
                bottom = bottom + width / 2
            )
        }
        val path = Path().apply {
            addArc(
                oval = rect,
                startAngleDegrees = if (direct == ForwardRippleDirect.Forward) 90f else -90f,
                sweepAngleDegrees = 180f
            )
        }
        return Outline.Generic(path)
    }
}