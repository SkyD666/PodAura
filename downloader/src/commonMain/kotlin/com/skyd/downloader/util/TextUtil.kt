package com.skyd.downloader.util

import com.skyd.fundation.ext.format

object TextUtil {
    fun getSpeedText(speedInBPerMs: Float): String {
        var value = speedInBPerMs * 1000
        val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s")
        var unitIndex = 0

        while (value >= 500 && unitIndex < units.size - 1) {
            value /= 1024
            unitIndex++
        }

        return "${value.format(2)} ${units[unitIndex]}"
    }

    fun getTotalLengthText(lengthInBytes: Long): String {
        var value = lengthInBytes.toFloat()
        val units = arrayOf("B", "KB", "MB", "GB")
        var unitIndex = 0

        while (value >= 500 && unitIndex < units.size - 1) {
            value /= 1024
            unitIndex++
        }

        return "${value.format(2)} ${units[unitIndex]}"
    }
}