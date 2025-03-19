package com.skyd.anivu.ui.mpv.land.controller.bar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.materialkolor.ktx.darken
import com.materialkolor.ktx.lighten


internal val TrackHeight = 3.dp

@Composable
fun Track(
    sliderState: SliderState,
    bufferDurationValue: Float,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SliderColors = SliderDefaults.colors(),
    drawStopIndicator: (DrawScope.(Offset) -> Unit)? = {
        drawCircle(
            color = colors.activeTrackColor,
            center = it,
            radius = SliderDefaults.TrackStopIndicatorSize.toPx() / 2f
        )
    },
    drawTick: (DrawScope.(Offset, Color) -> Unit)? = { offset, color ->
        drawCircle(
            color = color,
            center = offset,
            radius = SliderDefaults.TickSize.toPx() / 2f
        )
    },
    thumbTrackGapSize: Dp = 3.dp,
    trackInsideCornerSize: Dp = 2.dp
) {
    val inactiveTrackColor = colors.trackColor(enabled, active = false)
    val activeTrackColor = colors.trackColor(enabled, active = true)
    val bufferTrackColor = MaterialTheme.colorScheme.secondaryContainer.run {
        if (isSystemInDarkTheme()) lighten(3f) else darken(3f)
    }
    val inactiveTickColor = colors.tickColor(enabled, active = false)
    val activeTickColor = colors.tickColor(enabled, active = true)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(3.dp)
            .rotate(if (LocalLayoutDirection.current == LayoutDirection.Rtl) 180f else 0f)
    ) {
        drawTrack(
            tickFractions = sliderState.tickFractions,
            activeRangeStart = 0f,
            activeRangeFraction = sliderState.coercedValueAsFraction,
            bufferRangeFraction = sliderState.coercedBufferValueAsFraction(bufferDurationValue),
            inactiveTrackColor = inactiveTrackColor,
            activeTrackColor = activeTrackColor,
            bufferTrackColor = bufferTrackColor,
            inactiveTickColor = inactiveTickColor,
            activeTickColor = activeTickColor,
            height = TrackHeight,
            thumbWidth = ThumbSize,
            thumbTrackGapSize = thumbTrackGapSize,
            trackInsideCornerSize = trackInsideCornerSize,
            drawStopIndicator = drawStopIndicator,
            drawTick = drawTick,
        )
    }
}

private fun stepsToTickFractions(steps: Int): FloatArray {
    return if (steps == 0) floatArrayOf() else FloatArray(steps + 2) { it.toFloat() / (steps + 1) }
}

internal val SliderState.tickFractions
    get() = stepsToTickFractions(steps)

private fun calcFraction(a: Float, b: Float, pos: Float) =
    (if (b - a == 0f) 0f else (pos - a) / (b - a)).coerceIn(0f, 1f)

internal fun SliderState.coercedBufferValueAsFraction(bufferValue: Float) = calcFraction(
    valueRange.start,
    valueRange.endInclusive,
    bufferValue.coerceIn(valueRange.start, valueRange.endInclusive),
)

@Stable
internal fun SliderColors.trackColor(enabled: Boolean, active: Boolean): Color =
    if (enabled) {
        if (active) activeTrackColor else inactiveTrackColor
    } else {
        if (active) disabledActiveTrackColor else disabledInactiveTrackColor
    }

@Stable
internal fun SliderColors.tickColor(enabled: Boolean, active: Boolean): Color =
    if (enabled) {
        if (active) activeTickColor else inactiveTickColor
    } else {
        if (active) disabledActiveTickColor else disabledInactiveTickColor
    }

