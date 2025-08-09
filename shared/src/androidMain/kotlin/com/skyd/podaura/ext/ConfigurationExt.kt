package com.skyd.podaura.ext

import android.content.res.Configuration


val Configuration.isLandscape: Boolean
    get() = orientation == Configuration.ORIENTATION_LANDSCAPE