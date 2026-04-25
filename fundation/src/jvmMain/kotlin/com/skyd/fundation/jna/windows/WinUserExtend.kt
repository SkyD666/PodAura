package com.skyd.fundation.jna.windows

import com.sun.jna.Structure
import com.sun.jna.platform.win32.BaseTSD
import com.sun.jna.platform.win32.WinDef

@Suppress("SpellCheckingInspection", "ClassName")
interface WinUserExtend {

    @Structure.FieldOrder(
        "cbSize", "fMask", "fType", "fState", "wID", "hSubMenu",
        "hbmpChecked", "hbmpUnchecked", "dwItemData", "dwTypeData", "cch", "hbmpItem"
    )
    class MENUITEMINFOW(
        @JvmField var cbSize: Int = 0,
        @JvmField var fMask: Int = 0,
        @JvmField var fType: Int = 0,
        @JvmField var fState: Int = 0,
        @JvmField var wID: Int = 0,
        @JvmField var hSubMenu: WinDef.HMENU? = null,
        @JvmField var hbmpChecked: WinDef.HBITMAP? = null,
        @JvmField var hbmpUnchecked: WinDef.HBITMAP? = null,
        @JvmField var dwItemData: BaseTSD.ULONG_PTR = BaseTSD.ULONG_PTR(0),
        @JvmField var dwTypeData: String? = null,
        @JvmField var cch: Int = 0,
        @JvmField var hbmpItem: WinDef.HBITMAP? = null
    ) : Structure(), Structure.ByReference

    @Structure.FieldOrder("rgrc", "lppos")
    class NCCALCSIZE_PARAMS(
        @JvmField var rgrc: Array<WinDef.RECT?> = Array(3) { null },
        @JvmField var lppos: WINDOWPOS? = null
    ) : Structure(), Structure.ByReference

    @Structure.FieldOrder(
        "hwnd",
        "hwndInsertAfter",
        "x",
        "y",
        "cx",
        "cy",
        "flags",
    )
    class WINDOWPOS(
        @JvmField var hwnd: WinDef.HWND? = null,
        @JvmField var hwndInsertAfter: WinDef.HWND? = null,
        @JvmField var x: Int = 0,
        @JvmField var y: Int = 0,
        @JvmField var cx: Int = 0,
        @JvmField var cy: Int = 0,
        @JvmField var flags: WinDef.UINT = WinDef.UINT()
    ) : Structure(), Structure.ByReference

    companion object {

        // window active event
        const val WM_ACTIVATE: Int = 0x0006

        // window is deactivated
        const val WA_INACTIVE: Int = 0

        // setting changed message
        const val WM_SETTINGCHANGE: Int = 0x001A

        // calculate non client area size message
        const val WM_NCCALCSIZE: Int = 0x0083

        // non client area hit test message
        const val WM_NCHITTEST: Int = 0x0084

        // non client area mouse move message
        const val WM_NCMOUSEMOVE: Int = 0x00A0

        // non client area left mouse down message
        const val WM_NCLBUTTONDOWN: Int = 0x00A1

        // non client area left mouse up message
        const val WM_NCLBUTTONUP: Int = 0x00A2

        // non client area right mouse up message
        const val WM_NCRBUTTONUP: Int = 0x00A5

        // mouse move message
        const val WM_MOUSEMOVE: Int = 0x0200

        // left mouse button down message
        const val WM_LBUTTONDOWN: Int = 0x0201

        // left mouse button up message
        const val WM_LBUTTONUP: Int = 0x0202

        const val WS_EX_DLGMODALFRAME: Int = 0x00000001
        const val WS_EX_WINDOWEDGE: Int = 0x00000100
        const val WS_EX_CLIENTEDGE: Int = 0x00000200
        const val WS_EX_STATICEDGE: Int = 0x00020000

        const val MIIM_STATE: Int = 0x00000001 // The `fState` member is valid.

        const val TPM_RETURNCMD: Int = 0x0100 // Returns the menu item identifier of the user's selection instead of sending a message.

        const val MFT_STRING: Int = 0x00000000 // The item is a text string.b

        const val MFS_ENABLED: Int = 0x00000000 // The item is enabled.

        const val MFS_DISABLED: Int = 0x00000003 // The item is disabled.

        const val SWP_NOACTIVATE: Int = 0x0010

        const val SC_RESTORE: Int = 0xF120
        const val SC_MOVE: Int = 0xF010
        const val SC_SIZE: Int = 0xF000
        const val SC_CLOSE: Int = 0xF060

        val MONITOR_DEFAULTTONEAREST: WinDef.DWORD = WinDef.DWORD(2)
    }
}
