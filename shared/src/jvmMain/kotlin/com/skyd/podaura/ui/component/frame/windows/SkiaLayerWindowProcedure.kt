package com.skyd.podaura.ui.component.frame.windows

import com.skyd.fundation.jna.windows.ExtendedUser32
import com.skyd.fundation.util.WindowsUtil
import com.sun.jna.CallbackReference
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import org.jetbrains.skiko.SkiaLayer

internal class SkiaLayerHitTestWindowProc(
    skiaLayer: SkiaLayer,
    private val hitTest: (lParam: WinDef.LPARAM) -> WindowsWindowHitResult,
) : WinUser.WindowProc, AutoCloseable {

    private val windowHandle = WinDef.HWND(Pointer(skiaLayer.windowHandle))
    internal val contentHandle = WinDef.HWND(skiaLayer.canvas.let(Native::getComponentPointer))

    private val defaultWindowProc = ExtendedUser32.INSTANCE.SetWindowLongPtr(
        contentHandle,
        WinUser.GWL_WNDPROC,
        CallbackReference.getFunctionPointer(this)
    )

    private var hitResult = WindowsWindowHitResult.CLIENT

    init {
        skiaLayer.transparency = !WindowsUtil.isWindows11OrLater()
    }

    override fun callback(
        hwnd: WinDef.HWND,
        uMsg: Int,
        wParam: WinDef.WPARAM,
        lParam: WinDef.LPARAM,
    ): WinDef.LRESULT {
        return when (uMsg) {

            ExtendedUser32.WM_NCHITTEST -> {
                hitResult = hitTest(lParam)

                when (hitResult) {
                    WindowsWindowHitResult.CLIENT,
                    WindowsWindowHitResult.CAPTION_MAX,
                    WindowsWindowHitResult.CAPTION_MIN,
                    WindowsWindowHitResult.CAPTION_CLOSE -> hitResult

                    else -> WindowsWindowHitResult.TRANSPARENT
                }.let { WinDef.LRESULT(it.value.toLong()) }
            }

            ExtendedUser32.WM_NCMOUSEMOVE -> {
                ExtendedUser32.INSTANCE.SendMessage(
                    contentHandle,
                    ExtendedUser32.WM_MOUSEMOVE,
                    wParam,
                    lParam
                )
                WinDef.LRESULT(0)
            }

            ExtendedUser32.WM_NCLBUTTONDOWN -> {
                ExtendedUser32.INSTANCE.SendMessage(
                    contentHandle,
                    ExtendedUser32.WM_LBUTTONDOWN,
                    wParam,
                    lParam
                )
                WinDef.LRESULT(0)
            }

            ExtendedUser32.WM_NCLBUTTONUP -> {
                ExtendedUser32.INSTANCE.SendMessage(
                    contentHandle,
                    ExtendedUser32.WM_LBUTTONUP,
                    wParam,
                    lParam
                )
                WinDef.LRESULT(0)
            }

            ExtendedUser32.WM_NCRBUTTONUP -> {
                ExtendedUser32.INSTANCE.SendMessage(
                    windowHandle,
                    uMsg,
                    wParam,
                    lParam
                )
                WinDef.LRESULT(0)
            }

            else -> {
                ExtendedUser32.INSTANCE.CallWindowProc(
                    defaultWindowProc,
                    hwnd,
                    uMsg,
                    wParam,
                    lParam
                ) ?: WinDef.LRESULT(0)
            }
        }
    }

    override fun close() {
        ExtendedUser32.INSTANCE.SetWindowLongPtr(
            contentHandle,
            WinUser.GWL_WNDPROC,
            defaultWindowProc
        )
    }
}
