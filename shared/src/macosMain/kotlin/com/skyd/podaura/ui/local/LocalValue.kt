package com.skyd.podaura.ui.local

import androidx.compose.runtime.staticCompositionLocalOf
import platform.AppKit.NSWindow

internal val LocalMacosMainWindow = staticCompositionLocalOf<NSWindow?> { null }
