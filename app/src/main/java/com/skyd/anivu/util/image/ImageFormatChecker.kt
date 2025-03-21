package com.skyd.anivu.util.image

import com.skyd.anivu.util.image.format.FormatStandard.Companion.formatStandards
import com.skyd.anivu.util.image.format.ImageFormat
import java.io.InputStream

object ImageFormatChecker {
    fun check(tested: InputStream): ImageFormat {
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