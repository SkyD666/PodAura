package com.skyd.anivu.util

expect val isDebug: Boolean

inline fun debug(block: () -> Unit) {
    if (isDebug) block()
}