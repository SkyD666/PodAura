package com.skyd.fundation.jna.mac

import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import com.sun.jna.Structure

@Suppress("SpellCheckingInspection")
object ObjCRuntime {

    val INSTANCE: NativeLibrary = NativeLibrary.getInstance("objc")

    fun sel_registerName(selectorName: String): Pointer {
        return INSTANCE.getFunction("sel_registerName").invokePointer(arrayOf(selectorName))
    }

    fun objc_getClass(className: String): Pointer {
        return INSTANCE.getFunction("objc_getClass").invokePointer(arrayOf(className))
    }

    inline fun <reified T> objc_msgSend(
        receiver: Pointer,
        selector: Pointer,
        vararg args: Any?
    ): T {
        val function = INSTANCE.getFunction("objc_msgSend")
        return function.invoke(T::class.java, arrayOf(receiver, selector, *args)) as T
    }

    inline fun <reified T : Structure> objc_msgSend_stret(
        receiver: Pointer,
        selector: Pointer,
        vararg args: Any?
    ): T {
        val function = INSTANCE.getFunction("objc_msgSend_stret")
        return function.invoke(T::class.java, arrayOf(receiver, selector, *args)) as T
    }

    fun new(className: String): Pointer {
        return objc_getClass(className)("alloc")("init")
    }

    inline fun <reified T: Structure> msgSend(receiver: Pointer, selectorName: String, vararg args: Any?): T {
        val sel = sel_registerName(selectorName)
        val isArm64 = System.getProperty("os.arch") == "aarch64"
        return if (isArm64) {
            objc_msgSend(receiver, sel, *args)
        } else {
            objc_msgSend_stret(receiver, sel, *args)
        }
    }

    operator fun Pointer.invoke(selectorName: String, vararg args: Any?): Pointer {
        val sel = sel_registerName(selectorName)
        return objc_msgSend(this, sel, *args)
    }

    fun Pointer.getUtf8String(offset: Long): String {
        return this("UTF8String").getString(offset)
    }
}
