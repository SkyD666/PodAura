package com.skyd.podaura.ui.component.frame.windows

import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import com.skyd.fundation.jna.windows.Dwmapi
import com.skyd.fundation.jna.windows.Stdint
import com.skyd.fundation.jna.windows.User32Extend
import com.skyd.fundation.jna.windows.Uxtheme
import com.skyd.fundation.jna.windows.WinUserExtend
import com.skyd.fundation.util.WindowsUtil
import com.skyd.podaura.util.findSkiaLayer
import com.sun.jna.CallbackReference
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinReg
import com.sun.jna.platform.win32.WinUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal open class BasicWindowProc(
    window: ComposeWindow
) : WinUser.WindowProc, AutoCloseable {

    val windowHandle: WinDef.HWND = WinDef.HWND(
        Pointer(window.windowHandle)
    )

    val accentColor: StateFlow<Color>
        field = MutableStateFlow(currentAccentColor())

    private val defaultWindowProc = User32Extend.INSTANCE.SetWindowLongPtr(
        windowHandle,
        WinUser.GWL_WNDPROC,
        CallbackReference.getFunctionPointer(this),
    )

    override fun callback(
        hwnd: WinDef.HWND,
        uMsg: Int,
        wParam: WinDef.WPARAM,
        lParam: WinDef.LPARAM
    ): WinDef.LRESULT {
        if (uMsg == WinUserExtend.WM_SETTINGCHANGE) {
            val changedKey = Pointer(lParam.toLong()).getWideString(0)
            // Theme changed for color and darkTheme
            if (changedKey == "ImmersiveColorSet") {
                accentColor.tryEmit(currentAccentColor())
                onThemeChanged()
            }
        }
        return callDefWindowProc(hwnd, uMsg, wParam, lParam)
    }

    protected open fun onThemeChanged() {}

    private fun callDefWindowProc(
        @Suppress("SpellCheckingInspection") hwnd: WinDef.HWND,
        uMsg: Int,
        wParam: WinDef.WPARAM,
        lParam: WinDef.LPARAM
    ): WinDef.LRESULT {
        return User32Extend.INSTANCE.CallWindowProc(
            defaultWindowProc, hwnd, uMsg, wParam, lParam
        )
    }

    private fun currentAccentColor(): Color {
        val value = Advapi32Util.registryGetIntValue(
            WinReg.HKEY_CURRENT_USER,
            "SOFTWARE\\Microsoft\\Windows\\DWM",
            "AccentColor",
        ).toLong()
        val alpha = (value and 0xFF000000)
        val green = (value and 0xFF).shl(16)
        val blue = (value and 0xFF00)
        val red = (value and 0xFF0000).shr(16)
        return Color((alpha or green or blue or red).toInt())
    }

    override fun close() {
        User32Extend.INSTANCE.SetWindowLongPtr(
            windowHandle,
            WinUser.GWL_WNDPROC,
            defaultWindowProc
        )
    }
}

