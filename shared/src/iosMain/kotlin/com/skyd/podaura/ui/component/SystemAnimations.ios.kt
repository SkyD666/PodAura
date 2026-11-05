package com.skyd.podaura.ui.component

import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

internal actual fun platformAnimationsEnabled(): Boolean = !UIAccessibilityIsReduceMotionEnabled()
