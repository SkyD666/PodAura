package com.skyd.fundation.util

actual fun notSupport(operation: String): Nothing {
    error("macOS, not supported operation: $operation")
}
