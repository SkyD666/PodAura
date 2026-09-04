package com.skyd.podaura.ext

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed


val KeyEvent.hasModifier: Boolean
    get() = isCtrlPressed || isAltPressed || isMetaPressed || isShiftPressed
