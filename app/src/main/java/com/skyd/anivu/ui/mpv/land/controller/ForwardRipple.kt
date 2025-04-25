package com.skyd.anivu.ui.mpv.land.controller

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skyd.anivu.ui.component.shape.ForwardRippleDirect
import com.skyd.anivu.ui.component.shape.ForwardRippleShape
import kotlin.math.sqrt

@Composable
fun ForwardRipple(
    modifier: Modifier = Modifier,
    direct: ForwardRippleDirect = ForwardRippleDirect.Forward,
    text: String,
    icon: ImageVector,
    controllerWidth: () -> Int,
    parentLayoutCoordinates: LayoutCoordinates?,
    rippleStartControllerOffset: Offset,
    onHideRipple: () -> Unit,
) {
    var finished by remember { mutableStateOf(false) }
    var globallyPositioned by remember { mutableStateOf(false) }
    var rippleStartOffset by remember { mutableStateOf(Offset.Zero) }
    var maxRadius by remember { mutableFloatStateOf(0f) }

    val animateRadius by animateFloatAsState(
        targetValue = if (!globallyPositioned) 0f else maxRadius,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "animateRadius",
        finishedListener = {
            onHideRipple()
            finished = true
        }
    )

    if (finished) onHideRipple()

    Box(
        modifier = modifier
            .clip(ForwardRippleShape(direct))
            .onSizeChanged {
                maxRadius = sqrt(it.height * it.height.toDouble() + it.width * it.width).toFloat()
            }
            .width(with(LocalDensity.current) { controllerWidth().toDp() / 3f })
            .fillMaxHeight()
            .onGloballyPositioned { coordinates ->
                if (!globallyPositioned) {
                    rippleStartOffset = rippleStartControllerOffset -
                            (parentLayoutCoordinates?.localPositionOf(coordinates, Offset.Zero)
                                ?: Offset.Zero)
                    globallyPositioned = true
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = ControllerLabelGray,
                center = rippleStartOffset,
                radius = animateRadius,
            )
        }
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                modifier = Modifier.size(50.dp),
                imageVector = icon,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontSize = 18.sp,
                color = Color.White,
            )
        }
    }
}