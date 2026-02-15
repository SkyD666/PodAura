package com.skyd.podaura.ui.component

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs

@Composable
fun <T> IndexBar(
    modifier: Modifier = Modifier,
    indexes: List<T>,
    onDisplay: (Int) -> String,
    onIndexChanged: (Int) -> Unit,
    onShowTip: (Boolean) -> Unit,
) {
    val offsets = remember { mutableStateMapOf<Int, Float>() }
    var isPressing by remember { mutableStateOf(false) }

    fun updateSelectedIndexIfNeeded(offset: Float) {
        val index = offsets
            .mapValues { abs(it.value - offset) }
            .entries
            .minByOrNull { it.value }
            ?.key ?: return
        onIndexChanged(index)
    }

    Column(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressing = true
                        updateSelectedIndexIfNeeded(it.y)
                        try {
                            onShowTip(true)
                            awaitRelease()
                        } catch (_: CancellationException) {
                        } finally {
                            isPressing = false
                            onShowTip(false)
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        isPressing = false
                        onShowTip(false)
                    },
                    onDragCancel = {
                        isPressing = false
                        onShowTip(false)
                    },
                    onVerticalDrag = { change, _ ->
                        isPressing = true
                        onShowTip(true)
                        updateSelectedIndexIfNeeded(change.position.y)
                    },
                )
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        indexes.forEachIndexed { i, _ ->
            Text(
                text = onDisplay(i),
                modifier = Modifier
                    .onGloballyPositioned { offsets[i] = it.boundsInParent().center.y }
                    .padding(horizontal = 3.dp)
                    .alpha(if (isPressing) 1f else 0.5f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}