package com.skyd.podaura.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

actual val isMainThread: Boolean
    get() = (Dispatchers.Main.immediate as? CoroutineDispatcher)
        ?.isDispatchNeeded(CoroutineScope(Dispatchers.Main).coroutineContext) == false

actual val currentThreadName: String get() = Thread.currentThread().name
