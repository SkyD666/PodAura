package com.skyd.anivu.base.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.skyd.anivu.ext.collectIn
import kotlinx.coroutines.flow.Flow

/**
 * Immutable object which represents a single event
 * like snack bar message, navigation event, a dialog trigger, etc...
 */
interface MviSingleEvent

@Composable
fun <T : MviSingleEvent> MviEventListener(
    eventFlow: Flow<T>,
    onEach: suspend (event: T) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        eventFlow.collectIn(lifecycleOwner, action = onEach)
    }
}