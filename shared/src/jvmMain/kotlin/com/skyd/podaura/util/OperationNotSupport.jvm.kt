package com.skyd.podaura.util

actual fun notSupport(operation: String): Nothing {
    error("JVM, not supported operation: $operation")
}