package com.skyd.podaura.ext

actual fun Long.fileSize(): String {
    TODO("Not yet implemented")
}

actual fun Float.toPercentage(point: Int): String {
    assert(point >= 0) { "Float.toPercentage error, point should be positive" }
    return "%.${point}f%%".format(this * 100)
}