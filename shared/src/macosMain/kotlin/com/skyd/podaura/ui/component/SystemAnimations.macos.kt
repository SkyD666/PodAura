package com.skyd.podaura.ui.component

import platform.AppKit.NSWorkspace
import platform.AppKit.accessibilityDisplayShouldReduceMotion

internal actual fun platformAnimationsEnabled(): Boolean =
    !NSWorkspace.sharedWorkspace.accessibilityDisplayShouldReduceMotion
