package com.skyd.fundation.jna.mac

import com.skyd.fundation.jna.mac.ObjCRuntime.Companion.getUtf8String
import com.skyd.fundation.jna.mac.ObjCRuntime.Companion.invoke
import com.sun.jna.Pointer

class NSDateComponentsFormatter {
    object ZeroFormattingBehavior {
        const val BEHAVIOR_PAD = 1L shl 16
    }

    object UnitsStyle {
        const val POSITIONAL = 0L
    }

    private val formatter: Pointer = ObjCRuntime.new("NSDateComponentsFormatter")

    fun setUnitsStyle(style: Long) {
        formatter("setUnitsStyle:", style)
    }

    fun setAllowedUnits(allowed: Long) {
        formatter("setAllowedUnits:", allowed)
    }

    fun setZeroFormattingBehavior(behavior: Long) {
        formatter("setZeroFormattingBehavior:", behavior)
    }

    fun stringFromTimeInterval(interval: Double): String {
        return formatter("stringFromTimeInterval:", interval).getUtf8String(0)
    }

}