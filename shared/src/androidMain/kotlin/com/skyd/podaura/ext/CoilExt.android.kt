package com.skyd.podaura.ext

import android.content.Context
import com.skyd.podaura.di.get

actual fun platformContext() = get<Context>()