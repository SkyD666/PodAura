package com.skyd.anivu.ext

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

fun <T> Flow<T>.catchMap(transform: FlowCollector<T>.(Throwable) -> T): Flow<T> =
    catch {
        it.printStackTrace()
        emit(transform(it))
    }

fun <T> concat(flow1: Flow<T>, flow2: Flow<T>): Flow<T> = flow {
    emitAll(flow1)
    emitAll(flow2)
}

fun <T> Flow<T>.startWith(item: T): Flow<T> = concat(flowOf(item), this)

fun <T> Flow<T>.endWith(item: T): Flow<T> = concat(this, flowOf(item))


/**
 * Projects each source value to a [Flow] which is merged in the output [Flow] only if the previous projected [Flow] has completed.
 * If value is received while there is some projected [Flow] sequence being merged, it will simply be ignored.
 *
 * This method is a shortcut for `map(transform).flattenFirst()`. See [flattenFirst].
 *
 * ### Operator fusion
 *
 * Applications of [flowOn], [buffer], and [produceIn] _after_ this operator are fused with
 * its concurrent merging so that only one properly configured channel is used for execution of merging logic.
 *
 * @param transform A transform function to apply to value that was observed while no Flow is executing in parallel.
 */
fun <T, R> Flow<T>.flatMapFirst(transform: suspend (value: T) -> Flow<R>): Flow<R> =
    map(transform).flattenFirst()

/**
 * Converts a higher-order [Flow] into a first-order [Flow] by dropping inner [Flow] while the previous inner [Flow] has not yet completed.
 */
fun <T> Flow<Flow<T>>.flattenFirst(): Flow<T> = channelFlow {
    val busy = AtomicBoolean(false)

    collect { inner ->
        if (busy.compareAndSet(false, true)) {
            // Do not pay for dispatch here, it's never necessary
            launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    inner.collect { send(it) }
                    busy.set(false)
                } catch (e: CancellationException) {
                    busy.set(false)
                }
            }
        }
    }
}

// collect with lifecycle
suspend fun <T> Flow<T>.collectIn(
    lifecycleOwner: LifecycleOwner,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    action: suspend (T) -> Unit = {},
) = flowWithLifecycle(lifecycleOwner.lifecycle, minActiveState).collect(action)

fun <T> Flow<T>.sampleWithoutFirst(timeoutMillis: Long) = merge(
    take(1), drop(1).sample(timeoutMillis)
)

/**
 * Like PV operation in semaphore, but we do V first and then P
 */
suspend fun Channel<Unit>.vThenP(receiveChannel: Channel<Unit>, block: () -> Unit) {
    send(Unit)
    block()
    return receiveChannel.receive()
}