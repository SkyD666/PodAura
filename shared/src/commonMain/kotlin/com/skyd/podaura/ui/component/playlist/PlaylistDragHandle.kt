package com.skyd.podaura.ui.component.playlist

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.playlist_move_down
import podaura.shared.generated.resources.playlist_move_up
import sh.calvin.reorderable.DragGestureDetector
import sh.calvin.reorderable.ReorderableCollectionItemScope

/** Input-type based: a touchscreen on a desktop still requires a long press. */
@Composable
internal fun ReorderableCollectionItemScope.playlistDragHandle(
    enabled: Boolean,
    onStart: () -> Unit,
    onDrop: () -> Unit,
    onCancel: () -> Unit,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
): Modifier {
    val haptics = LocalHapticFeedback.current
    val cancel = rememberUpdatedState(onCancel)
    val startDrag = rememberUpdatedState(onStart)
    val drop = rememberUpdatedState(onDrop)
    val releasedNormally = remember { mutableStateOf(false) }
    val moveUp = stringResource(Res.string.playlist_move_up)
    val moveDown = stringResource(Res.string.playlist_move_down)
    val detector = remember {
        DragGestureDetector { onDragStart, onDragEnd, onDragCancel, onDrag ->
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val start = if (down.type == PointerType.Mouse) {
                    awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                } else awaitLongPressOrCancellation(down.id)
                if (start != null) {
                    releasedNormally.value = false
                    onDragStart(start.position)
                    var released = false
                    try {
                        released = drag(start.id) { onDrag(it, it.positionChange()) }
                        if (released) {
                            releasedNormally.value = true
                            onDragEnd()
                        }
                    } finally {
                        // Pointer cancellation (including resize/removal) must also end the
                        // library's drag; otherwise later gestures can remain captured.
                        if (!released) {
                            cancel.value()
                            onDragCancel()
                        }
                    }
                }
            }
        }
    }
    return Modifier.draggableHandle(
        enabled = enabled,
        onDragStarted = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            startDrag.value()
        },
        onDragStopped = {
            if (releasedNormally.value) drop.value() else cancel.value()
        },
        dragGestureDetector = detector,
    ).onPreviewKeyEvent {
        val action = when (it.key) {
            Key.DirectionUp -> onMoveUp
            Key.DirectionDown -> onMoveDown
            else -> null
        }
        if (enabled && it.type == KeyEventType.KeyDown && it.isAltPressed && action != null) {
            action()
            true
        } else false
    }.semantics {
        if (enabled) customActions = buildList {
            onMoveUp?.let { add(CustomAccessibilityAction(moveUp) { it(); true }) }
            onMoveDown?.let { add(CustomAccessibilityAction(moveDown) { it(); true }) }
        }
    }
}
