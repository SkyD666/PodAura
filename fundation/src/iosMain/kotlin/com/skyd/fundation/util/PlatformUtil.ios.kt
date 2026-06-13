package com.skyd.fundation.util

import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIApplication
import platform.UIKit.UIControl

actual val platform: Platform
    get() = Platform.iOS

actual fun exitApp() = UIControl().sendAction(
    action = NSSelectorFromString("suspend"),
    to = UIApplication.sharedApplication,
    forEvent = null
)
