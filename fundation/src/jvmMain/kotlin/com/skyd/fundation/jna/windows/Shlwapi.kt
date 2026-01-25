package com.skyd.fundation.jna.windows

import com.sun.jna.Native
import com.sun.jna.win32.StdCallLibrary

@Suppress("FunctionName")
interface Shlwapi : StdCallLibrary {
    /**
     * @param qdw    file size 64bit
     * @param pszBuf output buffer
     * @param cchBuf buffer size
     */
    fun StrFormatByteSizeW(qdw: Long, pszBuf: CharArray, cchBuf: Int): Int

    companion object {
        val INSTANCE: Shlwapi = Native.load<Shlwapi>(
            "shlwapi",
            Shlwapi::class.java,
        )

        fun strFormatByteSizeW(byteCount: Long): String {
            val buffer = CharArray(64)
            INSTANCE.StrFormatByteSizeW(
                byteCount,
                buffer,
                buffer.size,
            )
            return Native.toString(buffer)
        }
    }
}