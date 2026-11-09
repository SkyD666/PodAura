package com.skyd.podaura.ui.local

import androidx.compose.runtime.staticCompositionLocalOf
import platform.AppKit.NSWindow

val LocalMacosMainWindow = staticCompositionLocalOf<NSWindow?> { null }
