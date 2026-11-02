package com.skyd.podaura.ui.player.component.dialog

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalPointerSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skyd.compone.component.connectedButtonShapes
import com.skyd.fundation.ext.format
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.ui.player.component.state.dialog.SpeedDialogCallback
import com.skyd.podaura.ui.player.component.state.dialog.SpeedDialogState
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.player_speed
import podaura.shared.generated.resources.player_speed_fine_tune
import podaura.shared.generated.resources.reset
import kotlin.math.roundToInt

private val speedRange = 0.25f..3f
private val speedPresets = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)
private const val SpeedStep = 0.05f

private fun snapSpeed(value: Float): Float =
    ((value / SpeedStep).roundToInt() / 20f).coerceIn(speedRange)

private fun speedLabel(value: Float): String = "${value.format(2).trimEnd('0').trimEnd('.', ',')}×"

@Composable
internal fun SpeedDialog(
    onDismissRequest: () -> Unit,
    playState: () -> PlayState,
    speedDialogState: () -> SpeedDialogState,
    speedDialogCallback: SpeedDialogCallback,
) {
    val state = speedDialogState()
    if (state.show) {
        val currentPlayState = playState()
        var value by rememberSaveable { mutableFloatStateOf(currentPlayState.speed) }
        var fineTuning by rememberSaveable { mutableStateOf(false) }
        var rulerValue by remember { mutableFloatStateOf(value) }
        var dragging by remember { mutableStateOf(false) }
        val rulerPosition by animateFloatAsState(
            targetValue = rulerValue,
            animationSpec = if (dragging) snap() else spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium,
                visibilityThreshold = 0.0005f,
            ),
            label = "Speed ruler settling",
        )
        val onSpeedChanged by rememberUpdatedState(speedDialogCallback.onSpeedChanged)
        val speedTitle = stringResource(Res.string.player_speed)
        val density = LocalDensity.current
        val tickSpacing = with(density) { 12.dp.toPx() * fontScale.coerceAtLeast(1f) }
        val rulerHeight =
            with(density) { MaterialTheme.typography.labelMedium.lineHeight.toDp() } + 40.dp
        var presetsHeight by remember { mutableStateOf(rulerHeight) }
        val focusRequester = remember { FocusRequester() }
        val interactionSource = remember { MutableInteractionSource() }

        fun applySpeed(speed: Float) {
            value = speed
            rulerValue = speed
            onSpeedChanged(speed)
        }

        BasicPlayerDialog(onDismissRequest = onDismissRequest) {
            Column(
                modifier = Modifier.widthIn(max = 400.dp)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = speedTitle,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    IconButton(onClick = { applySpeed(1f) }) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = stringResource(Res.string.reset),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = speedLabel(value),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                // Keep the gesture handler mounted when presets turn into the ruler, so the
                // first swipe continues adjusting instead of being cancelled by recomposition.
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .heightIn(min = presetsHeight.coerceAtLeast(rulerHeight))
                        .clip(MaterialTheme.shapes.large)
                        .background(if (fineTuning) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent)
                        .indication(interactionSource, ripple())
                        .focusRequester(focusRequester)
                        .onKeyEvent { event ->
                            val next = when (event.key) {
                                Key.DirectionRight, Key.DirectionUp -> snapSpeed(value + SpeedStep)
                                Key.DirectionLeft, Key.DirectionDown -> snapSpeed(value - SpeedStep)
                                Key.MoveHome -> speedRange.start
                                Key.MoveEnd -> speedRange.endInclusive
                                else -> return@onKeyEvent false
                            }
                            if (event.type == KeyEventType.KeyDown) {
                                focusRequester.requestFocus()
                                fineTuning = true
                                applySpeed(next)
                            }
                            true
                        }
                        .semantics {
                            contentDescription = speedTitle
                            stateDescription = speedLabel(value)
                            progressBarRangeInfo =
                                ProgressBarRangeInfo(value, speedRange, steps = 54)
                            setProgress { requested ->
                                fineTuning = true
                                applySpeed(snapSpeed(requested))
                                true
                            }
                        }
                        .focusable(interactionSource = interactionSource)
                        .pointerInput(tickSpacing) {
                            var dragStart = value
                            var dragValue: Float
                            try {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    var overSlop = 0f
                                    // Mouse slop is almost zero. Use touch slop while presets
                                    // are visible so a small click wobble does not steal the click.
                                    val drag = awaitHorizontalPointerSlopOrCancellation(
                                        pointerId = down.id,
                                        pointerType = if (fineTuning) down.type else PointerType.Touch,
                                    ) { change, amount ->
                                        change.consume()
                                        overSlop = amount
                                    } ?: return@awaitEachGesture
                                    dragStart = value
                                    dragValue = if (fineTuning) rulerPosition else value
                                    rulerValue = dragValue
                                    dragging = true
                                    fineTuning = true
                                    focusRequester.requestFocus()

                                    fun updateDrag(amount: Float) {
                                        dragValue = (dragValue - amount / tickSpacing * SpeedStep)
                                            .coerceIn(speedRange)
                                        rulerValue = dragValue
                                        value = snapSpeed(dragValue)
                                    }
                                    updateDrag(overSlop)
                                    val completed = horizontalDrag(drag.id) { change ->
                                        updateDrag(change.positionChange().x)
                                        change.consume()
                                    }
                                    dragging = false
                                    if (completed) {
                                        rulerValue = value
                                        onSpeedChanged(value)
                                    } else {
                                        value = dragStart
                                        rulerValue = dragStart
                                    }
                                }
                            } finally {
                                // Density changes or disposal can cancel the coroutine itself.
                                if (dragging) {
                                    dragging = false
                                    value = dragStart
                                    rulerValue = dragStart
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (fineTuning) {
                        SpeedRuler(
                            value = if (dragging) rulerValue else rulerPosition,
                            tickSpacing = tickSpacing,
                            modifier = Modifier.matchParentSize(),
                        )
                    } else {
                        SpeedPresets(
                            value = value,
                            onSelect = ::applySpeed,
                            modifier = Modifier.fillMaxWidth().onSizeChanged {
                                presetsHeight =
                                    with(density) { it.height.toDp() }.coerceAtLeast(rulerHeight)
                            },
                        )
                    }
                }
                Text(
                    text = if (fineTuning) "${speedLabel(speedRange.start)} – ${
                        speedLabel(
                            speedRange.endInclusive
                        )
                    }"
                    else stringResource(Res.string.player_speed_fine_tune),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun SpeedPresets(value: Float, onSelect: (Float) -> Unit, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelLarge
    val labelWidth =
        speedPresets.maxOf { textMeasurer.measure(speedLabel(it), labelStyle).size.width }
    val itemWidth = with(density) { labelWidth.toDp() + 4.dp }.coerceAtLeast(48.dp)
    val spacing = ButtonGroupDefaults.ConnectedSpaceBetween
    BoxWithConstraints(modifier) {
        val columns =
            ((maxWidth + spacing) / (itemWidth + spacing)).toInt().coerceIn(1, speedPresets.size)
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            speedPresets.chunked(columns).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    row.forEachIndexed { index, preset ->
                        ToggleButton(
                            checked = value == preset,
                            onCheckedChange = { if (it) onSelect(preset) },
                            modifier = Modifier.weight(1f).semantics {
                                selected = value == preset
                                role = Role.RadioButton
                            },
                            shapes = if (row.size == 1) ToggleButtonDefaults.shapes()
                            else ButtonGroupDefaults.connectedButtonShapes(row, index),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp),
                        ) {
                            Text(speedLabel(preset), maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedRuler(value: Float, tickSpacing: Float, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val textStyle = MaterialTheme.typography.labelMedium.copy(color = colors.onSurfaceVariant)
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier.clipToBounds()) {
        val labelHeight = textMeasurer.measure(speedLabel(1f), textStyle).size.height
        val tickBottom = size.height - labelHeight - 16.dp.toPx()
        for (tick in 5..60) {
            val speed = tick / 20f
            val x = center.x + (speed - value) / SpeedStep * tickSpacing
            val major = tick % 5 == 0
            drawLine(
                color = if (major) colors.onSurfaceVariant else colors.outline,
                start = Offset(x, 8.dp.toPx()),
                end = Offset(x, tickBottom - (if (major) 0.dp else 8.dp).toPx()),
                strokeWidth = (if (major) 2.dp else 1.dp).toPx(),
                cap = StrokeCap.Round,
            )
            if (major) {
                val label = textMeasurer.measure(speedLabel(speed), textStyle)
                val left = x - label.size.width / 2f
                drawText(label, topLeft = Offset(left, tickBottom + 8.dp.toPx()))
            }
        }
        drawLine(
            color = colors.primary,
            start = Offset(center.x, 4.dp.toPx()),
            end = Offset(center.x, tickBottom + 2.dp.toPx()),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}
