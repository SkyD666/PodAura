package com.skyd.podaura.ui.player.land.controller.bar

import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalSlider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.skyd.podaura.ui.player.component.ControllerIconButton
import com.skyd.podaura.ui.player.component.volumeIcon
import com.skyd.podaura.ui.player.volume.LocalVolumeControlFocus
import com.skyd.podaura.ui.player.volume.VolumeController
import com.skyd.podaura.ui.player.volume.adjustTo
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.player_mute
import podaura.shared.generated.resources.player_muted
import podaura.shared.generated.resources.player_unmute
import podaura.shared.generated.resources.player_volume
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun VolumeButton(
    controller: VolumeController,
    enabled: Boolean,
    onInteractionChanged: (Boolean) -> Unit,
) {
    val controlsEnabled = enabled && controller.available
    val buttonInteraction = remember { MutableInteractionSource() }
    val panelInteraction = remember { MutableInteractionSource() }
    val sliderInteraction = remember { MutableInteractionSource() }
    val buttonHovered by buttonInteraction.collectIsHoveredAsState()
    val panelHovered by panelInteraction.collectIsHoveredAsState()
    val dragging by sliderInteraction.collectIsDraggedAsState()
    var expanded by remember { mutableStateOf(false) }
    var keyboardOpen by remember { mutableStateOf(false) }
    var buttonFocused by remember { mutableStateOf(false) }
    val inputMode = LocalInputModeManager.current
    val focusManager = LocalFocusManager.current
    val sliderFocus = remember { FocusRequester() }
    val buttonFocus = remember { FocusRequester() }
    var advanceAfterPopup by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(keyboardOpen, advanceAfterPopup) {
        val advance = advanceAfterPopup
        if (!keyboardOpen && advance != null) {
            buttonFocus.requestFocus()
            if (advance) focusManager.moveFocus(FocusDirection.Next)
            advanceAfterPopup = null
        }
    }
    val windowVolumeFocus = LocalVolumeControlFocus.current
    SideEffect { windowVolumeFocus?.value = buttonFocused || keyboardOpen }
    val notify by rememberUpdatedState(onInteractionChanged)
    val keyboardFocused = buttonFocused && inputMode.inputMode == InputMode.Keyboard
    LaunchedEffect(
        controlsEnabled,
        buttonHovered,
        panelHovered,
        dragging,
        keyboardOpen,
        keyboardFocused
    ) {
        if (controlsEnabled && (buttonHovered || panelHovered || dragging || keyboardOpen || keyboardFocused)) expanded =
            true
        else {
            // Allow crossing from the button into the panel without collapsing it mid-flight.
            delay(180.milliseconds)
            expanded = false
        }
    }
    LaunchedEffect(expanded) { notify(expanded) }
    DisposableEffect(windowVolumeFocus) {
        onDispose {
            windowVolumeFocus?.value = false; notify(false)
        }
    }
    val volumeLabel = stringResource(Res.string.player_volume)
    val volumeStatus = if (controller.muted) stringResource(Res.string.player_muted)
    else "${controller.value.roundToInt()}%"
    Box {
        ControllerIconButton(
            modifier = Modifier.focusRequester(buttonFocus).hoverable(buttonInteraction)
                .onFocusChanged { buttonFocused = it.isFocused }
                .onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyDown && it.key == Key.Tab && !it.isShiftPressed) {
                        keyboardOpen = true
                        true
                    } else false
                },
            enabled = controlsEnabled,
            onClick = controller::toggleMute,
            imageVector = volumeIcon(controller.value, controller.range, controller.muted),
            contentDescription = stringResource(
                if (controller.muted || controller.value <= controller.range.start) Res.string.player_unmute
                else Res.string.player_mute,
            ),
        )
        if (expanded) {
            Popup(
                popupPositionProvider = remember { AboveVolumeButton },
                onDismissRequest = {
                    if (keyboardOpen) {
                        keyboardOpen = false
                        expanded = false
                        focusManager.clearFocus()
                    }
                },
                properties = volumePopupProperties(focusable = keyboardOpen),
            ) {
                Surface(shape = MaterialTheme.shapes.medium) {
                    Column(
                        modifier = Modifier.hoverable(panelInteraction)
                            .padding(horizontal = 4.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(volumeStatus, style = MaterialTheme.typography.labelLarge)
                        val slider = remember { SliderState(valueRange = controller.range) }
                        slider.value = controller.value.coerceIn(controller.range)
                        slider.onValueChange = controller::adjustTo
                        VerticalSlider(
                            state = slider,
                            reverseDirection = true,
                            enabled = controlsEnabled,
                            interactionSource = sliderInteraction,
                            thumb = {
                                SliderDefaults.Thumb(
                                    interactionSource = sliderInteraction,
                                    sliderState = it,
                                    enabled = controlsEnabled,
                                    thumbSize = DpSize(28.dp, 4.dp),
                                )
                            },
                            track = {
                                SliderDefaults.Track(
                                    sliderState = it,
                                    enabled = controlsEnabled,
                                    modifier = Modifier.width(6.dp),
                                )
                            },
                            modifier = Modifier.width(48.dp).height(112.dp)
                                .focusRequester(sliderFocus)
                                .semantics {
                                    contentDescription = volumeLabel; stateDescription =
                                    volumeStatus
                                }
                                .onPreviewKeyEvent {
                                    if (it.type == KeyEventType.KeyDown && (it.key == Key.Tab || it.key == Key.Escape)) {
                                        keyboardOpen = false
                                        expanded = false
                                        advanceAfterPopup = it.key == Key.Tab && !it.isShiftPressed
                                        true
                                    } else false
                                },
                        )
                    }
                }
                LaunchedEffect(keyboardOpen) { if (keyboardOpen) sliderFocus.requestFocus() }
            }
        }
    }
}

private object AboveVolumeButton : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset(
        (anchorBounds.center.x - popupContentSize.width / 2)
            .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
        (anchorBounds.top - popupContentSize.height).coerceAtLeast(0),
    )
}

internal expect fun volumePopupProperties(focusable: Boolean): PopupProperties
