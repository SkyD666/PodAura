package com.skyd.htmlrender.core.util

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp


object TextUnitParser {
    const val EM = "em"
    const val REM = "rem"
    const val PX = "px"

    fun parse(value: String, unitlessAsEm: Boolean = false): TextUnit? {
        val normalized = value.trim().lowercase()
        return when {
            normalized.endsWith(REM) -> normalized.removeSuffix(REM).toFloat().em
            normalized.endsWith(EM) -> normalized.removeSuffix(EM).toFloat().em
            normalized.endsWith(PX) -> normalized.removeSuffix(PX).toFloat().sp
            normalized.endsWith("pt") -> (normalized.removeSuffix("pt").toFloat() * 4f / 3f).sp
            normalized.endsWith("%") -> (normalized.removeSuffix("%").toFloat() / 100f).em
            normalized.toFloatOrNull() == 0f -> 0.sp
            unitlessAsEm -> normalized.toFloatOrNull()?.em
            else -> null
        }
    }
}
