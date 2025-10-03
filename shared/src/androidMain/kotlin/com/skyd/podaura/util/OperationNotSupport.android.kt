package com.skyd.podaura.util

actual fun notSupport(operation: String): Nothing {
    error("Android, not supported operation: $operation")
}