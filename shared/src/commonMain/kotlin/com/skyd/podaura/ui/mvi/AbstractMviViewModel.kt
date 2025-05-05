package com.skyd.podaura.ui.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.skyd.podaura.util.currentThreadName
import com.skyd.podaura.util.debug
import com.skyd.podaura.util.isDebug
import com.skyd.podaura.util.isMainThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.atomic.AtomicInteger
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.coroutines.ContinuationInterceptor

private fun debugCheckMainThread() {
    if (isDebug) {
        check(isMainThread) { "Expected to be called on the main thread but was $currentThreadName" }
    }
}

suspend fun debugCheckImmediateMainDispatcher(log: Logger) {
    if (isDebug) {
        val interceptor = currentCoroutineContext()[ContinuationInterceptor]
        log.d(
            "debugCheckImmediateMainDispatcher: $interceptor, ${Dispatchers.Main.immediate}, ${Dispatchers.Main}"
        )

        check(interceptor === Dispatchers.Main.immediate) {
            "Expected ContinuationInterceptor to be Dispatchers.Main.immediate but was $interceptor"
        }
    }
}

abstract class AbstractMviViewModel<I : MviIntent, S : MviViewState, E : MviSingleEvent> :
    MviViewModel<I, S, E>, ViewModel() {
    protected open val rawLogTag: String? = null

    private val log by lazy(PUBLICATION) {
        Logger.withTag((rawLogTag ?: this::class.java.simpleName).take(MAX_TAG_LENGTH))
    }

    private val eventChannel = Channel<E>(Channel.UNLIMITED)
    private val intentMutableFlow = MutableSharedFlow<I>(extraBufferCapacity = Int.MAX_VALUE)

    final override val singleEvent: Flow<E> = eventChannel.receiveAsFlow()

    final override suspend fun processIntent(intent: I) {
        debugCheckMainThread()
        debugCheckImmediateMainDispatcher(log)

        log.i("processIntent: $intent")
        check(intentMutableFlow.tryEmit(intent)) { "Failed to emit intent: $intent" }
    }

    override fun onCleared() {
        super.onCleared()
        eventChannel.close()
    }

    // Send event and access intent flow.

    /**
     * Must be called in [MainCoroutineDispatcher.immediate],
     * otherwise it will throw an exception.
     *
     * If you want to send an event from other [kotlinx.coroutines.CoroutineDispatcher],
     * use `withContext(Dispatchers.Main.immediate) { sendEvent(event) }`.
     */
    protected suspend fun sendEvent(event: E) {
        debugCheckMainThread()
        debugCheckImmediateMainDispatcher(log)

        eventChannel.trySend(event)
            .onSuccess { debug { log.i("sendEvent: event=$event") } }
            .onFailure { debug { log.e("$it. Failed to send event: $event") } }
            .getOrThrow()
    }

    protected val intentFlow: Flow<I> get() = intentMutableFlow.asSharedFlow()

    // Extensions on Flow using viewModelScope.

    protected fun Flow<S>.toState(initialValue: S) = stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        initialValue
    )

    protected fun <T> Flow<T>.debugLog(subject: String): Flow<T> =
        if (isDebug) {
            onEach { log.i(">>> $subject: $it") }
        } else {
            this
        }

    protected fun <T> SharedFlow<T>.debugLog(subject: String): SharedFlow<T> =
        if (isDebug) {
            val self = this

            object : SharedFlow<T> by self {
                val subscriberCount = AtomicInteger(0)

                override suspend fun collect(collector: FlowCollector<T>): Nothing {
                    val count = subscriberCount.getAndIncrement()

                    self.collect {
                        log.i(">>> $subject ~ $count: $it")
                        collector.emit(it)
                    }
                }
            }
        } else {
            this
        }

    private companion object {
        private const val MAX_TAG_LENGTH = 23
    }
}
