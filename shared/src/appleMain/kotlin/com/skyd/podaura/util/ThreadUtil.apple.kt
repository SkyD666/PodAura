package com.skyd.podaura.util

import platform.Foundation.NSThread

actual val isMainThread: Boolean
    get() = NSThread.isMainThread

actual val currentThreadName: String
    get() = NSThread.currentThread().name ?: "unnamed"
