package com.skyd.podaura.ext

import platform.Foundation.NSString
import platform.Foundation.stringWithFormat

actual fun Long.format(minLength: Int, leading: Char): String {
    return NSString.stringWithFormat("%${leading}${minLength}d", this)
}
