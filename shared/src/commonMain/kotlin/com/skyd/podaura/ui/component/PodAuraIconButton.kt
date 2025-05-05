package com.skyd.podaura.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.IconToggleButtonColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter

enum class PodAuraIconButtonStyle {
    Normal, Filled, FilledTonal, Outlined
}

@Composable
fun PodAuraIconButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    painter: Painter,
    tint: Color? = null,
    style: PodAuraIconButtonStyle = PodAuraIconButtonStyle.Normal,
    contentDescription: String? = null,
    rotate: Float = 0f,
    enabled: Boolean = true,
    colors: IconButtonColors? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val iconButton: @Composable (modifier: Modifier) -> Unit = {
        val icon: @Composable () -> Unit = {
            Icon(
                modifier = Modifier.rotate(rotate),
                painter = painter,
                tint = tint ?: LocalContentColor.current,
                contentDescription = contentDescription,
            )
        }
        when (style) {
            PodAuraIconButtonStyle.Normal -> IconButton(
                modifier = it,
                onClick = onClick,
                enabled = enabled,
                colors = colors ?: IconButtonDefaults.iconButtonColors(),
                interactionSource = interactionSource,
                content = icon,
            )

            PodAuraIconButtonStyle.Filled -> FilledIconButton(
                modifier = it,
                onClick = onClick,
                enabled = enabled,
                colors = colors ?: IconButtonDefaults.filledIconButtonColors(),
                interactionSource = interactionSource,
                content = icon,
            )

            PodAuraIconButtonStyle.FilledTonal -> FilledTonalIconButton(
                modifier = it,
                onClick = onClick,
                enabled = enabled,
                colors = colors ?: IconButtonDefaults.filledTonalIconButtonColors(),
                interactionSource = interactionSource,
                content = icon,
            )

            PodAuraIconButtonStyle.Outlined -> OutlinedIconButton(
                modifier = it,
                onClick = onClick,
                enabled = enabled,
                colors = colors ?: IconButtonDefaults.outlinedIconButtonColors(),
                interactionSource = interactionSource,
                content = icon,
            )
        }
    }

    if (contentDescription.isNullOrEmpty()) {
        iconButton(modifier)
    } else {
        Box(modifier = modifier) {  // It is necessary to use Modifier.align
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                tooltip = {
                    PlainTooltip {
                        Text(contentDescription)
                    }
                },
                state = rememberTooltipState()
            ) {
                iconButton(Modifier)
            }
        }
    }
}

@Composable
fun PodAuraIconButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    style: PodAuraIconButtonStyle = PodAuraIconButtonStyle.Normal,
    contentDescription: String? = null,
    rotate: Float = 0f,
    enabled: Boolean = true,
    colors: IconButtonColors? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    PodAuraIconButton(
        modifier = modifier,
        onClick = onClick,
        painter = rememberVectorPainter(image = imageVector),
        style = style,
        contentDescription = contentDescription,
        rotate = rotate,
        tint = tint,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
    )
}

@Composable
fun PodAuraIconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    contentDescription: String? = null,
    rotate: Float = 0f,
    enabled: Boolean = true,
    colors: IconToggleButtonColors = IconButtonDefaults.iconToggleButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val iconButton: @Composable (modifier: Modifier) -> Unit = {
        IconToggleButton(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = it,
            enabled = enabled,
            colors = colors,
            interactionSource = interactionSource,
            content = {
                Icon(
                    modifier = Modifier.rotate(rotate),
                    imageVector = imageVector,
                    tint = tint ?: LocalContentColor.current,
                    contentDescription = contentDescription,
                )
            },
        )
    }
    if (contentDescription.isNullOrEmpty()) {
        iconButton(modifier)
    } else {
        TooltipBox(
            modifier = modifier,
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
            tooltip = {
                PlainTooltip {
                    Text(contentDescription)
                }
            },
            state = rememberTooltipState()
        ) {
            iconButton(Modifier)
        }
    }
}