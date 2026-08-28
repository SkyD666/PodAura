package com.skyd.podaura.util

import com.skyd.podaura.BuildKonfig

actual val appVersion: AppVersion = AppVersion(
    name = BuildKonfig.versionName,
    code = BuildKonfig.versionCode.toLong(),
)
