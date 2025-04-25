package com.skyd.anivu.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class PodAuraFloatingActionButtonStyle {
    Normal, Extended, Large, Small
}

@Composable
fun PodAuraFloatingActionButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onSizeWithSinglePaddingChanged: ((width: Dp, height: Dp) -> Unit)? = null,
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    style: PodAuraFloatingActionButtonStyle = PodAuraFloatingActionButtonStyle.Normal,
    contentDescription: String? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    val density = LocalDensity.current
    val floatingActionButton: @Composable (modifier: Modifier) -> Unit = {
        val newModifier = it.onSizeChanged {
            with(density) {
                onSizeWithSinglePaddingChanged?.invoke(
                    it.width.toDp() + 16.dp,
                    it.height.toDp() + 16.dp,
                )
            }
        }

        when (style) {
            PodAuraFloatingActionButtonStyle.Normal -> FloatingActionButton(
                modifier = newModifier,
                onClick = onClick,
                elevation = elevation,
                interactionSource = interactionSource,
                content = { Row { content() } },
            )

            PodAuraFloatingActionButtonStyle.Extended -> ExtendedFloatingActionButton(
                modifier = newModifier,
                onClick = onClick,
                elevation = elevation,
                interactionSource = interactionSource,
                content = { Row { content() } },
            )

            PodAuraFloatingActionButtonStyle.Large -> LargeFloatingActionButton(
                modifier = newModifier,
                onClick = onClick,
                elevation = elevation,
                interactionSource = interactionSource,
                content = { Row { content() } },
            )

            PodAuraFloatingActionButtonStyle.Small -> SmallFloatingActionButton(
                modifier = newModifier,
                onClick = onClick,
                elevation = elevation,
                interactionSource = interactionSource,
                content = { Row { content() } },
            )
        }
    }

    if (contentDescription.isNullOrEmpty()) {
        floatingActionButton(modifier)
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
            floatingActionButton(Modifier)
        }
    }
}

@Composable
fun PodAuraExtendedFloatingActionButton(
    modifier: Modifier = Modifier,
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    expanded: Boolean = true,
    onSizeWithSinglePaddingChanged: ((width: Dp, height: Dp) -> Unit)? = null,
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    contentDescription: String? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val density = LocalDensity.current
    val floatingActionButton: @Composable (modifier: Modifier) -> Unit = {
        ExtendedFloatingActionButton(
            text = text,
            icon = icon,
            modifier = it.onSizeChanged {
                with(density) {
                    onSizeWithSinglePaddingChanged?.invoke(
                        it.width.toDp() + 16.dp,
                        it.height.toDp() + 16.dp,
                    )
                }
            },
            onClick = onClick,
            expanded = expanded,
            elevation = elevation,
            interactionSource = interactionSource,
        )
    }

    if (contentDescription.isNullOrEmpty()) {
        floatingActionButton(modifier)
    } else {
        TooltipBox(
            modifier = modifier,
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
            tooltip = {
                PlainTooltip { Text(contentDescription) }
            },
            state = rememberTooltipState()
        ) {
            floatingActionButton(Modifier)
        }
    }
}

@Composable
fun BottomHideExtendedFloatingActionButton(
    visible: Boolean,
    modifier: Modifier = Modifier,
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    expanded: Boolean = true,
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    contentDescription: String? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val density = LocalDensity.current
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { with(density) { 40.dp.roundToPx() } } + fadeIn(),
        exit = slideOutVertically { with(density) { 40.dp.roundToPx() } } + fadeOut(),
    ) {
        PodAuraExtendedFloatingActionButton(
            modifier = modifier,
            text = text,
            icon = icon,
            onClick = onClick,
            expanded = expanded,
            elevation = elevation,
            contentDescription = contentDescription,
            interactionSource = interactionSource,
        )
    }
}