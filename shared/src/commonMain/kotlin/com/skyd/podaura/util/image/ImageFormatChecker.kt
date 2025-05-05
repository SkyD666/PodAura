package com.skyd.podaura.util.image

import com.skyd.podaura.util.image.format.FormatStandard.Companion.formatStandards
import com.skyd.podaura.util.image.format.ImageFormat
import kotlinx.io.Source

object ImageFormatChecker {
    fun check(tested: Source): ImageFormat {
        var readByteArray: ByteArray? = null
        formatStandards.forEach {
            val result = it.check(tested, readByteArray)
            readByteArray = result.second
            if (result.first) {
                return it.format
            }
        }
        return ImageFormat.UNDEFINED
    }

    fun check(tested: ByteArray): ImageFormat {
        formatStandards.forEach {
            if (it.check(tested)) {
                return it.format
            }
        }
        return ImageFormat.UNDEFINED
    }
}