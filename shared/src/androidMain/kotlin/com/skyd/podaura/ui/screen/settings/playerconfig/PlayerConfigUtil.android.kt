package com.skyd.podaura.ui.screen.settings.playerconfig

import android.os.Build

actual val maxCacheSizeMB: Int
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) 64 else 32