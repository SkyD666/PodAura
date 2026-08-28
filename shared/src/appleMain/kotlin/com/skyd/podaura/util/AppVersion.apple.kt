package com.skyd.podaura.util

import com.skyd.podaura.BuildKonfig
import platform.Foundation.NSBundle

actual val appVersion: AppVersion by lazy {
    val bundle = NSBundle.mainBundle
    val versionName = bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
    val versionCode = bundle.objectForInfoDictionaryKey("CFBundleVersion") as? String
    AppVersion(
        name = versionName?.takeIf { it.isNotBlank() } ?: BuildKonfig.versionForDesktop,
        code = versionCode?.toLongOrNull(),
    )
}
