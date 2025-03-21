package com.skyd.anivu.base.mvi

import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyd.anivu.BuildConfig
import com.skyd.anivu.util.debug
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
    if (BuildConfig.DEBUG) {
        check(Looper.getMainLooper() === Looper.myLooper()) {
            "Expected to be called on the main thread but was " + Thread.currentThread().name
        }
    }
}

suspend fun debugCheckImmediateMainDispatcher() {
    if (BuildConfig.DEBUG) {
        val interceptor = currentCoroutineContext()[ContinuationInterceptor]
        Log.d(
            "###",
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

    protected val logTag by lazy(PUBLICATION) {
        (rawLogTag ?: this::class.java.simpleName).let { tag: String ->
            // Tag length limit was removed in API 26.
            if (tag.length <= MAX_TAG_LENGTH || Build.VERSION.SDK_INT >= 26) {
                tag
            } else {
                tag.take(MAX_TAG_LENGTH)
            }
        }
    }

    private val eventChannel = Channel<E>(Channel.UNLIMITED)
    private val intentMutableFlow = MutableSharedFlow<I>(extraBufferCapacity = Int.MAX_VALUE)

    final override val singleEvent: Flow<E> = eventChannel.receiveAsFlow()

    @MainThread
    final override suspend fun processIntent(intent: I) {
        debugCheckMainThread()
        debugCheckImmediateMainDispatcher()

        Log.i(logTag, "processIntent: $intent")
        check(intentMutableFlow.tryEmit(intent)) { "Failed to emit intent: $intent" }
    }

    @CallSuper
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
        debugCheckImmediateMainDispatcher()

        eventChannel.trySend(event)
            .onSuccess { debug { Log.i(logTag, "sendEvent: event=$event") } }
            .onFailure { debug { Log.e(logTag, "$it. Failed to send event: $event") } }
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
        if (BuildConfig.DEBUG) {
            onEach { Log.i(logTag, ">>> $subject: $it") }
        } else {
            this
        }

    protected fun <T> SharedFlow<T>.debugLog(subject: String): SharedFlow<T> =
        if (BuildConfig.DEBUG) {
            val self = this

            object : SharedFlow<T> by self {
                val subscriberCount = AtomicInteger(0)

                override suspend fun collect(collector: FlowCollector<T>): Nothing {
                    val count = subscriberCount.getAndIncrement()

                    self.collect {
                        Log.i(logTag, ">>> $subject ~ $count: $it")
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
