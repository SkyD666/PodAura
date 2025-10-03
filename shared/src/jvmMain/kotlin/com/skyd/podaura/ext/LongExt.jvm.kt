package com.skyd.podaura.ext

actual fun Long.format(minLength: Int, leading: Char): String {
    return "%${leading}${minLength}d".format(this)
}