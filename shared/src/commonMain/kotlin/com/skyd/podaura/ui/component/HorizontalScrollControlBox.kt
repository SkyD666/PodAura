package com.skyd.podaura.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.horizontal_scroll_control_scroll_backward
import podaura.shared.generated.resources.horizontal_scroll_control_scroll_forward

@Composable
fun HorizontalScrollControlBox(
    modifier: Modifier = Modifier,
    state: HorizontalScrollControlState,
    scrollForwardButton: @Composable () -> Unit = {
        ScrollForwardButton(onClick = { state.scrollForward() })
    },
    scrollBackwardButton: @Composable () -> Unit = {
        ScrollBackwardButton(onClick = { state.scrollBackward() })
    },
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.type == PointerEventType.Exit) {
                        state.hovered = false
                        continue
                    }
                    event.changes.firstOrNull()?.let {
                        state.hovered = true
                    }
                }
            }
        },
    ) {
        content()

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = HorizontalScrollControlDefaults.buttonSpace),
        ) {
            Crossfade(targetState = state.showForwardButton) { show ->
                if (show) {
                    scrollForwardButton()
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = HorizontalScrollControlDefaults.buttonSpace),
        ) {
            Crossfade(targetState = state.showBackwardButton) { show ->
                if (show) {
                    scrollBackwardButton()
                }
            }
        }
    }
}

@Composable
fun rememberHorizontalScrollControlState(
    scrollableState: ScrollableState,
    onScroll: suspend (direction: HorizontalScrollDirection) -> Unit,
): HorizontalScrollControlState {
    val currentOnScroll by rememberUpdatedState(onScroll)
    val scope = rememberCoroutineScope()
    return remember(scrollableState, scope) {
        HorizontalScrollControlState(
            scrollableState = scrollableState,
            scope = scope,
            onScroll = currentOnScroll,
        )
    }
}

enum class HorizontalScrollDirection { BACKWARD, FORWARD }

@Stable
class HorizontalScrollControlState(
    private val scrollableState: ScrollableState,
    private val scope: CoroutineScope,
    private val onScroll: suspend (direction: HorizontalScrollDirection) -> Unit
) {
    var showForwardButton: Boolean by mutableStateOf(false)
        private set
    var showBackwardButton: Boolean by mutableStateOf(false)
        private set
    var hovered: Boolean = false
        internal set(value) {
            field = value
            calculate()
        }

    fun calculate() {
        showForwardButton = hovered && scrollableState.canScrollBackward
        showBackwardButton = hovered && scrollableState.canScrollForward
    }

    fun scrollBackward() = scope.launch {
        onScroll(HorizontalScrollDirection.BACKWARD)
        calculate()
    }

    fun scrollForward() = scope.launch {
        onScroll(HorizontalScrollDirection.FORWARD)
        calculate()
    }
}

object HorizontalScrollControlDefaults {
    val buttonSpace = 16.dp
    val buttonSize: Dp = 70.dp
    val buttonIconSize: Dp = 30.dp

    @Composable
    fun buttonShapes(): IconButtonShapes = IconButtonShapes(
        shape = IconButtonDefaults.extraLargeRoundShape,
        pressedShape = IconButtonDefaults.extraLargePressedShape,
    )
}

@Composable
private fun ScrollForwardButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        shapes = HorizontalScrollControlDefaults.buttonShapes(),
        modifier = Modifier.size(HorizontalScrollControlDefaults.buttonSize),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBackIos,
            contentDescription = stringResource(Res.string.horizontal_scroll_control_scroll_forward),
            modifier = Modifier.size(HorizontalScrollControlDefaults.buttonIconSize),
        )
    }
}

@Composable
private fun ScrollBackwardButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        shapes = HorizontalScrollControlDefaults.buttonShapes(),
        modifier = Modifier.size(HorizontalScrollControlDefaults.buttonSize),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
            contentDescription = stringResource(Res.string.horizontal_scroll_control_scroll_backward),
            modifier = Modifier.size(HorizontalScrollControlDefaults.buttonIconSize),
        )
    }
}