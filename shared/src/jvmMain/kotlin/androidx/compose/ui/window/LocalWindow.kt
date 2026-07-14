package androidx.compose.ui.window

import androidx.compose.runtime.compositionLocalOf
import java.awt.Window

val LocalWindow = compositionLocalOf<Window?> { null }