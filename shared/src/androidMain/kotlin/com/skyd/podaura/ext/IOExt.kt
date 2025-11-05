package com.skyd.podaura.ext

import android.content.Context
import android.net.Uri
import com.skyd.fundation.di.get


val Uri.type: String?
    get() = get<Context>().contentResolver.getType(this)
