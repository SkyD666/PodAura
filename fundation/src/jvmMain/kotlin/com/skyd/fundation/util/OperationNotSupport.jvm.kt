package com.skyd.fundation.util

actual fun notSupport(operation: String): Nothing {
    error("JVM, not supported operation: $operation")
}