package com.skyd.podaura.ui.component.shape

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class CurlyCornerShape(
    private val amp: Float = 16f,
    private val count: Float = 12f,
) : CornerBasedShape(
    topStart = ZeroCornerSize,
    topEnd = ZeroCornerSize,
    bottomEnd = ZeroCornerSize,
    bottomStart = ZeroCornerSize
) {

    private fun sineCircleXYatAngle(
        d1: Float,
        d2: Float,
        d3: Float,
        d4: Float,
        d5: Float,
        i: Float,
    ): List<Float> = (i * d5).run {
        listOf(
            (sin(this) * d4 + d3) * cos(d5) + d1,
            (sin(this) * d4 + d3) * sin(d5) + d2
        )
    }

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection,
    ): Outline {
        val d = 2f
        val r2: Float = size.width / d
        var r13: Float = size.height / d
        val r18: Float = size.width / 2f - amp
        val path = Path()
        path.moveTo((d * r2 - amp), r13)
        var i = 0
        while (true) {
            val i2 = i + 1
            val d3 = r13
            val r5: List<Float> = sineCircleXYatAngle(
                r2, r13, r18, amp, (i * PI / 180.0).toFloat(), count
            )
            path.lineTo(r5[0], r5[1])
            if (i2 >= 360) {
                path.close()
                return Outline.Generic(path)
            }
            i = i2
            r13 = d3
        }
    }

    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize,
    ) = RoundedCornerShape(
        topStart = topStart,
        topEnd = topEnd,
        bottomEnd = bottomEnd,
        bottomStart = bottomStart
    )
}