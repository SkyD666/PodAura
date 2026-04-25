package com.skyd.fundation.jna.mac

import com.sun.jna.Structure

@Structure.FieldOrder("x", "y", "width", "height")
open class CGRect(
    @JvmField var x: Double = 0.0,
    @JvmField var y: Double = 0.0,
    @JvmField var width: Double = 0.0,
    @JvmField var height: Double = 0.0
) : Structure(), Structure.ByValue
