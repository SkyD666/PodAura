package com.skyd.podaura.ext

import android.content.Context
import com.skyd.fundation.di.get

actual fun platformContext() = get<Context>()