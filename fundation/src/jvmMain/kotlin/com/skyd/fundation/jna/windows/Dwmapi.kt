package com.skyd.fundation.jna.windows

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions

@Suppress("FunctionName", "SpellCheckingInspection")
interface Dwmapi : StdCallLibrary {

    fun DwmSetWindowAttribute(
        hwnd: WinDef.HWND, dwAttribute: Int, pvAttribute: Pointer, cbAttribute: Int
    ): Int

    /**
     * Extends the window frame into the client area.
     *
     * @param hwnd The handle to the window in which the frame will be extended into the client area.
     * @param margins A MARGINS structure that describes the margins to use when extending the frame into the client area.
     * @return If this function succeeds, it returns S_OK. Otherwise, it returns an HRESULT error code.
     */
    fun DwmExtendFrameIntoClientArea(hwnd: WinDef.HWND, margins: Uxtheme.MARGINS): WinDef.LRESULT

    companion object {

        val INSTANCE: Dwmapi =
            Native.load("dwmapi", Dwmapi::class.java, W32APIOptions.DEFAULT_OPTIONS)

        // Windows 10 attribute constant for enabling Immersive Dark Mode
        // Note that this constant is not in official headers for all versions,
        // and might be considered undocumented for some builds.
        const val DWMWA_USE_IMMERSIVE_DARK_MODE: Int = 20
        const val DWMWA_CAPTION_COLOR: Int = 35
    }
}
