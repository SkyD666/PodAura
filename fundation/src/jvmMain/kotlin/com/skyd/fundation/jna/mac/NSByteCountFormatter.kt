package com.skyd.fundation.jna.mac

import com.skyd.fundation.jna.mac.ObjCRuntime.Companion.getUtf8String
import com.skyd.fundation.jna.mac.ObjCRuntime.Companion.invoke
import com.sun.jna.Pointer

class NSByteCountFormatter {
    object CountStyle {
        const val FILE = 0L
    }

    companion object {
        private val nsByteCountFormatterClass: Pointer =
            ObjCRuntime.INSTANCE.objc_getClass("NSByteCountFormatter")

        fun stringFromByteCount(byteCount: Long, countStyle: Long): String {
            return nsByteCountFormatterClass(
                "stringFromByteCount:countStyle:",
                byteCount,
                countStyle
            ).getUtf8String(0)
        }
    }
}