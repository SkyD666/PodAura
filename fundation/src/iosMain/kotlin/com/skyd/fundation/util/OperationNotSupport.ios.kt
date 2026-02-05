package com.skyd.fundation.util

actual fun notSupport(operation: String): Nothing {
    error("iOS, not supported operation: $operation")
}