private fun DrawScope.drawTrack(
    tickFractions: FloatArray,
    activeRangeStart: Float,
    activeRangeFraction: Float,
    bufferRangeFraction: Float,
    inactiveTrackColor: Color,
    activeTrackColor: Color,
    bufferTrackColor: Color,
    inactiveTickColor: Color,       // bufferTickColor == inactiveTickColor
    activeTickColor: Color,
    height: Dp,
    thumbWidth: Dp,
    thumbTrackGapSize: Dp,
    trackInsideCornerSize: Dp,
    drawStopIndicator: (DrawScope.(Offset) -> Unit)?,
    drawTick: (DrawScope.(Offset, Color) -> Unit)?,
) {
    val sliderStart = Offset(0f, center.y)
    val sliderEnd = Offset(size.width, center.y)
    val trackStrokeWidth = height.toPx()
    val gap = if (thumbTrackGapSize > 0.dp) thumbWidth.toPx() / 2 + thumbTrackGapSize.toPx() else 0f

    val sliderValueEnd = Offset(
        sliderStart.x + (sliderEnd.x - sliderStart.x) * activeRangeFraction,
        center.y
    )

    val sliderBufferEnd = Offset(
        sliderValueEnd.x + gap + (sliderEnd.x - sliderStart.x) * bufferRangeFraction,
        center.y
    )

    val cornerSize = trackStrokeWidth / 2
    val insideCornerSize = trackInsideCornerSize.toPx()

    val bufferTrackStart = sliderValueEnd.x + gap
    val bufferTrackEnd = (sliderBufferEnd.x).coerceAtMost(sliderEnd.x - cornerSize)

    val shouldDrawBufferTrack = bufferTrackStart < bufferTrackEnd

    val inactiveTrackStart =
        if (shouldDrawBufferTrack) sliderBufferEnd.x else (sliderValueEnd.x + gap)
    val showInactiveTrack = inactiveTrackStart < sliderEnd.x - cornerSize

    // buffer track
    if (shouldDrawBufferTrack) {
        drawTrackPath(
            offset = Offset(bufferTrackStart, 0f),
            size = Size(bufferTrackEnd - bufferTrackStart, trackStrokeWidth),
            color = bufferTrackColor,
            startCornerRadius = insideCornerSize,
            endCornerRadius = if (showInactiveTrack) 0f else cornerSize,
        )
    }
    // inactive track
    if (showInactiveTrack) {
        drawTrackPath(
            offset = Offset(inactiveTrackStart, 0f),
            size = Size(sliderEnd.x - inactiveTrackStart, trackStrokeWidth),
            color = inactiveTrackColor,
            startCornerRadius = if (shouldDrawBufferTrack) 0f else insideCornerSize,
            endCornerRadius = cornerSize,
        )
    }
    if (shouldDrawBufferTrack || showInactiveTrack) {
        drawStopIndicator?.invoke(this, Offset(sliderEnd.x - cornerSize, center.y))
    }
    // active track
    val activeTrackEnd = sliderValueEnd.x - gap
    if (activeTrackEnd > cornerSize) {
        drawTrackPath(
            offset = Offset(0f, 0f),
            size = Size(activeTrackEnd, trackStrokeWidth),
            color = activeTrackColor,
            startCornerRadius = cornerSize,
            endCornerRadius = insideCornerSize,
        )
    }

    val start = Offset(sliderStart.x + cornerSize, sliderStart.y)
    val end = Offset(sliderEnd.x - cornerSize, sliderEnd.y)
    val endGap = sliderValueEnd.x - gap..sliderValueEnd.x + gap
    tickFractions.forEachIndexed { index, tick ->
        // skip ticks that fall on the stop indicator
        if (drawStopIndicator != null) {
            if (index == tickFractions.size - 1) {
                return@forEachIndexed
            }
        }

        val outsideFraction = tick > activeRangeFraction || tick < activeRangeStart
        val center = Offset(lerp(start, end, tick).x, center.y)
        // skip ticks that fall on a gap
        if (center.x in endGap) {
            return@forEachIndexed
        }
        drawTick?.invoke(
            this,
            center, // offset
            if (outsideFraction) inactiveTickColor else activeTickColor // color
        )
    }
}

private fun DrawScope.drawTrackPath(
    offset: Offset,
    size: Size,
    color: Color,
    startCornerRadius: Float,
    endCornerRadius: Float
) {
    trackPath.rewind()

    val track =
        Rect(
            Offset(offset.x + startCornerRadius, 0f),
            size = Size(size.width - startCornerRadius - endCornerRadius, size.height)
        )
    trackPath.addRect(track)

    buildCorner(offset, size, startCornerRadius, isStart = true) // start
    buildCorner(
        Offset(track.right - endCornerRadius, 0f),
        size,
        endCornerRadius
    ) // end

    drawPath(trackPath, color)

    trackPath.rewind()
}

private fun buildCorner(
    offset: Offset,
    size: Size,
    cornerRadius: Float,
    isStart: Boolean = false
) {
    cornerPath.rewind()
    halfRectPath.rewind()

    val corner = RoundRect(
        rect = Rect(
            offset,
            size = Size(cornerRadius * 2, size.height)
        ), cornerRadius = CornerRadius(cornerRadius)
    )
    cornerPath.addRoundRect(corner)

    // delete the unnecessary half of the RoundRect
    halfRectPath.addRect(
        Rect(
            Offset(corner.left + if (isStart) cornerRadius else 0f, 0f),
            size = Size(cornerRadius, size.height)
        )
    )
    trackPath.addPath(cornerPath - halfRectPath)

    cornerPath.rewind()
    halfRectPath.rewind()
}

private val trackPath = Path()
private val cornerPath = Path()
private val halfRectPath = Path()