internal class ExtendedTitleBarWindowProc(
    window: ComposeWindow
) : BasicWindowProc(window) {

    private var childHitTestOwner: WindowsWindowHitTestOwner? = null

    val windowIsActive: StateFlow<Boolean>
        field = MutableStateFlow(User32Extend.INSTANCE.GetActiveWindow() == windowHandle)

    val frameIsColorful: StateFlow<Boolean>
        field = MutableStateFlow(isAccentColorWindowFrame())

    private var hitTestResult = WindowsWindowHitResult.CLIENT

    private val skiaLayerWindowProc: SkiaLayerHitTestWindowProc? =
        window.findSkiaLayer()?.let { SkiaLayerHitTestWindowProc(it, ::hitTest) }

    private var isMaximized: Boolean = User32Extend.INSTANCE.isWindowInMaximized(windowHandle)
    private var dpi: WinDef.UINT = WinDef.UINT(0)
    private var width: Int = 0
    private var height: Int = 0
    private var frameX: Int = 0
    private var frameY: Int = 0
    private var edgeX: Int = 0
    private var edgeY: Int = 0
    private var padding: Int = 0

    init {
        Dwmapi.INSTANCE.DwmExtendFrameIntoClientArea(
            windowHandle,
            Uxtheme.MARGINS(-1, -1, -1, -1)
        )
        windowHandle.updateWindowStyle { it and WinUser.WS_SYSMENU.inv() }
        eraseWindowBackground()
    }

    /***
     * @param x, the horizontal offset relative to the client area.
     * @param y, the vertical offset relative to the client area.
     */
    private fun hitTestWindowResizerBorder(x: Int, y: Int): WindowsWindowHitResult {
        // Force update window info.
        updateWindowInfo()
        // If window not contains the border, return NOWHERE.
        val currentStyle = User32.INSTANCE.GetWindowLong(windowHandle, WinUser.GWL_STYLE)
        if (currentStyle and WinUser.WS_CAPTION == 0) {
            return WindowsWindowHitResult.NOWHERE
        }
        val horizontalPadding = frameX
        val verticalPadding = frameY
        return when {
            x <= horizontalPadding && y > verticalPadding && y < height - verticalPadding ->
                WindowsWindowHitResult.BORDER_LEFT

            x <= horizontalPadding && y <= verticalPadding ->
                WindowsWindowHitResult.BORDER_TOP_LEFT

            x <= horizontalPadding ->
                WindowsWindowHitResult.BORDER_BOTTOM_LEFT

            y <= verticalPadding && x > horizontalPadding && x < width - horizontalPadding ->
                WindowsWindowHitResult.BORDER_TOP

            y <= verticalPadding ->
                WindowsWindowHitResult.BORDER_TOP_RIGHT

            x >= width - horizontalPadding && y > verticalPadding && y < height - verticalPadding ->
                WindowsWindowHitResult.BORDER_RIGHT

            x >= width - horizontalPadding ->
                WindowsWindowHitResult.BORDER_BOTTOM_RIGHT

            y >= height - verticalPadding && x > horizontalPadding && x < width - horizontalPadding ->
                WindowsWindowHitResult.BORDER_BOTTOM

            y >= height - verticalPadding ->
                WindowsWindowHitResult.BORDER_BOTTOM_RIGHT

            else -> WindowsWindowHitResult.NOWHERE
        }
    }

    private fun hitTest(lParam: WinDef.LPARAM): WindowsWindowHitResult {
        return lParam.usePoint { x, y ->
            if (!isMaximized) {
                hitTestResult = hitTestWindowResizerBorder(x, y)
                if (isHitWindowResizer(hitTestResult)) {
                    return@usePoint hitTestResult
                }
            }
            hitTestResult = childHitTestOwner
                ?.hitTest(x.toFloat(), y.toFloat())
                ?: WindowsWindowHitResult.CLIENT

            hitTestResult
        }
    }

    override fun callback(
        hwnd: WinDef.HWND,
        uMsg: Int,
        wParam: WinDef.WPARAM,
        lParam: WinDef.LPARAM
    ): WinDef.LRESULT {
        return when (uMsg) {
            // Returns 0 to make the window not draw the non-client area (title bar and border)
            // thus effectively making all the window our client area
            WinUserExtend.WM_NCCALCSIZE -> {
                if (wParam.toInt() == 0) {
                    super.callback(hwnd, uMsg, wParam, lParam)
                } else {
                    // this behavior is call full screen mode
                    val style = User32Extend.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_STYLE)
                    if (style and (WinUser.WS_CAPTION or WinUser.WS_THICKFRAME) == 0) {
                        frameX = 0
                        frameY = 0
                        edgeX = 0
                        edgeY = 0
                        padding = 0
                        isMaximized = User32Extend.INSTANCE.isWindowInMaximized(hwnd)
                        return WinDef.LRESULT(0)
                    }

                    dpi = User32Extend.INSTANCE.GetDpiForWindow(hwnd)
                    frameX = User32Extend.INSTANCE.GetSystemMetricsForDpi(WinUser.SM_CXFRAME, dpi)
                    frameY = User32Extend.INSTANCE.GetSystemMetricsForDpi(WinUser.SM_CYFRAME, dpi)
                    edgeX = User32Extend.INSTANCE.GetSystemMetricsForDpi(WinUser.SM_CXEDGE, dpi)
                    edgeY = User32Extend.INSTANCE.GetSystemMetricsForDpi(WinUser.SM_CYEDGE, dpi)
                    padding = User32Extend.INSTANCE.GetSystemMetricsForDpi(
                        WinUser.SM_CXPADDEDBORDER,
                        dpi
                    )
                    isMaximized = User32Extend.INSTANCE.isWindowInMaximized(hwnd)
                    val params = Structure.newInstance(
                        WinUserExtend.NCCALCSIZE_PARAMS::class.java,
                        Pointer(lParam.toLong())
                    )
                    params.read()
                    params.rgrc[0]?.apply {
                        left += if (isMaximized) {
                            frameX + padding
                        } else {
                            edgeX
                        }
                        right -= if (isMaximized) {
                            frameX + padding
                        } else {
                            edgeX
                        }
                        bottom -= if (isMaximized) {
                            padding + frameX
                        } else {
                            edgeY
                        }
                        top += if (isMaximized) {
                            padding + frameX
                        } else {
                            0
                        }
                    }
                    params.write()
                    WinDef.LRESULT(0)
                }
            }

            WinUserExtend.WM_NCHITTEST -> {
                // Skip resizer border hit test if window is maximized
                if (!isMaximized) {
                    val callResult = lParam.usePoint(::hitTestWindowResizerBorder)
                    if (isHitWindowResizer(callResult)) {
                        hitTestResult = callResult
                    }
                }
                WinDef.LRESULT(hitTestResult.value.toLong())
            }

            WinUserExtend.WM_NCRBUTTONUP -> {
                if (wParam.toInt() == WindowsWindowHitResult.CAPTION.value) {
                    val oldStyle = User32Extend.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_STYLE)
                    User32Extend.INSTANCE.SetWindowLong(
                        hwnd,
                        WinUser.GWL_STYLE,
                        oldStyle or WinUser.WS_SYSMENU
                    )
                    val menu = User32Extend.INSTANCE.GetSystemMenu(hwnd, false)
                    User32Extend.INSTANCE.SetWindowLong(hwnd, WinUser.GWL_STYLE, oldStyle)
                    isMaximized = User32Extend.INSTANCE.isWindowInMaximized(hwnd)
                    if (menu != null) {
                        // Update menu items state.
                        val menuItemInfo = WinUserExtend.MENUITEMINFOW().apply {
                            cbSize = this.size()
                            fMask = WinUserExtend.MIIM_STATE
                            fType = WinUserExtend.MFT_STRING
                        }

                        updateMenuItemInfo(
                            menu,
                            menuItemInfo,
                            WinUserExtend.SC_RESTORE,
                            isMaximized
                        )
                        updateMenuItemInfo(menu, menuItemInfo, WinUserExtend.SC_MOVE, !isMaximized)
                        updateMenuItemInfo(menu, menuItemInfo, WinUserExtend.SC_SIZE, !isMaximized)
                        updateMenuItemInfo(menu, menuItemInfo, WinUser.SC_MINIMIZE, true)
                        updateMenuItemInfo(menu, menuItemInfo, WinUser.SC_MAXIMIZE, !isMaximized)
                        updateMenuItemInfo(menu, menuItemInfo, WinUserExtend.SC_CLOSE, true)

                        // Set default menu item.
                        User32Extend.INSTANCE.SetMenuDefaultItem(
                            menu,
                            Stdint.WINT_MAX,
                            false
                        )

                        // Get cursor position.
                        val lParamValue = lParam.toInt()
                        val x = lowWord(lParamValue)
                        val y = highWord(lParamValue)

                        // Show menu and get user selection.
                        val ret = User32Extend.INSTANCE.TrackPopupMenu(
                            menu,
                            WinUserExtend.TPM_RETURNCMD,
                            x,
                            y,
                            0,
                            hwnd,
                            null
                        )
                        menuItemInfo.clear()
                        if (ret != 0) {
                            // Send WM_SYSCOMMAND message.
                            User32Extend.INSTANCE.PostMessage(
                                hwnd,
                                WinUser.WM_SYSCOMMAND,
                                WinDef.WPARAM(ret.toLong()),
                                WinDef.LPARAM(0),
                            )
                        }
                    }
                }
                super.callback(hwnd, uMsg, wParam, lParam)
            }

            WinUser.WM_SIZE -> {
                val lParamValue = lParam.toInt()
                width = lowWord(lParamValue)
                height = highWord(lParamValue)
                super.callback(hwnd, uMsg, wParam, lParam)
            }

            WinUserExtend.WM_ACTIVATE -> {
                windowIsActive.tryEmit(wParam.toInt() != WinUserExtend.WA_INACTIVE)
                super.callback(hwnd, uMsg, wParam, lParam)
            }

            WinUserExtend.WM_NCMOUSEMOVE -> {
                skiaLayerWindowProc?.let {
                    User32Extend.INSTANCE.PostMessage(it.contentHandle, uMsg, wParam, lParam)
                }
                super.callback(hwnd, uMsg, wParam, lParam)
            }

            WinUserExtend.WM_SETTINGCHANGE -> {
                val changedKey = Pointer(lParam.toLong()).getWideString(0)
                // Theme changed for color and darkTheme
                if (changedKey == "ImmersiveColorSet") {
                    frameIsColorful.tryEmit(isAccentColorWindowFrame())
                }
                super.callback(hwnd, uMsg, wParam, lParam)
            }

            else -> super.callback(hwnd, uMsg, wParam, lParam)
        }
    }

    internal fun updateChildHitTestProvider(owner: WindowsWindowHitTestOwner) {
        childHitTestOwner = owner
    }

    override fun onThemeChanged() {
        frameIsColorful.tryEmit(isAccentColorWindowFrame())
    }

    private fun updateMenuItemInfo(
        menu: WinDef.HMENU,
        menuItemInfo: WinUserExtend.MENUITEMINFOW,
        item: Int,
        enabled: Boolean
    ) {
        menuItemInfo.fState = if (enabled) WinUserExtend.MFS_ENABLED else WinUserExtend.MFS_DISABLED
        User32Extend.INSTANCE.SetMenuItemInfo(menu, item, false, menuItemInfo)
    }

    // Workaround for background erase.
    private fun eraseWindowBackground() {
        if (!WindowsUtil.isWindows11OrLater()) {
            val flag = WinUser.SWP_NOZORDER or WinUserExtend.SWP_NOACTIVATE or
                    WinUser.SWP_FRAMECHANGED or WinUser.SWP_NOMOVE or
                    WinUser.SWP_NOSIZE or WinUser.SWP_ASYNCWINDOWPOS
            User32Extend.INSTANCE.SetWindowPos(
                windowHandle,
                null,
                0,
                0,
                0,
                0,
                flag or WinUser.SWP_HIDEWINDOW
            )
            User32Extend.INSTANCE.SetWindowPos(
                windowHandle,
                null,
                0,
                0,
                0,
                0,
                flag or WinUser.SWP_SHOWWINDOW
            )
        }

    }

    private fun highWord(value: Int): Int = (value shr 16) and 0xFFFF

    private fun lowWord(value: Int): Int = value and 0xFFFF

    private fun isHitWindowResizer(hitResult: WindowsWindowHitResult): Boolean = when (hitResult) {
        WindowsWindowHitResult.BORDER_TOP,
        WindowsWindowHitResult.BORDER_LEFT,
        WindowsWindowHitResult.BORDER_RIGHT,
        WindowsWindowHitResult.BORDER_BOTTOM,
        WindowsWindowHitResult.BORDER_TOP_LEFT,
        WindowsWindowHitResult.BORDER_TOP_RIGHT,
        WindowsWindowHitResult.BORDER_BOTTOM_LEFT,
        WindowsWindowHitResult.BORDER_BOTTOM_RIGHT -> true

        else -> false
    }

    private fun updateWindowInfo() {
        dpi = User32Extend.INSTANCE.GetDpiForWindow(windowHandle)
        frameX = User32Extend.INSTANCE.GetSystemMetricsForDpi(WinUser.SM_CXFRAME, dpi)
        frameY = User32Extend.INSTANCE.GetSystemMetricsForDpi(WinUser.SM_CYFRAME, dpi)

        val rect = WinDef.RECT()
        if (User32Extend.INSTANCE.GetWindowRect(windowHandle, rect)) {
            rect.read()
            width = rect.right - rect.left
            height = rect.bottom - rect.top
        }
        rect.clear()
    }

    private inline fun <T> WinDef.LPARAM.usePoint(crossinline block: (x: Int, y: Int) -> T): T {
        val intValue = toInt()
        val x = lowWord(intValue).toShort().toInt()
        val y = highWord(intValue).toShort().toInt()
        val point = WinDef.POINT(x, y)
        User32Extend.INSTANCE.ScreenToClient(windowHandle, point)
        point.read()
        val result = block(point.x, point.y)
        point.clear()
        return result
    }

    private inline fun WinDef.HWND.updateWindowStyle(block: (old: Int) -> Int) {
        val oldStyle = User32Extend.INSTANCE.GetWindowLong(this, WinUser.GWL_STYLE)
        User32Extend.INSTANCE.SetWindowLong(this, WinUser.GWL_STYLE, block(oldStyle))
    }

    private fun User32.isWindowInMaximized(hWnd: WinDef.HWND): Boolean {
        val placement = WinUser.WINDOWPLACEMENT()
        val result = GetWindowPlacement(hWnd, placement).booleanValue() &&
                placement.showCmd == WinUser.SW_SHOWMAXIMIZED
        placement.clear()
        return result
    }

    private fun isAccentColorWindowFrame(): Boolean {
        return Advapi32Util.registryGetIntValue(
            WinReg.HKEY_CURRENT_USER,
            "SOFTWARE\\Microsoft\\Windows\\DWM",
            "ColorPrevalence",
        ) != 0
    }

    override fun close() {
        skiaLayerWindowProc?.close()
        windowHandle.updateWindowStyle { it or WinUser.WS_SYSMENU }
        super.close()
    }
}
