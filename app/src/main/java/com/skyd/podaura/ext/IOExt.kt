package com.skyd.podaura.ext

import android.net.Uri
import com.skyd.podaura.appContext


val Uri.type: String?
    get() = appContext.contentResolver.getType(this)
