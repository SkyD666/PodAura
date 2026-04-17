package com.skyd.fundation.jna.windows

import com.sun.jna.Structure

/**
 * Ported from Uxtheme.h
 */
@Suppress("SpellCheckingInspection")
interface Uxtheme {

    // See https://stackoverflow.com/q/62240901
    @Structure.FieldOrder(
        "leftBorderWidth",
        "rightBorderWidth",
        "topBorderHeight",
        "bottomBorderHeight",
    )
    data class MARGINS(
        @JvmField var leftBorderWidth: Int,
        @JvmField var rightBorderWidth: Int,
        @JvmField var topBorderHeight: Int,
        @JvmField var bottomBorderHeight: Int
    ) : Structure(), Structure.ByReference
}
