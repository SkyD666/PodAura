package com.skyd.podaura.util

expect val isDebug: Boolean

inline fun debug(block: () -> Unit) {
    if (isDebug) block()
}