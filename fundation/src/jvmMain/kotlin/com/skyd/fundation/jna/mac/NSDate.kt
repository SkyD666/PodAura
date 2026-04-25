package com.skyd.fundation.jna.mac

import com.skyd.fundation.jna.mac.ObjCRuntime.invoke
import com.sun.jna.Pointer

class NSDate {
    companion object {
        private val nsDateClass: Pointer = ObjCRuntime.objc_getClass("NSDate")
        fun dateWithTimeIntervalSince1970(interval: Double): Pointer {
            return nsDateClass("dateWithTimeIntervalSince1970:", interval)
        }
    }
}
