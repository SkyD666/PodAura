package com.skyd.htmlrender.core.util

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp


object TextUnitParser {
    const val EM = "em"
    const val PX = "px"

    fun parse(value: String): TextUnit? = when {
        value.endsWith(EM) -> value.removeSuffix(EM).toFloat().em
        value.endsWith(PX) -> value.removeSuffix(PX).toFloat().sp
        else -> null
    }
}