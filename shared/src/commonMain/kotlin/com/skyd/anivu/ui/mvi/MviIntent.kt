package com.skyd.anivu.ui.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.skyd.anivu.ext.startWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.withContext

/**
 * Immutable object which represent an view's intent.
 */
interface MviIntent

@Composable
fun <I : MviIntent, S : MviViewState, E : MviSingleEvent>
        AbstractMviViewModel<I, S, E>.getDispatcher(startWith: I?): (I) -> Unit {
    return getDispatcher(Unit, startWith = startWith)
}

@Composable
fun <I : MviIntent, S : MviViewState, E : MviSingleEvent>
        AbstractMviViewModel<I, S, E>.getDispatcher(key1: Any?, startWith: I?): (I) -> Unit {
    return getDispatcher(*arrayOf(key1), startWith = startWith)
}

@Composable
fun <I : MviIntent, S : MviViewState, E : MviSingleEvent>
        AbstractMviViewModel<I, S, E>.getDispatcher(vararg keys: Any?, startWith: I?): (I) -> Unit {
    val intentChannel = remember(*keys) { Channel<I>(Channel.UNLIMITED) }
    LaunchedEffect(*keys, intentChannel) {
        withContext(Dispatchers.Main.immediate) {
            intentChannel
                .consumeAsFlow()
                .run { startWith?.let { startWith(startWith) } ?: this }
                .collect(this@getDispatcher::processIntent)
        }
    }
    return remember(*keys, intentChannel) {
        { intent: I ->
            intentChannel.trySend(intent).getOrThrow()
        }
    }
}