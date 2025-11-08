package com.skyd.fundation.jna.mac

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer

@Suppress("FunctionName")
interface ObjCRuntime : Library {
    fun objc_getClass(className: String): Pointer
    fun sel_registerName(selectorName: String): Pointer
    fun objc_msgSend(receiver: Pointer, selector: Pointer): Pointer
    fun objc_msgSend(receiver: Pointer, selector: Pointer, arg: Any?): Pointer

    companion object {
        val INSTANCE: ObjCRuntime = Native.load<ObjCRuntime>("objc", ObjCRuntime::class.java)

        fun new(className: String): Pointer {
            return INSTANCE.objc_getClass(className)("alloc")("init")
        }

        operator fun Pointer.invoke(selectorName: String, selector: Any?): Pointer {
            val sel = INSTANCE.sel_registerName(selectorName)
            return INSTANCE.objc_msgSend(this, sel, selector)
        }

        operator fun Pointer.invoke(selectorName: String): Pointer {
            val sel = INSTANCE.sel_registerName(selectorName)
            return INSTANCE.objc_msgSend(this, sel)
        }

        fun Pointer.getUtf8String(offset: Long): String {
            return this("UTF8String").getString(offset)
        }
    }
}