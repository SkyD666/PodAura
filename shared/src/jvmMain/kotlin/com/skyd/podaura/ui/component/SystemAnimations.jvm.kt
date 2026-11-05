package com.skyd.podaura.ui.component

import com.skyd.fundation.jna.mac.ObjCRuntime
import com.skyd.fundation.jna.mac.ObjCRuntime.invoke
import com.skyd.fundation.util.Platform
import com.skyd.fundation.util.platform
import com.sun.jna.Function
import com.sun.jna.NativeLibrary
import com.sun.jna.ptr.IntByReference
import java.awt.Toolkit

internal actual fun platformAnimationsEnabled(): Boolean = try {
    when (platform) {
        Platform.Windows -> WindowsAnimations.enabled()
        Platform.macOS_Jvm -> MacAnimations.enabled()
        Platform.Linux -> linuxAnimationsEnabled(Toolkit.getDefaultToolkit()::getDesktopProperty)
        else -> true
    }
} catch (_: RuntimeException) {
    true
} catch (_: LinkageError) {
    true
}

internal fun linuxAnimationsEnabled(readProperty: (String) -> Any?): Boolean =
    (readProperty("gnome.Gtk/EnableAnimations") as? Number)?.toInt() != 0

private object WindowsAnimations {
    private val systemParametersInfo = NativeLibrary.getInstance("user32")
        .getFunction("SystemParametersInfoW", Function.ALT_CONVENTION)

    fun enabled(): Boolean {
        val enabled = IntByReference()
        // SPI_GETCLIENTAREAANIMATION: Windows accessibility's "Animation effects" switch.
        val success = systemParametersInfo.invokeInt(arrayOf(0x1042, 0, enabled, 0)) != 0
        return !success || enabled.value != 0
    }
}

private object MacAnimations {
    // Keep AppKit loaded while its objects are in use.
    private val appKit = NativeLibrary.getInstance("AppKit")
    private val workspace = ObjCRuntime.objc_getClass("NSWorkspace")("sharedWorkspace")
    private val reduceMotion =
        ObjCRuntime.sel_registerName("accessibilityDisplayShouldReduceMotion")

    fun enabled(): Boolean = ObjCRuntime.objc_msgSend<Byte>(workspace, reduceMotion) == 0.toByte()
}
