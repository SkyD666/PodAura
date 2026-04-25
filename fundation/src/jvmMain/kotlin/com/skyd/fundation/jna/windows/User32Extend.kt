package com.skyd.fundation.jna.windows

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.win32.W32APIOptions

@Suppress("FunctionName", "LocalVariableName", "SpellCheckingInspection")
interface User32Extend : User32 {

    /**
     * Is the window zoomed (maximized) or not?
     *
     * @param hWnd native window handle
     * @return `true` if the window is zoomed; `false` if it is not
     */
    fun IsZoomed(hWnd: WinDef.HWND?): Boolean

    /**
     * Get a native monitor handle from a window handle.
     *
     * @param hWnd native window handle
     * @param dwFlags flags
     * @return native monitor handle
     */
    fun MonitorFromWindow(hWnd: WinDef.HWND?, dwFlags: WinDef.DWORD?): Pointer?

    /**
     * Get native monitor information.
     *
     * @param hMonitor native monitor handle
     * @param lpMonitorInfo structure to receive monitor information
     * @return `true` on success; `false` otherwise
     */
    fun GetMonitorInfoA(hMonitor: Pointer?, lpMonitorInfo: WinUser.MONITORINFO?): Boolean

    /**
     * Send a message to a native window.
     *
     * @param hWnd native window handle
     * @param Msg message identifier
     * @param wParam message parameter
     * @param lParam message parameter
     * @return result
     */
    override fun SendMessage(
        hWnd: WinDef.HWND, Msg: Int, wParam: WinDef.WPARAM, lParam: WinDef.LPARAM
    ): WinDef.LRESULT

    /**
     * Converts the screen coordinates of a specified point on the screen to client-area coordinates.
     *
     * @param hWnd A handle to the window whose client area will be used for the conversion.
     * @param lpPoint A POINT structure that specifies the screen coordinates to be converted.
     * @return If the function succeeds, the return value is true, else the return value is false.
     */
    fun ScreenToClient(
        hWnd: WinDef.HWND, lpPoint: WinDef.POINT,
    ): Boolean

    /**
     * Retrieves the specified system metric or system configuration setting taking into account a provided DPI.
     *
     * @param nIndex The system metric or configuration setting to be retrieved. See GetSystemMetrics for the possible values.
     * @param dpi The DPI to use for scaling the metric.
     * @return If the function succeeds, the return value is nonzero, else the return value is zero
     */
    fun GetSystemMetricsForDpi(
        nIndex: Int, dpi: WinDef.UINT,
    ): Int

    /**
     * Returns the dots per inch (dpi) value for the specified window.
     *
     * @param hWnd The window that you want to get information about.
     * @return The DPI for the window, which depends on the DPI_AWARENESS of the window. See the Remarks section for more information. An invalid hwnd value will result in a return value of 0.
     */
    fun GetDpiForWindow(hWnd: WinDef.HWND): WinDef.UINT

    /**
     * Retrieves a handle to the system menu of the specified window.
     *
     * The system menu is the menu that appears when the user clicks on the icon in the title bar of the window
     * or presses ALT+SPACE. This function allows the application to access and modify the system menu.
     *
     * @param hWnd A handle to the window that will own the system menu.
     * @param bRevert The action to be taken. If this parameter is FALSE, the function returns a handle
     * to the copy of the system menu currently in use. The copy is initially identical to the default system menu,
     * but it can be modified. If this parameter is TRUE, the function resets the system menu back to the default state.
     * @return If the bRevert parameter is FALSE, the return value is a handle to the copy of the system menu.
     * If the bRevert parameter is TRUE, the return value is NULL.
     */
    fun GetSystemMenu(hWnd: WinDef.HWND, bRevert: Boolean): WinDef.HMENU?

    /**
     * Changes information about a menu item.
     *
     * This function allows you to modify various attributes of a menu item, such as its text, state, and ID.
     *
     * @param hMenu A handle to the menu that contains the item.
     * @param uItem The identifier or position of the menu item to change. The meaning of this parameter
     * depends on the value of `fByPosition`.
     * @param fByPosition If this parameter is TRUE, `uItem` is a zero-based relative position. If it is FALSE,
     * `uItem` is a menu item identifier.
     * @param lpmii A pointer to a `MENUITEMINFO` structure that contains information about the menu item
     * and specifies which attributes to change.
     * @return If the function succeeds, the return value is nonzero.
     * If the function fails, the return value is zero. To get extended error information, call `GetLastError`.
     */
    fun SetMenuItemInfo(
        hMenu: WinDef.HMENU, uItem: Int, fByPosition: Boolean, lpmii: WinUserExtend.MENUITEMINFOW
    ): Boolean

    /**
     * Displays a shortcut menu at the specified location and tracks the selection of items on the menu.
     *
     * This function is used to display a context menu (usually triggered by a right-click) and allows
     * the user to select an option from the menu. The menu is displayed as a pop-up window.
     *
     * @param hMenu A handle to the shortcut menu to be displayed.
     * @param uFlags The function options. Use a combination of the following flags:
     * - `TPM_LEFTALIGN`: Aligns the menu horizontally so that the left side is at the x-coordinate.
     * - `TPM_RIGHTALIGN`: Aligns the menu horizontally so that the right side is at the x-coordinate.
     * - `TPM_TOPALIGN`: Aligns the menu vertically so that the top is at the y-coordinate.
     * - `TPM_BOTTOMALIGN`: Aligns the menu vertically so that the bottom is at the y-coordinate.
     * - `TPM_RETURNCMD`: Returns the menu item identifier of the user's selection instead of sending a message.
     * @param x The horizontal position of the menu, in screen coordinates.
     * @param y The vertical position of the menu, in screen coordinates.
     * @param nReserved Reserved; must be zero.
     * @param hWnd A handle to the window that owns the shortcut menu.
     * @param prcRect A pointer to a `RECT` structure that specifies an area of the screen the menu should not overlap.
     * If this parameter is null, the function ignores it.
     * @return If the `TPM_RETURNCMD` flag is specified, the return value is the menu item identifier of the item selected by the user.
     * If the user cancels the menu without making a selection or an error occurs, the return value is zero.
     */
    fun TrackPopupMenu(
        hMenu: WinDef.HMENU, uFlags: Int, x: Int, y: Int, nReserved: Int,
        hWnd: WinDef.HWND, prcRect: WinDef.RECT?
    ): Int

    /**
     * Sets the default menu item for the specified menu.
     *
     * The default menu item is typically displayed in bold and is activated when the user double-clicks
     * on the menu or presses the Enter key while the menu is open.
     *
     * @param hMenu A handle to the menu whose default item is to be set.
     * @param uItem The identifier or position of the new default menu item. The meaning of this parameter
     * depends on the value of `fByPosition`.
     * @param fByPos If this parameter is TRUE, `uItem` is a zero-based relative position. If it is FALSE,
     * `uItem` is a menu item identifier.
     * @return If the function succeeds, the return value is nonzero.
     * If the function fails, the return value is zero. To get extended error information, call `GetLastError`.
     */
    fun SetMenuDefaultItem(hMenu: WinDef.HMENU, uItem: Int, fByPos: Boolean): Boolean

    companion object {

        val INSTANCE: User32Extend =
            Native.load("user32", User32Extend::class.java, W32APIOptions.DEFAULT_OPTIONS)
    }
}
