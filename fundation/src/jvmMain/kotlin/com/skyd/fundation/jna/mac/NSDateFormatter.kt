package com.skyd.fundation.jna.mac

import com.skyd.fundation.jna.mac.ObjCRuntime.Companion.getUtf8String
import com.skyd.fundation.jna.mac.ObjCRuntime.Companion.invoke
import com.sun.jna.Pointer

class NSDateFormatter {
    object Style {
        const val NO = 0L
        const val SHORT = 1L
        const val MEDIUM = 2L
    }

    private val formatter: Pointer = ObjCRuntime.new("NSDateFormatter")

    fun setDateStyle(style: Long) {
        formatter("setDateStyle:", style)
    }

    fun setDoesRelativeDateFormatting(relative: Boolean) {
        formatter("setDoesRelativeDateFormatting:", relative)
    }

    fun setTimeStyle(style: Long) {
        formatter("setTimeStyle:", style)
    }

    fun stringFromDate(nsDate: Pointer): String {
        return formatter("stringFromDate:", nsDate).getUtf8String(0)
    }
}