package com.skyd.podaura.util

import android.os.Looper

actual val isMainThread: Boolean = Looper.getMainLooper() === Looper.myLooper()

actual val currentThreadName: String = Thread.currentThread().name