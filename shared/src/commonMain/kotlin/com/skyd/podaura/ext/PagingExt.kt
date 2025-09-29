package com.skyd.podaura.ext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter

fun <T : Any> LazyPagingItems<T>.safeItemKey(
    default: (Int) -> Any = { it },
    key: ((item: @JvmSuppressWildcards T) -> Any)? = null,
): (index: Int) -> Any {
    return { index ->
        if (index >= itemCount) {
            default(index)
        } else {
            itemKey(key).invoke(index)
        }
    }
}

val <T : Any> LazyPagingItems<T>.lastIndex
    get() = itemCount - 1

fun <T : Any> LazyPagingItems<T>.getOrNull(index: Int): T? {
    return if (index !in 0..<itemCount) {
        null
    } else {
        get(index)
    }
}

@Composable
fun <T : Any, U> LazyPagingItems<T>.rememberUpdateSemaphore(
    default: U?,
    sendData: suspend (LazyPagingItems<T>) -> U? = { default },
): Channel<U> {
    val semaphoreChannel = remember { Channel<U>(capacity = Channel.UNLIMITED) }
    LaunchedEffect(Unit) {
        snapshotFlow { itemSnapshotList.items }
            .distinctUntilChanged()
            .filter { loadState.refresh is LoadState.NotLoading }
            .drop(1)
            .collect {
                sendData(this@rememberUpdateSemaphore)?.let {
                    semaphoreChannel.trySend(it)
                }
            }
    }
    return semaphoreChannel
}