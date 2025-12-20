package com.skyd.podaura.util

import android.content.Context
import android.content.pm.ApplicationInfo
import com.skyd.fundation.di.get

actual val isDebug: Boolean
    get() = get<Context>().applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
